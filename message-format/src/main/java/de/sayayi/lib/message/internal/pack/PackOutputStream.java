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
package de.sayayi.lib.message.internal.pack;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import static java.lang.Integer.bitCount;
import static java.nio.charset.StandardCharsets.US_ASCII;


/**
 * signature = %{msg} z1vvvvvv  (z = gzip, v = version)
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class PackOutputStream implements Closeable
{
  public static final int PACK_VERSION = 3;
  static final byte[] PACK_HEADER = "%{msg}".getBytes(US_ASCII);

  private final @NotNull OutputStream stream;
  private int bit = 7;
  private byte b;


  public PackOutputStream(@NotNull OutputStream stream, boolean compress) throws IOException
  {
    stream.write(PACK_HEADER);
    stream.write((compress ? 0b1100_0000 : 0b0100_0000) + (PACK_VERSION & 0b0011_1111));

    this.stream = compress ? new GZIPOutputStream(stream) : stream;
  }


  public void writeBoolean(boolean value) throws IOException
  {
    if (value)
      b |= (byte)(1 << bit);

    if (bit-- == 0)
    {
      stream.write(b);
      bit = 7;
      b = 0;
    }
  }


  public <T extends Enum<T>> void writeEnum(@NotNull T value) throws IOException
  {
    int n = value.getClass().getEnumConstants().length;
    n |= n >> 1;
    n |= n >> 2;
    n |= n >> 4;

    writeSmall(value.ordinal(), bitCount(n));
  }


  /**
   * @param value  unsigned value (0..65535)
   *
   * @throws IOException  if an I/O error occurs
   */
  public void writeUnsignedShort(int value) throws IOException {
    writeLarge(value, 16);
  }


  public void writeLong(long value) throws IOException {
    writeLarge(value, 64);
  }


  public void writeString(String str) throws IOException
  {
    if (str == null)
    {
      writeSmall(0, 2);
      return;
    }

    var strlen = str.length();
    int utflen = 0;

    for(int i = 0; i < strlen; i++)
    {
      int c = str.charAt(i);

      if (c >= 0x0001 && c <= 0x007F)
        utflen++;
      else if (c > 0x07FF)
        utflen += 3;
      else
        utflen += 2;
    }

    if (utflen < 16)
      writeSmall(0b01_0000 | utflen, 6);
    else if (utflen < 256)
      writeLarge(0b10_0000_0000 | utflen, 10);
    else
    {
      writeSmall(0b11, 2);
      writeLarge(utflen, 16);
    }

    if (utflen > 0)
    {
      forceByteAlignment();

      var bytes = new byte[utflen];

      for(int i = 0, count = 0; i < strlen; i++)
      {
        int c = str.charAt(i);

        if (c >= 0x0001 && c <= 0x007F)
          bytes[count++] = (byte)c;
        else if (c > 0x07FF)
        {
          bytes[count++] = (byte)(0xE0 | ((c >> 12) & 0x0F));
          bytes[count++] = (byte)(0x80 | ((c >>  6) & 0x3F));
          bytes[count++] = (byte)(0x80 | ( c        & 0x3F));
        }
        else
        {
          bytes[count++] = (byte)(0xC0 | ((c >> 6) & 0x1F));
          bytes[count++] = (byte)(0x80 | ( c       & 0x3F));
        }
      }

      stream.write(bytes);
    }
  }


  /**
   * Ranges: 0..7 (4 bit), 8..15 (5 bit), 16..255 (10 bit)
   *
   * @param value  unsigned value (0..255)
   *
   * @throws IOException  if an I/O error occurs
   */
  public void writeSmallVar(int value) throws IOException
  {
    if (value <= 7)
      writeSmall(value, 4);  // 0vvv
    else if (value <= 15)
      writeSmall(0b10_000 | (value - 8), 5);  // 10vvv (vvv + 8)
    else
      writeLarge(0b11_00000000 | value, 10);  // 11vvvvvvvv
  }


  /**
   * @param value     unsigned value (0..255)
   * @param bitWidth  range 1..8 bits
   *
   * @throws IOException  if an I/O error occurs
   */
  public void writeSmall(int value, int bitWidth) throws IOException
  {
    if (value >= (1 << bitWidth))
      throw new IllegalArgumentException("value " + value + " occupies more than " + bitWidth + " bits");

    var bitsRemaining = bit + 1 - bitWidth;

    if (bitsRemaining > 0)
    {
      b |= (byte)((value & ((1 << bitWidth) - 1)) << bitsRemaining);
      bit -= bitWidth;
    }
    else if (bitsRemaining == 0)
    {
      stream.write(b | (value & ((1 << bitWidth) - 1)));
      b = 0;
      bit = 7;
    }
    else  // bitsRemaining < 0
    {
      stream.write(b | (byte)(value >>> -bitsRemaining) & ((1 << (bit + 1)) - 1));
      b = (byte)((value << (8 + bitsRemaining)) & 0xff);
      bit = 7 + bitsRemaining;
    }
  }


  /**
   * @param value     signed value
   * @param bitWidth  range 9..64 bits
   *
   * @throws IOException  if an I/O error occurs
   */
  public void writeLarge(long value, int bitWidth) throws IOException
  {
    if (bit < 7)
    {
      var bitsLeft = bit + 1 - bitWidth;

      b |= (byte)((byte)(value >>> -bitsLeft) & ((1 << (bit + 1)) - 1));
      stream.write(b);

      bitWidth -= bit + 1;
      b = 0;
      bit = 7;
    }

    while(bitWidth >= 8)
      stream.write((byte)(value >>> (bitWidth -= 8)));

    if (bitWidth > 0)
    {
      b = (byte)((value << (8 - bitWidth)) & 0xff);
      bit = 7 - bitWidth;
    }
  }


  private void forceByteAlignment() throws IOException
  {
    if (bit != 7)
    {
      stream.write(b);
      bit = 7;
      b = 0;
    }
  }


  @Override
  public void close() throws IOException
  {
    forceByteAlignment();
    stream.close();
  }
}
