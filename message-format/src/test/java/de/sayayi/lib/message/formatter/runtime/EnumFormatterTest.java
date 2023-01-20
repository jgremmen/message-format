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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.data.map.MapKeyName;
import de.sayayi.lib.message.data.map.MapValueString;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static java.util.Collections.singletonMap;
import static java.util.Locale.ROOT;
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
    val context = new MessageContext(createFormatterService(new EnumFormatter()), NO_CACHE_INSTANCE, ROOT);

    //assertEquals(nullText(), formatter.format(context, null, null, parameters, null));
    //assertEquals(noSpaceText("CC"), formatter.format(context, MyEnum.CC, null, parameters, null));
    assertEquals(noSpaceText("3"), format(context, MyEnum.DD,
        singletonMap(new MapKeyName("enum"), new MapValueString("ordinal"))));

    context.setDefaultData("enum", "ordinal");
    assertEquals(noSpaceText("0"), format(context, MyEnum.AA));
  }




  public enum MyEnum {
    AA, BB, CC, DD
  }
}