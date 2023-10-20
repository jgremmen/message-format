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

import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("Pack/unpack various types to/from stream")
class PackStreamTest
{
  @Test
  @DisplayName("Pack/unpack a mixture of types")
  void packMixed() throws IOException
  {
    val byteStream = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(byteStream, false)) {
      packStream.writeSmall(5, 3);
      packStream.writeBoolean(true);
      packStream.writeEnum(CompareType.GT);
      packStream.writeUnsignedShort(11234);
      packStream.writeString(null);
      packStream.writeSmallVar(13);
      packStream.writeString("Schön ist es hier ÄÖß§");
      packStream.writeEnum(CompareType.LTE);
      packStream.writeLong(Long.MIN_VALUE);
    }

    val packed = byteStream.toByteArray();

    try(val packStream = new PackInputStream(new ByteArrayInputStream(packed))) {
      assertEquals(5, packStream.readSmall(3));
      assertTrue(packStream.readBoolean());
      assertEquals(CompareType.GT, packStream.readEnum(CompareType.class));
      assertEquals(11234, packStream.readUnsignedShort());
      assertNull(packStream.readString());
      assertEquals(13, packStream.readSmallVar());
      assertEquals("Schön ist es hier ÄÖß§", packStream.readString());
      assertEquals(CompareType.LTE, packStream.readEnum(CompareType.class));
      assertEquals(Long.MIN_VALUE, packStream.readLong());
    }
  }


  @Test
  @DisplayName("Pack/unpack small value 0..255 with variable bit width")
  void packSmallVar() throws IOException
  {
    val byteStream = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(byteStream, false)) {
      for(int n = 0; n < 255; n++)
        packStream.writeSmallVar(n);
    }

    val packed = byteStream.toByteArray();

    try(val packStream = new PackInputStream(new ByteArrayInputStream(packed))) {
      for(int n = 0; n < 255; n++)
        assertEquals(n, packStream.readSmallVar());
    }
  }


  @Test
  @DisplayName("Pack/unpack small value 0..255 with fixed bit widths")
  void packSmall() throws IOException
  {
    val byteStream = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(byteStream, false)) {
      for(int bitWidth = 1; bitWidth <= 8; bitWidth++)
        for(int value = 0; value < (1 << bitWidth); value++)
          packStream.writeSmall(value, bitWidth);
    }

    val packed = byteStream.toByteArray();

    try(val packStream = new PackInputStream(new ByteArrayInputStream(packed))) {
      for(int bitWidth = 1; bitWidth <= 8; bitWidth++)
        for(int value = 0; value < (1 << bitWidth); value++)
          assertEquals(value, packStream.readSmall(bitWidth));
    }
  }


  @Test
  @DisplayName("Pack/unpack large values with fixed bit widths")
  void packLarge() throws IOException
  {
    val random = new Random(currentTimeMillis());
    final Map<Integer,long[]> valueMap = Arrays
        .stream(new int[] { 9, 16, 17, 29, 32, 43, 48, 57, 64 })
        .boxed()
        .collect(toMap(Function.identity(), bitWidth -> {
          val values = new long[1000];
          long mask = bitWidth == 64 ? -1L : ((1L << bitWidth) - 1);

          for(int n = 0; n < values.length; n++)
            values[n] = random.nextLong() & mask;

          return values;
        }, (l1,l2) -> l1, TreeMap::new));

    val byteStream = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(byteStream, true)) {
      valueMap.forEach((Integer bitWidth, long[] values) -> {
        try {
          for(long value: values)
            packStream.writeLarge(value, bitWidth);
        } catch(IOException ex) {
          fail(ex);
        }
      });
    }

    val packed = byteStream.toByteArray();

    try(val packStream = new PackInputStream(new ByteArrayInputStream(packed))) {
      valueMap.forEach((Integer bitWidth, long[] values) -> {
        try {
          for(long value: values)
            assertEquals(value, packStream.readLarge(bitWidth));
        } catch(IOException ex) {
          fail(ex);
        }
      });
    }
  }


  @Test
  @DisplayName("Pack/unpack booleans")
  void packBoolean() throws IOException
  {
    val random = new Random(currentTimeMillis());
    val longs = new long[100];
    for(int n = 0; n < longs.length; n++)
      longs[n] = random.nextLong();
    val bits = BitSet.valueOf(longs);

    val byteStream = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(byteStream, false)) {
      for(int n = 0; n < bits.length(); n++)
        packStream.writeBoolean(bits.get(n));
    }

    val packed = byteStream.toByteArray();

    try(val packStream = new PackInputStream(new ByteArrayInputStream(packed))) {
      for(int n = 0; n < bits.length(); n++)
        assertEquals(bits.get(n), packStream.readBoolean());
    }
  }
}
