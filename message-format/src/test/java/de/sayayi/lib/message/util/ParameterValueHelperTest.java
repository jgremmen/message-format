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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

import static de.sayayi.lib.message.util.ParameterValueHelperTest.TestEnum.SOME_VALUE;
import static java.util.Locale.ROOT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test class for {@link ParameterValueHelper}.
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




  // ---- getInt ----

  @Nested
  @DisplayName("getInt")
  class GetIntTest
  {
    @Test
    @DisplayName("null value returns empty")
    void nullValue() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", null), "p").isEmpty());
    }


    @Test
    @DisplayName("missing parameter returns empty")
    void missingParameter() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("other", 1), "p").isEmpty());
    }


    @Test
    @DisplayName("Integer value")
    void integerValue()
    {
      assertEquals(OptionalInt.of(42), ParameterValueHelper.getInt(parametersOf("p", 42), "p"));
      assertEquals(OptionalInt.of(-1), ParameterValueHelper.getInt(parametersOf("p", -1), "p"));
      assertEquals(OptionalInt.of(0), ParameterValueHelper.getInt(parametersOf("p", 0), "p"));
    }


    @Test
    @DisplayName("Long value within int range")
    void longValue() {
      assertEquals(OptionalInt.of(100), ParameterValueHelper.getInt(parametersOf("p", 100L), "p"));
    }


    @Test
    @DisplayName("Long value exceeding int range returns empty")
    void longOverflow() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", Long.MAX_VALUE), "p").isEmpty());
    }


    @Test
    @DisplayName("Double exact integer value")
    void doubleExactValue() {
      assertEquals(OptionalInt.of(5), ParameterValueHelper.getInt(parametersOf("p", 5.0), "p"));
    }


    @Test
    @DisplayName("Double fractional value returns empty")
    void doubleFractionalValue() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", 3.7), "p").isEmpty());
    }


    @Test
    @DisplayName("Float exact integer value")
    void floatExactValue() {
      assertEquals(OptionalInt.of(5), ParameterValueHelper.getInt(parametersOf("p", 5.0f), "p"));
    }


    @Test
    @DisplayName("Float fractional value returns empty")
    void floatFractionalValue() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", 3.7f), "p").isEmpty());
    }


    @Test
    @DisplayName("BigInteger within int range")
    void bigIntegerValue()
    {
      assertEquals(OptionalInt.of(999),
          ParameterValueHelper.getInt(parametersOf("p", BigInteger.valueOf(999)), "p"));
    }


    @Test
    @DisplayName("BigInteger exceeding int range returns empty")
    void bigIntegerOverflow()
    {
      var huge = BigInteger.valueOf(Long.MAX_VALUE);
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", huge), "p").isEmpty());
    }


    @Test
    @DisplayName("BigDecimal exact integer value")
    void bigDecimalExactValue()
    {
      assertEquals(OptionalInt.of(42),
          ParameterValueHelper.getInt(parametersOf("p", new BigDecimal("42.00")), "p"));
    }


    @Test
    @DisplayName("BigDecimal fractional value returns empty")
    void bigDecimalFractionalValue() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", new BigDecimal("42.99")), "p").isEmpty());
    }


    @Test
    @DisplayName("BigDecimal exceeding int range returns empty")
    void bigDecimalOverflow()
    {
      var huge = new BigDecimal("99999999999999999999");
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", huge), "p").isEmpty());
    }


    @Test
    @DisplayName("Numeric string")
    void numericString()
    {
      assertEquals(OptionalInt.of(123), ParameterValueHelper.getInt(parametersOf("p", "123"), "p"));
      assertEquals(OptionalInt.of(-5), ParameterValueHelper.getInt(parametersOf("p", "-5"), "p"));
    }


    @Test
    @DisplayName("Non-numeric string returns empty")
    void nonNumericString()
    {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", "abc"), "p").isEmpty());
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", ""), "p").isEmpty());
    }


    @Test
    @DisplayName("String exceeding int range returns empty")
    void stringOverflow() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", "99999999999999"), "p").isEmpty());
    }


    @Test
    @DisplayName("OptionalInt value is passed through")
    void optionalIntValue()
    {
      assertEquals(OptionalInt.of(7),
          ParameterValueHelper.getInt(parametersOf("p", OptionalInt.of(7)), "p"));
    }


    @Test
    @DisplayName("Empty OptionalInt returns empty")
    void emptyOptionalInt() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", OptionalInt.empty()), "p").isEmpty());
    }


    @Test
    @DisplayName("OptionalLong within int range")
    void optionalLongValue()
    {
      assertEquals(OptionalInt.of(10),
          ParameterValueHelper.getInt(parametersOf("p", OptionalLong.of(10)), "p"));
    }


    @Test
    @DisplayName("OptionalLong exceeding int range returns empty")
    void optionalLongOverflow()
    {
      assertTrue(ParameterValueHelper.getInt(
          parametersOf("p", OptionalLong.of(Long.MAX_VALUE)), "p").isEmpty());
    }


    @Test
    @DisplayName("Empty OptionalLong returns empty")
    void emptyOptionalLong() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", OptionalLong.empty()), "p").isEmpty());
    }


    @Test
    @DisplayName("Optional wrapping number is unwrapped")
    void optionalWrappedNumber()
    {
      assertEquals(OptionalInt.of(55),
          ParameterValueHelper.getInt(parametersOf("p", Optional.of(55)), "p"));
    }


    @Test
    @DisplayName("Optional wrapping string is unwrapped")
    void optionalWrappedString()
    {
      assertEquals(OptionalInt.of(88),
          ParameterValueHelper.getInt(parametersOf("p", Optional.of("88")), "p"));
    }


    @Test
    @DisplayName("Empty Optional returns empty")
    void emptyOptional() {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", Optional.empty()), "p").isEmpty());
    }


    @Test
    @DisplayName("Non-convertible type returns empty")
    void nonConvertibleType()
    {
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", new Object()), "p").isEmpty());
      assertTrue(ParameterValueHelper.getInt(parametersOf("p", true), "p").isEmpty());
    }
  }




  // ---- getLong ----

  @Nested
  @DisplayName("getLong")
  class GetLongTest
  {
    @Test
    @DisplayName("null value returns empty")
    void nullValue() {
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", null), "p").isEmpty());
    }


    @Test
    @DisplayName("missing parameter returns empty")
    void missingParameter() {
      assertTrue(ParameterValueHelper.getLong(parametersOf("other", 1L), "p").isEmpty());
    }


    @Test
    @DisplayName("Long value")
    void longValue()
    {
      assertEquals(OptionalLong.of(42L), ParameterValueHelper.getLong(parametersOf("p", 42L), "p"));
      assertEquals(OptionalLong.of(Long.MAX_VALUE),
          ParameterValueHelper.getLong(parametersOf("p", Long.MAX_VALUE), "p"));
    }


    @Test
    @DisplayName("Integer value widened to long")
    void integerValue() {
      assertEquals(OptionalLong.of(7), ParameterValueHelper.getLong(parametersOf("p", 7), "p"));
    }


    @Test
    @DisplayName("Double exact integer value")
    void doubleExactValue() {
      assertEquals(OptionalLong.of(5), ParameterValueHelper.getLong(parametersOf("p", 5.0), "p"));
    }


    @Test
    @DisplayName("Double fractional value returns empty")
    void doubleFractionalValue() {
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", 3.9), "p").isEmpty());
    }


    @Test
    @DisplayName("Float exact integer value")
    void floatExactValue() {
      assertEquals(OptionalLong.of(5), ParameterValueHelper.getLong(parametersOf("p", 5.0f), "p"));
    }


    @Test
    @DisplayName("Float fractional value returns empty")
    void floatFractionalValue() {
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", 3.7f), "p").isEmpty());
    }


    @Test
    @DisplayName("BigInteger within long range")
    void bigIntegerValue()
    {
      assertEquals(OptionalLong.of(123456789L),
          ParameterValueHelper.getLong(parametersOf("p", BigInteger.valueOf(123456789L)), "p"));
    }


    @Test
    @DisplayName("BigInteger exceeding long range returns empty")
    void bigIntegerOverflow()
    {
      var huge = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", huge), "p").isEmpty());
    }


    @Test
    @DisplayName("BigDecimal exact integer value")
    void bigDecimalExactValue()
    {
      assertEquals(OptionalLong.of(42),
          ParameterValueHelper.getLong(parametersOf("p", new BigDecimal("42.00")), "p"));
    }


    @Test
    @DisplayName("BigDecimal fractional value returns empty")
    void bigDecimalFractionalValue() {
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", new BigDecimal("42.99")), "p").isEmpty());
    }


    @Test
    @DisplayName("BigDecimal exceeding long range returns empty")
    void bigDecimalOverflow()
    {
      var huge = new BigDecimal("99999999999999999999");
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", huge), "p").isEmpty());
    }


    @Test
    @DisplayName("Numeric string")
    void numericString()
    {
      assertEquals(OptionalLong.of(123), ParameterValueHelper.getLong(parametersOf("p", "123"), "p"));
      assertEquals(OptionalLong.of(-5), ParameterValueHelper.getLong(parametersOf("p", "-5"), "p"));
    }


    @Test
    @DisplayName("Non-numeric string returns empty")
    void nonNumericString()
    {
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", "abc"), "p").isEmpty());
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", ""), "p").isEmpty());
    }


    @Test
    @DisplayName("String exceeding long range returns empty")
    void stringOverflow()
    {
      assertTrue(ParameterValueHelper.getLong(
          parametersOf("p", "99999999999999999999"), "p").isEmpty());
    }


    @Test
    @DisplayName("OptionalLong value is passed through")
    void optionalLongValue()
    {
      assertEquals(OptionalLong.of(7),
          ParameterValueHelper.getLong(parametersOf("p", OptionalLong.of(7)), "p"));
    }


    @Test
    @DisplayName("Empty OptionalLong returns empty")
    void emptyOptionalLong() {
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", OptionalLong.empty()), "p").isEmpty());
    }


    @Test
    @DisplayName("OptionalInt value widened to long")
    void optionalIntValue()
    {
      assertEquals(OptionalLong.of(10),
          ParameterValueHelper.getLong(parametersOf("p", OptionalInt.of(10)), "p"));
    }


    @Test
    @DisplayName("Empty OptionalInt returns empty")
    void emptyOptionalInt() {
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", OptionalInt.empty()), "p").isEmpty());
    }


    @Test
    @DisplayName("Optional wrapping number is unwrapped")
    void optionalWrappedNumber()
    {
      assertEquals(OptionalLong.of(55),
          ParameterValueHelper.getLong(parametersOf("p", Optional.of(55L)), "p"));
    }


    @Test
    @DisplayName("Optional wrapping string is unwrapped")
    void optionalWrappedString()
    {
      assertEquals(OptionalLong.of(88),
          ParameterValueHelper.getLong(parametersOf("p", Optional.of("88")), "p"));
    }


    @Test
    @DisplayName("Empty Optional returns empty")
    void emptyOptional() {
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", Optional.empty()), "p").isEmpty());
    }


    @Test
    @DisplayName("Non-convertible type returns empty")
    void nonConvertibleType()
    {
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", new Object()), "p").isEmpty());
      assertTrue(ParameterValueHelper.getLong(parametersOf("p", true), "p").isEmpty());
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
