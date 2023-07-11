/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyBool;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class BoolFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormattableTypes()
  {
    assertFormatterForType(new BoolFormatter(), boolean.class);
    assertFormatterForType(new BoolFormatter(), Boolean.class);
  }


  @Test
  void testFormat()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new BoolFormatter()), NO_CACHE_INSTANCE)
        .setLocale("de-DE")
        .getMessageAccessor();

    val map = new HashMap<ConfigKey,ConfigValue>();
    map.put(ConfigKeyBool.TRUE, new ConfigValueString("wahr"));
    map.put(ConfigKeyBool.FALSE, new ConfigValueString("falsch"));

    assertEquals(noSpaceText("wahr"), format(messageAccessor, Boolean.TRUE, map));
    assertEquals(noSpaceText("falsch"), format(messageAccessor, 0.0d, map, "bool"));
    assertEquals(noSpaceText("wahr"), format(messageAccessor, -0.0001f, map, "bool"));
    assertEquals(noSpaceText("falsch"), format(messageAccessor, "FALSE", map, "bool"));
    assertEquals(noSpaceText("wahr"), format(messageAccessor, "TrUe", map, "bool"));
    assertEquals(noSpaceText("wahr"), format(messageAccessor, -4, map, "bool"));
    assertEquals(nullText(), format(messageAccessor, (Object)null, map, "bool"));
  }


  @Test
  void testFormatter()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new BoolFormatter()), NO_CACHE_INSTANCE)
        .setLocale(ENGLISH);

    assertEquals("false true 1234 true no 3.14", messageSupport
        .message("%{a} %{b} %{c} %{c,bool} %{d,bool,true:'yes',false:'no'} %{e}")
        .with("a", Boolean.FALSE)
        .with("b", Boolean.TRUE)
        .with("c", Integer.valueOf(1234))
        .with("d", Integer.valueOf(0))
        .with("e", Double.valueOf(3.14d))
        .format());
  }


  @Test
  void testNamedFormatter()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new BoolFormatter()), NO_CACHE_INSTANCE)
        .setLocale(GERMAN);

    val msg = messageSupport.message("%{b,bool,null:'<unknown>',true:'yes',false:'no'}").getMessage();

    assertEquals("<unknown>", messageSupport.message(msg).with("b", null).format());
    assertEquals("yes", messageSupport.message(msg).with("b", true).format());
    assertEquals("no", messageSupport.message(msg).with("b", false).format());
  }
}
