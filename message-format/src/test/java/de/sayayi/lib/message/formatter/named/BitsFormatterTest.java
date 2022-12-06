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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.MapKeyName;
import de.sayayi.lib.message.data.map.MapValueNumber;
import de.sayayi.lib.message.data.map.MapValueString;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class BitsFormatterTest
{
  @Test
  public void testFormatterConfig()
  {
    final BitsFormatter formatter = new BitsFormatter();

    assertTrue(formatter.getFormattableTypes().isEmpty());
    assertEquals("bits", formatter.getName());
  }


  @Test
  public void testByte()
  {
    final BitsFormatter formatter = new BitsFormatter();
    final MessageContext context =
        new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    final Parameters parameters = context.noParameters();

    assertEquals(noSpaceText("11111111"), formatter.format(context, (byte)0xff, parameters, null));
    assertEquals(noSpaceText("00000000"), formatter.format(context, (byte)0, parameters, null));
    assertEquals(noSpaceText("10101010"), formatter.format(context, (byte)0xaa, parameters, null));
    assertEquals(noSpaceText("01010101"), formatter.format(context, (byte)0x55, parameters, null));
    assertEquals(noSpaceText("10101"), formatter.format(context, (byte)0x15, parameters,
        new DataMap(singletonMap(new MapKeyName("length"), new MapValueString("auto")))));
    assertEquals(noSpaceText("0"), formatter.format(context, (byte)0, parameters,
        new DataMap(singletonMap(new MapKeyName("length"), new MapValueString("auto")))));
    assertEquals(noSpaceText("101"), formatter.format(context, (byte)0x15, parameters,
        new DataMap(singletonMap(new MapKeyName("length"), new MapValueNumber(3)))));
    assertEquals(nullText(), formatter.format(context, null, parameters, null));
    assertEquals(nullText(), formatter.format(context, "hello", parameters, null));
  }


  @Test
  public void testShort()
  {
    final BitsFormatter formatter = new BitsFormatter();
    final MessageContext context =
        new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    final Parameters parameters = context.noParameters();

    assertEquals(noSpaceText("1111111111111111"),
        formatter.format(context, (short)0xffff, parameters, null));
    assertEquals(noSpaceText("0000000000000000"),
        formatter.format(context, (short)0, parameters, null));
    assertEquals(noSpaceText("1010101010101010"),
        formatter.format(context, (short)0xaaaa, parameters, null));
    assertEquals(noSpaceText("0101010101010101"),
        formatter.format(context, (short)0x5555, parameters, null));
    assertEquals(noSpaceText("101010101"), formatter.format(context, (short)0x155, parameters,
        new DataMap(singletonMap(new MapKeyName("length"), new MapValueString("auto")))));
    assertEquals(noSpaceText("0"), formatter.format(context, (short)0, parameters,
        new DataMap(singletonMap(new MapKeyName("length"), new MapValueString("auto")))));
    assertEquals(noSpaceText("010110"), formatter.format(context, (byte)0x456, parameters,
        new DataMap(singletonMap(new MapKeyName("length"), new MapValueNumber(6)))));
  }
}