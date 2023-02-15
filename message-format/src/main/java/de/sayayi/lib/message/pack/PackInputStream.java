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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.*;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import static de.sayayi.lib.message.pack.PackOutputStream.PACK_MAGIC;
import static java.lang.Integer.bitCount;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class PackInputStream implements Closeable
{
  private final @NotNull InputStream stream;
  private int bit = -1;
  private byte b;


  public PackInputStream(@NotNull InputStream stream) throws IOException
  {
    final byte[] signature = new byte[PACK_MAGIC.length];
    if (stream.read(signature) != PACK_MAGIC.length || !Arrays.equals(signature, PACK_MAGIC))
      throw new IOException("pack stream has wrong signature");

    this.stream = stream.read() != 0 ? new GZIPInputStream(stream) : stream;
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


  public int readUnsignedShort() throws IOException {
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
      case 0:
        return null;

      case 1:
        if ((utflen = readSmall(4)) == 0)
          return "";
        break;

      case 2:
        utflen = readSmall(8);
        break;

      case 3:
        utflen = (int)readLarge(16);
        break;
    }

    forceByteAlignment();

    final byte[] bytes = new byte[utflen];
    if (stream.read(bytes) != utflen)
      throw new EOFException();

    final char[] chars = new char[utflen];
    int charIdx = 0;

    for(int count = 0; count < utflen;)
    {
      final int c = (int)bytes[count] & 0xff;

      switch(c >> 4)
      {
        case 0:
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
          /* 0xxxxxxx*/
          count++;
          chars[charIdx++] = (char)c;
          break;

        case 12:
        case 13: {
          /* 110x xxxx   10xx xxxx*/
          if ((count += 2) > utflen)
            throw new UTFDataFormatException("malformed input: partial character at end");

          final int char2 = bytes[count - 1];
          if ((char2 & 0xC0) != 0x80)
            throw new UTFDataFormatException("malformed input around byte " + count);

          chars[charIdx++] = (char)((c & 0x1F) << 6 | (char2 & 0x3F));
          break;
        }

        case 14: {
          /* 1110 xxxx  10xx xxxx  10xx xxxx */
          if ((count += 3) > utflen)
            throw new UTFDataFormatException("malformed input: partial character at end");

          final int char2 = bytes[count - 2];
          final int char3 = bytes[count - 1];
          if ((char2 & 0xC0) != 0x80 || (char3 & 0xC0) != 0x80)
            throw new UTFDataFormatException("malformed input around byte " + (count - 1));

          chars[charIdx++] = (char)(((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | (char3 & 0x3F));
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
      bit = 7;
      int c = stream.read();
      if (c < 0)
        throw new EOFException();

      b = (byte)c;
    }
  }


  public int readSmall(@Range(from = 1, to = 8) int bitWidth) throws IOException
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
