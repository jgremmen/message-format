/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.data.DataNumber;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.internal.part.TextPart;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class BitsFormatterTest
{
  @Test
  public void testFormatterConfig()
  {
    BitsFormatter formatter = new BitsFormatter();

    assertTrue(formatter.getFormattableTypes().isEmpty());
    assertEquals("bits", formatter.getName());
  }


  @Test
  public void testByte()
  {
    BitsFormatter formatter = new BitsFormatter();
    Parameters parameters = ParameterFactory.DEFAULT.noParameters();

    assertEquals(new TextPart("11111111"), formatter.format((byte)0xff, null, parameters, null));
    assertEquals(new TextPart("00000000"), formatter.format((byte)0, null, parameters, null));
    assertEquals(new TextPart("10101010"), formatter.format((byte)0xaa, null, parameters, null));
    assertEquals(new TextPart("01010101"), formatter.format((byte)0x55, null, parameters, null));
    assertEquals(new TextPart("10101"), formatter.format((byte)0x15, null, parameters, new DataString("auto")));
    assertEquals(new TextPart("0"), formatter.format((byte)0, null, parameters, new DataString("auto")));
    assertEquals(new TextPart("101"), formatter.format((byte)0x15, null, parameters, new DataNumber(3)));
    assertEquals(TextPart.NULL, formatter.format(null, null, parameters, null));
    assertEquals(TextPart.NULL, formatter.format("hello", null, parameters, null));
  }


  @Test
  public void testShort()
  {
    BitsFormatter formatter = new BitsFormatter();
    Parameters parameters = ParameterFactory.DEFAULT.noParameters();

    assertEquals(new TextPart("1111111111111111"), formatter.format((short)0xffff, null, parameters, null));
    assertEquals(new TextPart("0000000000000000"), formatter.format((short)0, null, parameters, null));
    assertEquals(new TextPart("1010101010101010"), formatter.format((short)0xaaaa, null, parameters, null));
    assertEquals(new TextPart("0101010101010101"), formatter.format((short)0x5555, null, parameters, null));
    assertEquals(new TextPart("101010101"), formatter.format((short)0x155, null, parameters, new DataString("auto")));
    assertEquals(new TextPart("0"), formatter.format((short)0, null, parameters, new DataString("auto")));
    assertEquals(new TextPart("010110"), formatter.format((byte)0x456, null, parameters, new DataNumber(6)));
  }
}
