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
package de.sayayi.lib.message.part.config.value;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.message.TextMessage;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Tests {@link de.sayayi.lib.message.part.TypedValue TypedValue} implementations for boolean, number, string and
 * message values.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@DisplayName("Typed values")
class TypedValueTest
{
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  @DisplayName("A bool value returns its boolean value")
  void bool(boolean value)
  {
    var bool = value ? TypedValueBool.TRUE : TypedValueBool.FALSE;

    assertEquals(value, bool.booleanValue());
    assertEquals(value, bool.asObject());
  }


  @Test
  @DisplayName("A number value returns its long value and an int value clamped to the int range")
  void number()
  {
    var number = new TypedValueNumber(1234);

    assertEquals(1234, number.longValue());
    assertEquals(1234, number.intValue());
    assertEquals(Long.valueOf(1234), number.asObject());

    assertEquals(Integer.MAX_VALUE, new TypedValueNumber(Long.MAX_VALUE).intValue());
    assertEquals(Integer.MIN_VALUE, new TypedValueNumber(Long.MIN_VALUE).intValue());
  }


  @Test
  @DisplayName("A string value rejects null and is used as text, not parsed as message")
  void string()
  {
    var messageFactory = MessageFactory.getSharedInstance();

    //noinspection DataFlowIssue
    assertThrows(Exception.class, () -> new TypedValueString(messageFactory, null));

    var string = new TypedValueString(messageFactory, "Hello %{s}");

    assertEquals("Hello %{s}", string.stringValue());
    assertEquals("Hello %{s}", string.asObject());

    // the string is used as is, not parsed as a message format
    assertEquals(new TextMessage(new TextPart("Hello %{s}")), string.asMessage());
  }


  @Test
  @DisplayName("A message value rejects null and returns its message")
  void message()
  {
    //noinspection DataFlowIssue
    assertThrows(Exception.class, () -> new TypedValueMessage(null));

    var messageFactory = MessageFactory.getSharedInstance();
    var msg = messageFactory.parseMessage("%{a,format:bool} %{s}.");

    var message = new TypedValueMessage(msg);

    assertEquals(messageFactory.parseMessage("%{a, format:bool } %{ s }."), message.asObject());
  }
}
