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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("Enum formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class EnumFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertFormatterForType(new EnumFormatter(), Enum.class);
  }


  @Test
  void testFormat()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new EnumFormatter()))
        .setLocale(ROOT);
    val messageAccessor = messageSupport.getMessageAccessor();

    assertEquals(noSpaceText("3"), format(messageAccessor, MyEnum.DD,
        Map.of("enum", new TypedValueString("ordinal")), Map.of()));

    messageSupport.setDefaultConfig("enum", "ordinal");
    assertEquals(noSpaceText("0"), format(messageAccessor, MyEnum.AA));
  }


  @Test
  void testMapWithoutDefault()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new EnumFormatter()))
        .getMessageAccessor();

    assertEquals("upper", MessageFactory.getSharedInstance().parseMessage("%{e,>'C':upper,<'C':lower}")
        .format(messageAccessor, Map.of("e", MyEnum.CC)));

    assertEquals("", MessageFactory.getSharedInstance().parseMessage("%{e,>'C':upper}")
        .format(messageAccessor, Map.of("e", MyEnum.AA)));
  }


  @Test
  void testMapWithDefault()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new EnumFormatter()))
        .getMessageAccessor();

    assertEquals("C or D", MessageFactory.getSharedInstance()
        .parseMessage("%{e,'AA':A,'BB':B,:'C or D'}")
        .format(messageAccessor, Map.of("e", MyEnum.CC)));
  }




  public enum MyEnum {
    AA, BB, CC, DD
  }
}
