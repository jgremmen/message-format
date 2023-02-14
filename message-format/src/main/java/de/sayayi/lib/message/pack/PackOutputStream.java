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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.US_ASCII;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class PackOutputStream implements Closeable
{
  static final byte[] PACK_MAGIC = "MSGB\u0000\u0008".getBytes(US_ASCII);

  private final @NotNull OutputStream stream;
  private int bit = 7;
  private byte b = 0;


  public PackOutputStream(@NotNull OutputStream stream, boolean compress) throws IOException
  {
    stream.write(PACK_MAGIC);
    stream.write(compress ? 1 : 0);

    this.stream = compress ? new GZIPOutputStream(stream) : stream;
  }


  public void writeBoolean(boolean value) throws IOException
  {
    if (value)
      b |= 1 << bit;

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
    n |= n >> 8;

    write(value.ordinal(), Integer.bitCount(n));
  }


  public void writeByte(byte value) throws IOException {
    write(value, 8);
  }


  public void writeShort(short value) throws IOException {
    write(value, 16);
  }


  public void writeInt(int value) throws IOException {
    write(value, 32);
  }


  public void writeLong(long value) throws IOException {
    write(value, 64);
  }


  public void writeString(String str) throws IOException
  {
    if (str == null)
    {
      write(0, 2);
      return;
    }

    final int strlen = str.length();
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
      write(0x10 + utflen, 6);
    else if (utflen < 256)
      write(0x200 + utflen, 10);
    else
    {
      write(3, 2);
      write(utflen, 16);
    }

    if (utflen > 0)
    {
      forceByteAlignment();

      final byte[] bytes = new byte[utflen];

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


  public void write(long value, int bitWidth) throws IOException
  {
    if (bitWidth > 0 && bit < 7)
    {
      int bitsLeft = bit + 1 - bitWidth;

      if (bitsLeft == 0)
      {
        b |= value & ((1L << bitWidth) - 1);
        stream.write(b);
        bit = 7;
        b = 0;

        return;
      }

      if (bitsLeft > 0)
      {
        b |= (value & ((1L << bitWidth) - 1)) << bitsLeft;
        bit -= bitWidth;

        return;
      }

      b |= (byte)(value >>> -bitsLeft) & ((1 << (bit + 1)) - 1);
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


  protected void forceByteAlignment() throws IOException
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
