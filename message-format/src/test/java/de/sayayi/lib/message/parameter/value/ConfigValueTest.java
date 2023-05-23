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
package de.sayayi.lib.message.parameter.value;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.parameter.value.ConfigValue.Type;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * @author Jeroen Gremmen
 */
class ConfigValueTest
{
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void testBool(boolean value)
  {
    val bool = value ? ConfigValueBool.TRUE : ConfigValueBool.FALSE;

    assertEquals(Type.BOOL, bool.getType());
    assertEquals(value, bool.booleanValue());
    assertEquals(value, bool.asObject());
  }


  @Test
  void testNumber()
  {
    val number = new ConfigValueNumber(1234);

    assertEquals(Type.NUMBER, number.getType());
    assertEquals(1234, number.longValue());
    assertEquals(1234, number.intValue());
    assertEquals(Long.valueOf(1234), number.asObject());

    assertEquals(Integer.MAX_VALUE, new ConfigValueNumber(Long.MAX_VALUE).intValue());
    assertEquals(Integer.MIN_VALUE, new ConfigValueNumber(Long.MIN_VALUE).intValue());
  }


  @Test
  void testString()
  {
    //noinspection DataFlowIssue
    assertThrows(Exception.class, () -> new ConfigValueString(null));

    val string = new ConfigValueString("Hello %{s}");

    assertEquals(Type.STRING, string.getType());
    assertEquals("Hello %{s}", string.stringValue());
    assertEquals("Hello %{s}", string.asObject());

    val messageFactory = MessageFactory.NO_CACHE_INSTANCE;

    assertEquals(messageFactory.parseMessage("Hello  %{ s }"),
        string.asMessage(messageFactory));
  }


  @Test
  void testMessage()
  {
    //noinspection DataFlowIssue
    assertThrows(Exception.class, () -> new ConfigValueMessage(null));

    val messageFactory = MessageFactory.NO_CACHE_INSTANCE;
    val msg = messageFactory.parseMessage("%{a,bool} %{s}.");

    val message = new ConfigValueMessage(msg);

    assertEquals(Type.MESSAGE, message.getType());
    assertEquals(messageFactory.parseMessage("%{a, bool } %{ s }."), message.asObject());
    assertEquals(
        message.asObject(),
        new ConfigValueString("%{a, bool } %{ s }.").asMessage(messageFactory));
  }
}
