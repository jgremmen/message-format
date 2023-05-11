/*
 * Copyright 2022 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.parameter.value.ConfigValueString;
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
  void testFormat()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new EnumFormatter()), NO_CACHE_INSTANCE)
        .setLocale(ROOT);
    val messageAccessor = messageSupport.getMessageAccessor();

    assertEquals(noSpaceText("3"), format(messageAccessor, MyEnum.DD,
        singletonMap(new ConfigKeyName("enum"), new ConfigValueString("ordinal"))));

    messageSupport.setDefaultParameterConfig("enum", "ordinal");
    assertEquals(noSpaceText("0"), format(messageAccessor, MyEnum.AA));
  }


  @Test
  void testMapWithoutDefault()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new EnumFormatter()), NO_CACHE_INSTANCE)
        .getMessageAccessor();

    assertEquals("upper", NO_CACHE_INSTANCE.parseMessage("%{e,>'C':upper,<'C':lower}")
        .format(messageAccessor, singletonMap("e", MyEnum.CC)));

    assertEquals("AA", NO_CACHE_INSTANCE.parseMessage("%{e,>'C':upper}")
        .format(messageAccessor, singletonMap("e", MyEnum.AA)));
  }


  @Test
  void testMapWithDefault()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new EnumFormatter()), NO_CACHE_INSTANCE)
        .getMessageAccessor();

    assertEquals("C or D", NO_CACHE_INSTANCE
        .parseMessage("%{e,'AA':A,'BB':B,:'C or D'}")
        .format(messageAccessor, singletonMap("e", MyEnum.CC)));
  }




  public enum MyEnum {
    AA, BB, CC, DD
  }
}
