package de.sayayi.lib.message.pack;

import de.sayayi.lib.message.data.map.MapKey.CompareType;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.DisplayName.class)
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
      assertEquals("Schön ist es hier ÄÖß§", packStream.readString());
      assertEquals(CompareType.LTE, packStream.readEnum(CompareType.class));
      assertEquals(Long.MIN_VALUE, packStream.readLong());
    }
  }


  @Test
  @DisplayName("Pack/unpack small value with variable bit width")
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
  @DisplayName("Pack/unpack small value with fix bit width")
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
  @DisplayName("Pack/unpack large value with fix bit width")
  void packLarge() throws IOException
  {
    val random = new Random(System.currentTimeMillis());
    val valueMap = Stream
        .of(9, 16, 17, 29, 32, 43, 48, 57, 64)
        .collect(toMap(bitWidth -> bitWidth, bitWidth -> {
          val values = new long[100];
          long mask = bitWidth == 64 ? -1L : ((1L << bitWidth) - 1);

          for(int n = 0; n < 100; n++)
            values[n] = random.nextLong() & mask;

          return values;
        }, (l1,l2) -> l1, TreeMap<Integer,long[]>::new));

    val byteStream = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(byteStream, false)) {
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
}
