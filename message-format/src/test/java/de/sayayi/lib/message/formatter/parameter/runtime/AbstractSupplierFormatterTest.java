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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;


/**
 * Common tests for formatters of a single value holder, like a supplier or an atomic value: the formattable type, the
 * formatting of {@code null} and the formatting of the held values.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@TestInstance(PER_CLASS)
abstract class AbstractSupplierFormatterTest extends AbstractFormatterTest
{
  /**
   * Returns a new instance of the formatter under test.
   */
  protected abstract @NotNull ParameterFormatter createFormatter();


  /**
   * Returns the value holder type the formatter reports as formattable.
   */
  protected abstract @NotNull Class<?> getFormattableType();


  /**
   * Returns the locale used to format the values.
   */
  protected abstract @NotNull Locale getLocale();


  /**
   * Returns the values to format, as pairs of (value holder, expected text).
   */
  protected abstract @NotNull Stream<Arguments> formattedValues();


  @Test
  @DisplayName("Formattable types")
  void formattableTypes() {
    assertFormatterForType(createFormatter(), getFormattableType());
  }


  @Test
  @DisplayName("Null is formatted as null text")
  void formatNull() {
    assertEquals(nullText(), format(createMessageAccessor(getLocale(), createFormatter()), null));
  }


  @ParameterizedTest(name = "-> {1}")
  @DisplayName("Held value is formatted")
  @MethodSource("formattedValues")
  void formatHeldValue(Object value, Text expected) {
    assertEquals(expected, format(createMessageAccessor(getLocale(), createFormatter()), value));
  }
}
