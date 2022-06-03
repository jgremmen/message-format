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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataNumber;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
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
    BitsFormatter formatter = new BitsFormatter();

    assertTrue(formatter.getFormattableTypes().isEmpty());
    assertEquals("bits", formatter.getName());
  }


  @Test
  public void testByte()
  {
    BitsFormatter formatter = new BitsFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    Parameters parameters = context.noParameters();

    assertEquals(noSpaceText("11111111"), formatter.format(context, (byte)0xff, null, parameters, null));
    assertEquals(noSpaceText("00000000"), formatter.format(context, (byte)0, null, parameters, null));
    assertEquals(noSpaceText("10101010"), formatter.format(context, (byte)0xaa, null, parameters, null));
    assertEquals(noSpaceText("01010101"), formatter.format(context, (byte)0x55, null, parameters, null));
    assertEquals(noSpaceText("10101"), formatter.format(context, (byte)0x15, null, parameters, new DataString("auto")));
    assertEquals(noSpaceText("0"), formatter.format(context, (byte)0, null, parameters, new DataString("auto")));
    assertEquals(noSpaceText("101"), formatter.format(context, (byte)0x15, null, parameters, new DataNumber(3)));
    assertEquals(nullText(), formatter.format(context, null, null, parameters, null));
    assertEquals(nullText(), formatter.format(context, "hello", null, parameters, null));
  }


  @Test
  public void testShort()
  {
    BitsFormatter formatter = new BitsFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    Parameters parameters = context.noParameters();

    assertEquals(noSpaceText("1111111111111111"), formatter.format(context, (short)0xffff, null, parameters, null));
    assertEquals(noSpaceText("0000000000000000"), formatter.format(context, (short)0, null, parameters, null));
    assertEquals(noSpaceText("1010101010101010"), formatter.format(context, (short)0xaaaa, null, parameters, null));
    assertEquals(noSpaceText("0101010101010101"), formatter.format(context, (short)0x5555, null, parameters, null));
    assertEquals(noSpaceText("101010101"), formatter.format(context, (short)0x155, null, parameters, new DataString("auto")));
    assertEquals(noSpaceText("0"), formatter.format(context, (short)0, null, parameters, new DataString("auto")));
    assertEquals(noSpaceText("010110"), formatter.format(context, (byte)0x456, null, parameters, new DataNumber(6)));
  }
}