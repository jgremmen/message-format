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

import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Locale;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link LongSupplierFormatter}: formattable types and formatting of the supplied value, also by the number
 * formatter.
 *
 * @author Jeroen Gremmen
 * @since 0.6.3
 */
@DisplayName("LongSupplier formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class LongSupplierFormatterTest extends AbstractSupplierFormatterTest
{
  @Override
  protected @NotNull ParameterFormatter createFormatter() {
    return new LongSupplierFormatter();
  }


  @Override
  protected @NotNull Class<?> getFormattableType() {
    return LongSupplier.class;
  }


  @Override
  protected @NotNull Locale getLocale() {
    return ROOT;
  }


  @Override
  protected @NotNull Stream<Arguments> formattedValues()
  {
    return Stream.of(
        arguments((LongSupplier)() -> 0, noSpaceText("0")),
        arguments((LongSupplier)() -> MAX_VALUE, noSpaceText(Long.toString(MAX_VALUE))),
        arguments((LongSupplier)() -> MIN_VALUE, noSpaceText(Long.toString(MIN_VALUE))),
        arguments((LongSupplier)() -> 123456789L, noSpaceText("123456789"))
    );
  }


  @Test
  @DisplayName("Supplied value is formatted by the number formatter")
  void testNumberFormat()
  {
    assertEquals(
        noSpaceText("1,234,567,890"),
        format(
            createMessageAccessor(ENGLISH, new NumberFormatter(), new LongSupplierFormatter()),
            (LongSupplier)() -> 1234567890L,
            "number",
            "###,###,###,###"));
  }
}
