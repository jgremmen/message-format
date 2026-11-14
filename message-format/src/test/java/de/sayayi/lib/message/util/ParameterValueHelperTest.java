/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.util;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.formatter.parameter.SingletonParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static de.sayayi.lib.message.util.ParameterValueHelperTest.TestEnum.SOME_VALUE;
import static java.util.Locale.ROOT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link ParameterValueHelper}: conversion of parameter values to target types, including {@code null}, missing,
 * overflowing and optional wrapped values.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("ParameterValueHelper")
class ParameterValueHelperTest
{
  // ---- getBoolean ----

  @Nested
  @DisplayName("getBoolean")
  class GetBooleanTest
  {
    @Test
    @DisplayName("null value returns empty")
    void nullValue()
    {
      var params = parametersOf("p", null);
      assertTrue(ParameterValueHelper.getBoolean(params, "p").isEmpty());
    }


    @Test
    @DisplayName("missing parameter returns empty")
    void missingParameter()
    {
      var params = parametersOf("other", true);
      assertTrue(ParameterValueHelper.getBoolean(params, "p").isEmpty());
    }


    @Test
    @DisplayName("Boolean.TRUE returns true")
    void booleanTrue()
    {
      var params = parametersOf("p", Boolean.TRUE);
      assertEquals(Optional.of(true), ParameterValueHelper.getBoolean(params, "p"));
    }


    @Test
    @DisplayName("Boolean.FALSE returns false")
    void booleanFalse()
    {
      var params = parametersOf("p", Boolean.FALSE);
      assertEquals(Optional.of(false), ParameterValueHelper.getBoolean(params, "p"));
    }


    @Test
    @DisplayName("String 'true' (case-insensitive) returns true")
    void stringTrue()
    {
      assertEquals(Optional.of(true), ParameterValueHelper.getBoolean(parametersOf("p", "true"), "p"));
      assertEquals(Optional.of(true), ParameterValueHelper.getBoolean(parametersOf("p", "TRUE"), "p"));
      assertEquals(Optional.of(true), ParameterValueHelper.getBoolean(parametersOf("p", "True"), "p"));
    }


    @Test
    @DisplayName("String 'false' (case-insensitive) returns false")
    void stringFalse()
    {
      assertEquals(Optional.of(false), ParameterValueHelper.getBoolean(parametersOf("p", "false"), "p"));
      assertEquals(Optional.of(false), ParameterValueHelper.getBoolean(parametersOf("p", "FALSE"), "p"));
      assertEquals(Optional.of(false), ParameterValueHelper.getBoolean(parametersOf("p", "False"), "p"));
    }


    @Test
    @DisplayName("Non-boolean string returns empty")
    void nonBooleanString()
    {
      assertTrue(ParameterValueHelper.getBoolean(parametersOf("p", "yes"), "p").isEmpty());
      assertTrue(ParameterValueHelper.getBoolean(parametersOf("p", "1"), "p").isEmpty());
      assertTrue(ParameterValueHelper.getBoolean(parametersOf("p", ""), "p").isEmpty());
    }


    @Test
    @DisplayName("Optional wrapping Boolean is unwrapped")
    void optionalWrappedBoolean()
    {
      assertEquals(Optional.of(true),
          ParameterValueHelper.getBoolean(parametersOf("p", Optional.of(true)), "p"));
      assertEquals(Optional.of(false),
          ParameterValueHelper.getBoolean(parametersOf("p", Optional.of(false)), "p"));
    }


    @Test
    @DisplayName("Optional wrapping string is unwrapped")
    void optionalWrappedString()
    {
      assertEquals(Optional.of(true),
          ParameterValueHelper.getBoolean(parametersOf("p", Optional.of("true")), "p"));
      assertEquals(Optional.of(false),
          ParameterValueHelper.getBoolean(parametersOf("p", Optional.of("false")), "p"));
    }


    @Test
    @DisplayName("Empty Optional returns empty")
    void emptyOptional() {
      assertTrue(ParameterValueHelper.getBoolean(parametersOf("p", Optional.empty()), "p").isEmpty());
    }


    @Test
    @DisplayName("Non-convertible type returns empty")
    void nonConvertibleType()
    {
      assertTrue(ParameterValueHelper.getBoolean(parametersOf("p", 42), "p").isEmpty());
      assertTrue(ParameterValueHelper.getBoolean(parametersOf("p", new Object()), "p").isEmpty());
    }
  }




  // ---- getInt and getLong ----

  /**
   * Values that {@code getInt} and {@code getLong} convert to the same result.
   */
  static Stream<Arguments> numberValues()
  {
    return Stream.of(
        arguments("null", null, null),
        arguments("Double exact integer", 5.0, 5),
        arguments("Float exact integer", 5.0f, 5),
        arguments("Float fractional", 3.7f, null),
        arguments("BigDecimal exact integer", new BigDecimal("42.00"), 42),
        arguments("BigDecimal fractional", new BigDecimal("42.99"), null),
        arguments("BigDecimal exceeding long range", new BigDecimal("99999999999999999999"), null),
        arguments("Numeric string", "123", 123),
        arguments("Negative numeric string", "-5", -5),
        arguments("Non-numeric string", "abc", null),
        arguments("Empty string", "", null),
        arguments("Empty OptionalInt", OptionalInt.empty(), null),
        arguments("Empty OptionalLong", OptionalLong.empty(), null),
        arguments("Optional wrapping string", Optional.of("88"), 88),
        arguments("Empty Optional", Optional.empty(), null),
        arguments("Object", new Object(), null),
        arguments("Boolean", true, null)
    );
  }




  @Nested
  @DisplayName("getInt")
  class GetIntTest
  {
    @Test
    @DisplayName("missing parameter returns empty")
    void missingParameter() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("other", 1), "p").isEmpty());
    }


    static Stream<Arguments> intValues()
    {
      return Stream.of(
          arguments("Integer", 42, 42),
          arguments("Negative integer", -1, -1),
          arguments("Zero integer", 0, 0),
          arguments("Long within int range", 100L, 100),
          arguments("Long exceeding int range", Long.MAX_VALUE, null),
          arguments("Double fractional", 3.7, null),
          arguments("BigInteger within int range", BigInteger.valueOf(999), 999),
          arguments("BigInteger exceeding int range", BigInteger.valueOf(Long.MAX_VALUE), null),
          arguments("String exceeding int range", "99999999999999", null),
          arguments("OptionalInt", OptionalInt.of(7), 7),
          arguments("OptionalLong within int range", OptionalLong.of(10), 10),
          arguments("OptionalLong exceeding int range", OptionalLong.of(Long.MAX_VALUE), null),
          arguments("Optional wrapping number", Optional.of(55), 55)
      );
    }


    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @DisplayName("Values are converted to int or return empty")
    @MethodSource({ "intValues", "de.sayayi.lib.message.util.ParameterValueHelperTest#numberValues" })
    void valueConversion(@SuppressWarnings("unused") String name, Object value, Number expected)
    {
      assertEquals(
          expected == null ? OptionalInt.empty() : OptionalInt.of(expected.intValue()),
          ParameterValueHelper.getInt(parametersOf("p", value), "p"));
    }
  }




  @Nested
  @DisplayName("getLong")
  class GetLongTest
  {
    @Test
    @DisplayName("missing parameter returns empty")
    void missingParameter() {
      assertTrue(ParameterValueHelper.getLong(parametersOf("other", 1L), "p").isEmpty());
    }


    static Stream<Arguments> longValues()
    {
      return Stream.of(
          arguments("Long", 42L, 42L),
          arguments("Long maximum", Long.MAX_VALUE, Long.MAX_VALUE),
          arguments("Integer widened to long", 7, 7),
          arguments("Double fractional", 3.9, null),
          arguments("BigInteger within long range", BigInteger.valueOf(123456789L), 123456789L),
          arguments("BigInteger exceeding long range", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), null),
          arguments("String exceeding long range", "99999999999999999999", null),
          arguments("OptionalLong", OptionalLong.of(7), 7),
          arguments("OptionalInt widened to long", OptionalInt.of(10), 10),
          arguments("Optional wrapping number", Optional.of(55L), 55)
      );
    }


    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @DisplayName("Values are converted to long or return empty")
    @MethodSource({ "longValues", "de.sayayi.lib.message.util.ParameterValueHelperTest#numberValues" })
    void valueConversion(@SuppressWarnings("unused") String name, Object value, Number expected)
    {
      assertEquals(
          expected == null ? OptionalLong.empty() : OptionalLong.of(expected.longValue()),
          ParameterValueHelper.getLong(parametersOf("p", value), "p"));
    }
  }




  // ---- getEnum ----

  @Nested
  @DisplayName("getEnum")
  class GetEnumTest
  {
    @Test
    @DisplayName("null value returns empty")
    void nullValue()
    {
      assertTrue(ParameterValueHelper.getEnum(
          parametersOf("p", null), "p", TimeUnit.class).isEmpty());
    }


    @Test
    @DisplayName("missing parameter returns empty")
    void missingParameter()
    {
      assertTrue(ParameterValueHelper.getEnum(
          parametersOf("other", SECONDS), "p", TimeUnit.class).isEmpty());
    }


    @Test
    @DisplayName("Enum instance of matching type")
    void enumInstance()
    {
      assertEquals(Optional.of(SECONDS),
          ParameterValueHelper.getEnum(parametersOf("p", SECONDS), "p", TimeUnit.class));
    }


    @Test
    @DisplayName("String matching enum name exactly")
    void stringExactMatch()
    {
      assertEquals(Optional.of(SECONDS),
          ParameterValueHelper.getEnum(parametersOf("p", "SECONDS"), "p", TimeUnit.class));
    }


    @Test
    @DisplayName("String matching enum name case-insensitively")
    void stringCaseInsensitiveMatch()
    {
      assertEquals(Optional.of(SECONDS),
          ParameterValueHelper.getEnum(parametersOf("p", "seconds"), "p", TimeUnit.class));
      assertEquals(Optional.of(MILLISECONDS),
          ParameterValueHelper.getEnum(parametersOf("p", "Milliseconds"), "p", TimeUnit.class));
    }


    @Test
    @DisplayName("String with hyphens matches underscore enum names")
    void stringHyphenMatchesUnderscore()
    {
      assertEquals(Optional.of(SOME_VALUE),
          ParameterValueHelper.getEnum(parametersOf("p", "some-value"), "p", TestEnum.class));
      assertEquals(Optional.of(SOME_VALUE),
          ParameterValueHelper.getEnum(parametersOf("p", "SOME-VALUE"), "p", TestEnum.class));
    }


    @Test
    @DisplayName("Non-matching string returns empty")
    void nonMatchingString()
    {
      assertTrue(ParameterValueHelper.getEnum(
          parametersOf("p", "NONEXISTENT"), "p", TimeUnit.class).isEmpty());
      assertTrue(ParameterValueHelper.getEnum(
          parametersOf("p", ""), "p", TimeUnit.class).isEmpty());
    }


    @Test
    @DisplayName("Optional wrapping enum is unwrapped")
    void optionalWrappedString()
    {
      assertEquals(Optional.of(TimeUnit.DAYS),
          ParameterValueHelper.getEnum(parametersOf("p", Optional.of("DAYS")), "p", TimeUnit.class));
    }


    @Test
    @DisplayName("Optional wrapping enum constant is unwrapped")
    void optionalWrappedEnum()
    {
      assertEquals(Optional.of(SECONDS),
          ParameterValueHelper.getEnum(parametersOf("p", Optional.of(SECONDS)), "p", TimeUnit.class));
      assertTrue(ParameterValueHelper.getEnum(
          parametersOf("p", Optional.of(SECONDS)), "p", TestEnum.class).isEmpty());
    }


    @Test
    @DisplayName("Empty Optional returns empty")
    void emptyOptional()
    {
      assertTrue(ParameterValueHelper.getEnum(
          parametersOf("p", Optional.empty()), "p", TimeUnit.class).isEmpty());
    }


    @Test
    @DisplayName("Non-convertible type returns empty")
    void nonConvertibleType()
    {
      assertTrue(ParameterValueHelper.getEnum(
          parametersOf("p", 42), "p", TimeUnit.class).isEmpty());
      assertTrue(ParameterValueHelper.getEnum(
          parametersOf("p", new Object()), "p", TimeUnit.class).isEmpty());
    }


    @Test
    @DisplayName("Enum of wrong type returns empty")
    void wrongEnumType() {
      assertTrue(ParameterValueHelper.getEnum(parametersOf("p", SECONDS), "p", TestEnum.class).isEmpty());
    }
  }




  enum TestEnum { SOME_VALUE, ANOTHER }




  // ---- getString ----

  @Nested
  @DisplayName("getString")
  class GetStringTest
  {
    @Test
    @DisplayName("null value returns empty")
    void nullValue() {
      assertTrue(ParameterValueHelper.getString(parametersOf("p", null), "p").isEmpty());
    }


    @Test
    @DisplayName("missing parameter returns empty")
    void missingParameter() {
      assertTrue(ParameterValueHelper.getString(parametersOf("other", "hello"), "p").isEmpty());
    }


    @Test
    @DisplayName("String value")
    void stringValue()
    {
      assertEquals(Optional.of("hello"), ParameterValueHelper.getString(parametersOf("p", "hello"), "p"));
      assertEquals(Optional.of(""), ParameterValueHelper.getString(parametersOf("p", ""), "p"));
    }


    @Test
    @DisplayName("StringBuilder value converted to String")
    void stringBuilderValue()
    {
      assertEquals(Optional.of("test"),
          ParameterValueHelper.getString(parametersOf("p", new StringBuilder("test")), "p"));
    }


    @Test
    @DisplayName("StringBuffer value converted to String")
    void stringBufferValue()
    {
      assertEquals(Optional.of("buf"),
          ParameterValueHelper.getString(parametersOf("p", new StringBuffer("buf")), "p"));
    }


    @Test
    @DisplayName("Optional wrapping String is unwrapped")
    void optionalWrappedString()
    {
      assertEquals(Optional.of("wrapped"),
          ParameterValueHelper.getString(parametersOf("p", Optional.of("wrapped")), "p"));
    }


    @Test
    @DisplayName("Optional wrapping CharSequence is unwrapped")
    void optionalWrappedCharSequence()
    {
      assertEquals(Optional.of("sb"),
          ParameterValueHelper.getString(parametersOf("p", Optional.of(new StringBuilder("sb"))), "p"));
    }


    @Test
    @DisplayName("Empty Optional returns empty")
    void emptyOptional() {
      assertTrue(ParameterValueHelper.getString(parametersOf("p", Optional.empty()), "p").isEmpty());
    }


    @Test
    @DisplayName("Non-CharSequence type returns empty")
    void nonCharSequenceType()
    {
      assertTrue(ParameterValueHelper.getString(parametersOf("p", 42), "p").isEmpty());
      assertTrue(ParameterValueHelper.getString(parametersOf("p", true), "p").isEmpty());
      assertTrue(ParameterValueHelper.getString(parametersOf("p", new Object()), "p").isEmpty());
    }
  }




  /**
   * Creates a {@link Parameters} instance backed by a single key-value pair.
   */
  private static SingletonParameters parametersOf(String name, Object value) {
    return new SingletonParameters(ROOT, name).setValue(value);
  }
}
