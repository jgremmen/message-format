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
package de.sayayi.lib.message.formatter.parameter.named;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.internal.part.map.key.MapKeyBool;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.TypedValue;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link BoolFormatter}: formattable types, formatting of boolean and convertible values, named use and map key
 * matching for null, empty, boolean and string keys.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
@DisplayName("'bool' formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class BoolFormatterTest extends AbstractFormatterTest
{
  @Test
  @DisplayName("Formattable types include boolean and Boolean")
  void formattableTypes()
  {
    assertFormatterForType(new BoolFormatter(), boolean.class);
    assertFormatterForType(new BoolFormatter(), Boolean.class);
  }


  @Test
  @DisplayName("Bool map keys select the message for booleans and for numbers selected by name; null is formatted as null text")
  void formatWithBoolMapKeys()
  {
    var messageAccessor = createMessageAccessor(GERMANY, new BoolFormatter());

    var map = new HashMap<MapKey,TypedValue.MessageValue>();
    map.put(MapKeyBool.TRUE, new TypedValueMessage(messageAccessor.getMessageFactory().parseMessage("wahr")));
    map.put(MapKeyBool.FALSE, new TypedValueMessage(messageAccessor.getMessageFactory().parseMessage("falsch")));

    assertEquals(noSpaceText("wahr"), format(messageAccessor, TRUE, Map.of(), map));
    assertEquals(noSpaceText("falsch"), format(messageAccessor, 0.0d, Map.of(), map, "bool"));
    assertEquals(noSpaceText("wahr"), format(messageAccessor, -0.0001f, Map.of(), map, "bool"));
    assertEquals(noSpaceText("wahr"), format(messageAccessor, -4, Map.of(), map, "bool"));
    assertEquals(nullText(), format(messageAccessor, null, Map.of(), map, "bool"));
  }


  @Test
  @DisplayName("Booleans are formatted as true or false, numbers only if the bool formatter is selected by name")
  void formatter()
  {
    var messageSupport = createMessageSupport(ENGLISH, new BoolFormatter());

    assertEquals("false true 1234 true no 3.14", messageSupport
        .message("%{a} %{b} %{c} %{c,format:bool} %{d,format:bool,true:'yes',false:'no'} %{e}")
        .with("a", FALSE)
        .with("b", TRUE)
        .with("c", Integer.valueOf(1234))
        .with("d", Integer.valueOf(0))
        .with("e", Double.valueOf(3.14d))
        .format());
  }


  static Stream<Arguments> convertedValues()
  {
    return Stream.of(
        arguments(true, "yes"),
        arguments(false, "no"),
        arguments((byte)1, "yes"),
        arguments((short)0, "no"),
        arguments(2, "yes"),
        arguments(0L, "no"),
        arguments(1.5f, "yes"),
        arguments(-0.0d, "no"),
        arguments(Double.NaN, "invalid"),
        arguments(Double.POSITIVE_INFINITY, "invalid"),
        arguments(Float.NEGATIVE_INFINITY, "invalid"),
        arguments(BigInteger.ONE, "yes"),
        arguments(new BigDecimal("0.00"), "no"),
        arguments(new AtomicInteger(3), "yes"),
        arguments("true", "yes"),
        arguments("false", "no"),
        arguments("1", "yes"),
        arguments("0.0", "no"),
        arguments("maybe", "invalid"),
        arguments("", "invalid"),
        arguments(Optional.of(1), "yes"),
        arguments(Optional.of("false"), "no"),
        arguments(Optional.empty(), "invalid"),
        arguments(Optional.of(Double.NaN), "invalid"),
        arguments(OptionalInt.of(0), "no"),
        arguments(OptionalInt.empty(), "invalid"),
        arguments(OptionalLong.of(5), "yes"),
        arguments(OptionalLong.empty(), "invalid"),
        arguments(null, "unset"));
  }


  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("Selected by name, all formattable types are converted to a boolean")
  @MethodSource("convertedValues")
  void selectedByName(Object value, String expected)
  {
    assertEquals(expected, MessageSupportFactory
        .createDefault()
        .message("%{b,format:bool,true:yes,false:no,empty:invalid,null:unset}")
        .with("b", value)
        .format());
  }


  static Stream<Arguments> typedValues()
  {
    return Stream.of(
        arguments(true, "yes"),
        arguments(false, "no"),
        arguments(2, "2"),
        arguments(0L, "0"),
        arguments("true", "true"),
        arguments("maybe", "maybe"),
        arguments(Optional.of(1), "1"),
        arguments(OptionalLong.of(5), "5"));
  }


  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("Selected by type for boolean values only, other types are not converted")
  @MethodSource("typedValues")
  void selectedByType(Object value, String expected)
  {
    assertEquals(expected, MessageSupportFactory
        .createDefault()
        .message("%{b,true:yes,false:no,empty:invalid}")
        .with("b", value)
        .format());
  }


  @Test
  @DisplayName("Selected by name, null, true and false are formatted with their mapped messages")
  void namedFormatter()
  {
    var messageSupport = createMessageSupport(GERMAN, new BoolFormatter());

    var msg = messageSupport.message("%{b,format:bool,null:'<unknown>',true:'yes',false:'no'}").getMessage();

    assertEquals("<unknown>", messageSupport.message(msg).with("b", null).format());
    assertEquals("yes", messageSupport.message(msg).with("b", true).format());
    assertEquals("no", messageSupport.message(msg).with("b", false).format());
  }


  @Test
  @DisplayName("Null and not-null keys match null and boolean values; an unmapped null is formatted as empty text")
  void configKeyNull()
  {
    var messageSupport = MessageSupportFactory.createDefault();
    var message = messageSupport
        .message("%{b,empty:empty,!empty:'not empty',null:null,!null:'not null'}");

    assertEquals("null", message
        .with("b", null)
        .format());

    assertEquals("not null", message
        .with("b", TRUE)
        .format());

    assertEquals("", messageSupport
        .message("%{b,format:bool}")
        .with("b", null)
        .format());

  }


  @Test
  @DisplayName("Empty keys match null and values that cannot be converted to a boolean, but not false")
  void configKeyEmpty()
  {
    var messageSupport = MessageSupportFactory.createDefault();

    assertEquals("not empty", messageSupport
        .message("%{b,empty:empty,!empty:'not empty',null:null}")
        .with("b", FALSE)
        .format());

    assertEquals("empty", messageSupport
        .message("%{b,empty:empty,!empty:'not empty'}")
        .with("b", null)
        .format());

    assertEquals("empty", messageSupport
        .message("%{b,format:bool,empty:empty,!empty:'not empty'}")
        .with("b", "very true")
        .format());
  }


  static Stream<Arguments> boolKeyCases()
  {
    final var byType = "%{b,true:true,false:false}";
    final var byName = "%{b,format:bool,true:true,false:false}";

    return Stream.of(
        arguments(byType, true, "true"),
        arguments(byType, false, "false"),
        arguments(byType, Optional.of(false), "false"),

        // int value
        arguments(byName, -24, "true"),
        arguments(byName, 0, "false"),
        arguments(byName, Optional.of(4), "true"),

        // float value
        arguments(byName, 3.14f, "true"),
        arguments(byName, 0.0f, "false"),

        // big decimal value
        arguments(byName, BigDecimal.TEN, "true"),
        arguments(byName, BigDecimal.ZERO, "false"),
        arguments(byName, Optional.of(BigDecimal.ONE), "true"),

        // string value
        arguments(byName, "true", "true"),
        arguments(byName, "false", "false"),
        arguments(byName, Optional.of("false"), "false"),

        // optional int value
        arguments(byName, OptionalInt.of(-24), "true"),
        arguments(byName, OptionalInt.of(0), "false"),
        arguments(byName, OptionalInt.empty(), ""),

        // optional long value
        arguments(byName, OptionalLong.of(Long.MAX_VALUE), "true"),
        arguments(byName, OptionalLong.of(0), "false"),
        arguments(byName, OptionalLong.empty(), ""));
  }


  @ParameterizedTest(name = "{0} with {1} -> {2}")
  @DisplayName("Bool map keys select the message matching the boolean value")
  @MethodSource("boolKeyCases")
  void configKeyBool(@Language("MessageFormat") String format, Object value, String expected)
  {
    assertEquals(
        expected,
        MessageSupportFactory
            .createDefault()
            .message(format)
            .with("b", value)
            .format());
  }


  static Stream<Arguments> stringKeyCases()
  {
    final var boolAndStringKeys = """
        %{b,format:bool,\
            true:'bool true',\
            false:'bool false',\
            'true':'string true',\
            'false':'string false',\
            'True':'string True'}\
        """;
    final var stringKeysOnly = "%{b,format:bool,'false':'string false','True':'string True'}";
    final var byType = "%{b,'false':'string false','true':'string true'}";

    return Stream.of(
        arguments(boolAndStringKeys, TRUE, "bool true"),
        arguments(boolAndStringKeys, "true", "bool true"),
        arguments(boolAndStringKeys, "1", "bool true"),
        arguments(boolAndStringKeys, FALSE, "bool false"),
        arguments(boolAndStringKeys, "false", "bool false"),
        arguments(boolAndStringKeys, "0", "bool false"),

        // "True" cannot be converted to a boolean, and the equal string key 'True' does not match either
        arguments(boolAndStringKeys, "True", ""),

        // with string keys only, the boolean value itself is formatted
        arguments(stringKeysOnly, TRUE, "true"),
        arguments(stringKeysOnly, "true", "true"),
        arguments(stringKeysOnly, FALSE, "false"),
        arguments(stringKeysOnly, "false", "false"),

        // selected by type for a boolean value, string keys don't match either
        arguments(byType, TRUE, "true"),
        arguments(byType, FALSE, "false"));
  }


  @ParameterizedTest(name = "{0} with {1} -> {2}")
  @DisplayName("String map keys never match a value converted to a boolean")
  @MethodSource("stringKeyCases")
  void configKeyString(@Language("MessageFormat") String format, Object value, String expected)
  {
    assertEquals(
        expected,
        MessageSupportFactory
            .createDefault()
            .message(format)
            .with("b", value)
            .format());
  }
}
