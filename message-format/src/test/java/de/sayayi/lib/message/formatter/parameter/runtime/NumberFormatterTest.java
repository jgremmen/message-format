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

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.parameter.named.ChoiceFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static java.lang.Double.*;
import static java.lang.Math.PI;
import static java.util.Locale.GERMANY;
import static java.util.Locale.UK;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link NumberFormatter}: formattable types, decimal formats and non finite values matched against numeric and
 * non numeric string keys.
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
  void testDoubleFormat()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new NumberFormatter()))
        .setLocale(GERMANY);

    assertEquals("3,14", messageSupport
        .message("%{pi,number:'#0.00'}")
        .with("pi", PI)
        .format());

    assertEquals("0,1", messageSupport
        .message("%{d}")
        .with("d", 0.1d)
        .format());
  }


  @Test
  @DisplayName("Compare non-finite values to numeric string keys")
  void testNonFiniteValueWithNumericStringKey()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new NumberFormatter(), new ChoiceFormatter()))
        .setLocale(UK);
    val message = messageSupport.message("%{x,format:choice,'1':'one',:'other'}");

    assertEquals("one", message.with("x", 1.0).format());
    assertEquals("other", message.with("x", 2.0).format());
    assertEquals("other", message.with("x", NaN).format());
    assertEquals("other", message.with("x", POSITIVE_INFINITY).format());
    assertEquals("other", message.with("x", NEGATIVE_INFINITY).format());
    assertEquals("other", message.with("x", Float.NaN).format());

    val relational = messageSupport.message("%{x,format:choice,>'1000':'large',<>'1':'not one',:'other'}");

    assertEquals("large", relational.with("x", 2000.0).format());
    assertEquals("other", relational.with("x", POSITIVE_INFINITY).format());
    assertEquals("other", relational.with("x", NEGATIVE_INFINITY).format());
    assertEquals("other", relational.with("x", NaN).format());
    assertEquals("other", relational.with("x", Float.POSITIVE_INFINITY).format());
  }


  @Test
  @DisplayName("Compare non-finite values to non-numeric string keys")
  void testNonFiniteValueWithNonNumericStringKey()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new NumberFormatter(), new ChoiceFormatter()))
        .setLocale(UK);
    val message = messageSupport.message(
        "%{x,format:choice,'abc':'abc','NaN':'nan','Infinity':'+inf','-Infinity':'-inf',:'other'}");

    assertEquals("other", message.with("x", NaN).format());
    assertEquals("other", message.with("x", POSITIVE_INFINITY).format());
    assertEquals("other", message.with("x", NEGATIVE_INFINITY).format());
    assertEquals("other", message.with("x", Float.NaN).format());
    assertEquals("other", message.with("x", 1.5).format());
  }
}
