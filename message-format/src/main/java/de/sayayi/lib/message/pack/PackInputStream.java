/*
 * Copyright 2023 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.pack;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.*;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import static de.sayayi.lib.message.pack.PackOutputStream.PACK_HEADER;
import static java.lang.Integer.bitCount;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class PackInputStream implements Closeable
{
  private final @NotNull InputStream stream;
  private final int version;
  private int bit = -1;
  private byte b;


  public PackInputStream(@NotNull InputStream stream) throws IOException
  {
    final byte[] header = new byte[PACK_HEADER.length];
    if (stream.read(header) != header.length || !Arrays.equals(header, PACK_HEADER))
      throw new IOException("pack stream has wrong header; possibly not a message pack");

    final int zv = stream.read();
    if ((zv & 0b0100_0000) == 0)
      throw new IOException("malformed message pack version");

    this.stream = (zv & 0b1000_0000) != 0 ? new GZIPInputStream(stream) : stream;
    version = zv & 0b0011_1111;
  }


  @Contract(pure = true)
  public int getVersion() {
    return version;
  }


  public boolean readBoolean() throws IOException
  {
    assertData();

    return (b & (1 << bit--)) != 0;
  }


  public <T extends Enum<T>> @NotNull T readEnum(@NotNull Class<T> enumType) throws IOException
  {
    final T[] enums = enumType.getEnumConstants();

    int n = enums.length;
    n |= n >> 1;
    n |= n >> 2;
    n |= n >> 4;

    return enums[readSmall(bitCount(n))];
  }


  public @Range(from = 0, to = 65535) int readUnsignedShort() throws IOException {
    return (int)readLarge(16);
  }


  public long readLong() throws IOException {
    return readLarge(64);
  }


  public String readString() throws IOException
  {
    int utflen = 0;

    switch(readSmall(2))
    {
      case 0b00:
        return null;

      case 0b01:
        if ((utflen = readSmall(4)) == 0)
          return "";
        break;

      case 0b10:
        utflen = readSmall(8);
        break;

      case 0b11:
        utflen = (int)readLarge(16);
        break;
    }

    forceByteAlignment();

    final byte[] bytes = new byte[utflen];
    if (stream.read(bytes) != utflen)
      throw new EOFException("unexpected end of pack stream while reading utf string");

    final char[] chars = new char[utflen];
    int charIdx = 0;

    for(int count = 0; count < utflen;)
    {
      final int c = (int)bytes[count] & 0xff;

      switch(c >> 4)
      {
        case 0b0_000:
        case 0b0_001:
        case 0b0_010:
        case 0b0_011:
        case 0b0_100:
        case 0b0_101:
        case 0b0_110:
        case 0b0_111:
          /* 0xxx xxxx*/
          count++;
          chars[charIdx++] = (char)c;
          break;

        case 0b110_0:
        case 0b110_1: {
          /* 110x xxxx   10xx xxxx*/
          if ((count += 2) > utflen)
            throw new UTFDataFormatException("malformed input: partial character at end");

          final int char2 = bytes[count - 1];
          if ((char2 & 0b1100_0000) != 0b1000_0000)
            throw new UTFDataFormatException("malformed input around byte " + count);

          chars[charIdx++] = (char)((c & 0b0001_1111) << 6 | (char2 & 0b0011_1111));
          break;
        }

        case 0b1110: {
          /* 1110 xxxx  10xx xxxx  10xx xxxx */
          if ((count += 3) > utflen)
            throw new UTFDataFormatException("malformed input: partial character at end");

          final int char2 = bytes[count - 2];
          final int char3 = bytes[count - 1];
          if ((char2 & 0b1100_0000) != 0b1000_0000 || (char3 & 0b1100_0000) != 0b1000_0000)
            throw new UTFDataFormatException("malformed input around byte " + (count - 1));

          chars[charIdx++] = (char)(((c & 0b0000_1111) << 12) |
              ((char2 & 0b0011_1111) << 6) | (char3 & 0b0011_1111));
          break;
        }

        default:
          /* 10xx xxxx,  1111 xxxx */
          throw new UTFDataFormatException("malformed input around byte " + count);
      }
    }

    return new String(chars, 0, charIdx);
  }


  private void assertData() throws IOException
  {
    if (bit < 0)
    {
      int c = stream.read();
      if (c < 0)
        throw new EOFException("unexpected end of pack stream");

      b = (byte)c;
      bit = 7;
    }
  }


  /**
   * Ranges: 0..7 (4 bit), 8..15 (5 bit), 16..255 (10 bit)
   *
   * @return  value in range 0..255
   *
   * @throws IOException  if an I/O error occurs
   */
  public @Range(from = 0, to = 255) int readSmallVar() throws IOException
  {
    final int v4 = readSmall(4);

    if ((v4 & 0b1000) == 0)  // 0vvv
      return v4;
    else if ((v4 & 0b0100) == 0)  // 10vv_v (-> 1vvv)
      return ((v4 - 0b0100) << 1) | (readBoolean() ? 1 : 0);
    else  // 11vv_vvvvvv
      return ((v4 & 0b0011) << 6) | readSmall(6);
  }


  public @Range(from = 0, to = 255) int readSmall(@Range(from = 1, to = 8) int bitWidth)
      throws IOException
  {
    assertData();

    final int bitsRemaining = bit + 1 - bitWidth;

    if (bitsRemaining > 0)
    {
      bit = bitsRemaining - 1;
      return (b >> bitsRemaining) & ((1 << bitWidth) - 1);
    }
    else if (bitsRemaining == 0)
    {
      bit = -1;
      return b & ((1 << bitWidth) - 1);
    }
    else  // bitsRemaining < 0
    {
      int value = (b & ((1 << (bit + 1)) - 1)) << -bitsRemaining;

      bit = -1;
      assertData();

      value |= (b >> (8 + bitsRemaining)) & ((1 << -bitsRemaining) - 1);
      bit = 7 + bitsRemaining;
      return value;
    }
  }


  public long readLarge(@Range(from = 9, to = 64) int bitWidth) throws IOException
  {
    assertData();

    long value = b & ((1L << (bit + 1)) - 1);

    for(bitWidth -= bit + 1; bitWidth >= 8; bitWidth -= 8)
    {
      int c = stream.read();
      if (c < 0)
        throw new EOFException();

      value = (value << 8) | c;
    }

    bit = -1;

    if (bitWidth > 0)
    {
      assertData();

      int c = (b >> (8 - bitWidth)) & ((1 << bitWidth) - 1);
      value = (value << bitWidth) | c;
      bit -= bitWidth;
    }

    return value;
  }


  private void forceByteAlignment()
  {
    if (bit >= 0)
      bit = -1;
  }


  @Override
  public void close() throws IOException {
    stream.close();
  }
}
