/*
 * Copyright 2022 Jeroen Gremmen
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
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.MapKeyName;
import de.sayayi.lib.message.data.map.MapValueString;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Locale;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
class EnumFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertFormatterForType(new EnumFormatter(), Enum.class);
  }


  @Test
  public void testFormat()
  {
    final EnumFormatter formatter = new EnumFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(),
        NO_CACHE_INSTANCE, Locale.ROOT);
    final Parameters parameters = context.noParameters();

    //assertEquals(nullText(), formatter.format(context, null, null, parameters, null));
    //assertEquals(noSpaceText("CC"), formatter.format(context, MyEnum.CC, null, parameters, null));
    assertEquals(noSpaceText("3"), formatter.format(context, MyEnum.DD, null, parameters,
        new DataMap(Collections.singletonMap(new MapKeyName("enum"), new MapValueString("ordinal")))));

    context.setDefaultData("enum", "ordinal");
    assertEquals(noSpaceText("0"), formatter.format(context, MyEnum.AA, null, parameters, null));
  }




  public enum MyEnum {
    AA, BB, CC, DD
  }
}