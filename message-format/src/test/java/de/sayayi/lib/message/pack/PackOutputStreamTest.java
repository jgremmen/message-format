package de.sayayi.lib.message.pack;

import de.sayayi.lib.message.data.map.MapKey.CompareType;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


class PackOutputStreamTest
{
  @Test
  void writeAndRead() throws IOException
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
}
