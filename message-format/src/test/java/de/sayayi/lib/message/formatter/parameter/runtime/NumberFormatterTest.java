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

import de.sayayi.lib.message.formatter.parameter.named.ChoiceFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.lang.Double.*;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link NumberFormatter}: formattable types, the {@code plain} and {@code integer} formats, decimal formats and
 * non-finite values matched against number keys, numeric and non-numeric string keys.
 *
 * @author Jeroen Gremmen
 * @since 0.9.1
 */
@DisplayName("Number formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class NumberFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormattableTypes()
  {
    val formatter = new NumberFormatter();

    assertFormatterForType(formatter, Number.class);
    assertFormatterForType(formatter, byte.class);
    assertFormatterForType(formatter, short.class);
    assertFormatterForType(formatter, int.class);
    assertFormatterForType(formatter, long.class);
    assertFormatterForType(formatter, float.class);
    assertFormatterForType(formatter, double.class);
  }


  @Test
  @DisplayName("Plain and integer formats")
  void testPlainAndIntegerFormat()
  {
    val messageSupport = createMessageSupport(US, new NumberFormatter());

    for(val value: new Number[] {
        1234567, 1234567L, BigInteger.valueOf(1234567), new AtomicLong(1234567), 1234567.0, new BigDecimal("1234567") })
    {
      assertEquals("1,234,567", messageSupport
          .message("%{v,number:integer}")
          .with("v", value)
          .format(), value.getClass().getSimpleName());
    }

    assertEquals("4", messageSupport
        .message("%{v,number:integer}")
        .with("v", 3.7)
        .format());

    for(val format: new String[] { "%{v}", "%{v,number:plain}" })
    {
      //noinspection LanguageMismatch
      val message = messageSupport.message(format);

      assertEquals("1234567", message.with("v", 1234567).format());
      assertEquals("1234567", message.with("v", 1234567L).format());
      assertEquals("1234567", message.with("v", BigInteger.valueOf(1234567)).format());
      assertEquals("1,234,567.5", message.with("v", 1234567.5).format());
      assertEquals("1,234,567", message.with("v", new BigDecimal("1234567")).format());
    }
  }


  static Stream<Arguments> nonFiniteValuesWithNumberKey()
  {
    val formatString = "%{x,format:choice,>5:'big',<=5:'small',:'other'}";

    return Stream.of(
        arguments(formatString, 6.0, "big"),
        arguments(formatString, 5.0, "small"),
        arguments(formatString, NaN, "other"),
        arguments(formatString, POSITIVE_INFINITY, "other"),
        arguments(formatString, NEGATIVE_INFINITY, "other"),
        arguments(formatString, Float.NaN, "other"),
        arguments("%{x,format:choice,<>1:'not one',:'other'}", NaN, "other")
    );
  }


  @ParameterizedTest(name = "{0} with {1} -> {2}")
  @DisplayName("Compare non-finite values to number keys")
  @MethodSource("nonFiniteValuesWithNumberKey")
  void testNonFiniteValueWithNumberKey(@Language("MessageFormat") String formatString, Number value, String expected)
  {
    assertEquals(
        expected,
        createMessageSupport(UK, new NumberFormatter(), new ChoiceFormatter())
            .message(formatString)
            .with("x", value)
            .format());
  }


  static Stream<Arguments> nonFiniteValuesWithNumericStringKey()
  {
    val formatString = "%{x,format:choice,'1':'one',:'other'}";
    val relationalFormatString = "%{x,format:choice,>'1000':'large',<>'1':'not one',:'other'}";

    return Stream.of(
        arguments(formatString, 1.0, "one"),
        arguments(formatString, 2.0, "other"),
        arguments(formatString, NaN, "other"),
        arguments(formatString, POSITIVE_INFINITY, "other"),
        arguments(formatString, NEGATIVE_INFINITY, "other"),
        arguments(formatString, Float.NaN, "other"),
        arguments(relationalFormatString, 2000.0, "large"),
        arguments(relationalFormatString, POSITIVE_INFINITY, "other"),
        arguments(relationalFormatString, NEGATIVE_INFINITY, "other"),
        arguments(relationalFormatString, NaN, "other"),
        arguments(relationalFormatString, Float.POSITIVE_INFINITY, "other")
    );
  }


  @ParameterizedTest(name = "{0} with {1} -> {2}")
  @DisplayName("Compare non-finite values to numeric string keys")
  @MethodSource("nonFiniteValuesWithNumericStringKey")
  void testNonFiniteValueWithNumericStringKey(@Language("MessageFormat") String formatString, Number value,
                                              String expected)
  {
    assertEquals(
        expected,
        createMessageSupport(UK, new NumberFormatter(), new ChoiceFormatter())
            .message(formatString)
            .with("x", value)
            .format());
  }


  static Stream<Arguments> nonFiniteValuesWithNonNumericStringKey()
  {
    val formatString = "%{x,format:choice,'abc':'abc','NaN':'nan','Infinity':'+inf','-Infinity':'-inf',:'other'}";

    return Stream.of(
        arguments(formatString, NaN, "other"),
        arguments(formatString, POSITIVE_INFINITY, "other"),
        arguments(formatString, NEGATIVE_INFINITY, "other"),
        arguments(formatString, Float.NaN, "other"),
        arguments(formatString, 1.5, "other")
    );
  }


  @ParameterizedTest(name = "{0} with {1} -> {2}")
  @DisplayName("Compare non-finite values to non-numeric string keys")
  @MethodSource("nonFiniteValuesWithNonNumericStringKey")
  void testNonFiniteValueWithNonNumericStringKey(@Language("MessageFormat") String formatString, Number value,
                                                 String expected)
  {
    assertEquals(expected, createMessageSupport(UK, new NumberFormatter(), new ChoiceFormatter())
        .message(formatString)
        .with("x", value)
        .format());
  }


  static Stream<Arguments> nonFiniteValuesWithBoolKey()
  {
    val formatString = "%{x,format:choice,true:'true',false:'false',:'other'}";

    return Stream.of(
        arguments(formatString, 1.5, "true"),
        arguments(formatString, 0.0, "false"),
        arguments(formatString, NaN, "other"),
        arguments(formatString, POSITIVE_INFINITY, "other"),
        arguments(formatString, NEGATIVE_INFINITY, "other"),
        arguments(formatString, Float.NaN, "other")
    );
  }


  @ParameterizedTest(name = "{0} with {1} -> {2}")
  @DisplayName("Compare non-finite values to bool keys")
  @MethodSource("nonFiniteValuesWithBoolKey")
  void testNonFiniteValueWithBoolKey(@Language("MessageFormat") String formatString, Number value, String expected)
  {
    assertEquals(
        expected,
        createMessageSupport(UK, new NumberFormatter(), new ChoiceFormatter())
            .message(formatString)
            .with("x", value)
            .format());
  }


  @Test
  @DisplayName("Cached number formats are used per format and locale")
  void testCachedNumberFormats()
  {
    val message = createMessageSupport(new NumberFormatter())
        .message("%{v,number:'#,##0.00'} %{v} %{v,number:percent}");

    for(var n = 0; n < 2; n++)
    {
      assertEquals(
          "12.345,68 12.345,678 1.234.568 %",
          message
              .with("v", 12345.678)
              .locale(GERMANY)
              .format());
      assertEquals(
          "12,345.68 12,345.678 1,234,568%",
          message
              .with("v", 12345.678)
              .locale(US)
              .format());
      assertEquals(
          "1,50 1,5 150 %",
          message
              .with("v", 1.5)
              .locale(GERMANY)
              .format());
    }
  }
}
