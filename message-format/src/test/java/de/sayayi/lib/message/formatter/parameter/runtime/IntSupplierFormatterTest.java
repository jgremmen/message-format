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
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link IntSupplierFormatter}: formattable types and formatting of the supplied value.
 *
 * @author Jeroen Gremmen
 * @since 0.6.3
 */
@DisplayName("IntSupplier formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class IntSupplierFormatterTest extends AbstractSupplierFormatterTest
{
  @Override
  protected @NotNull ParameterFormatter createFormatter() {
    return new IntSupplierFormatter();
  }


  @Override
  protected @NotNull Class<?> getFormattableType() {
    return IntSupplier.class;
  }


  @Override
  protected @NotNull Locale getLocale() {
    return ROOT;
  }


  @Override
  protected @NotNull Stream<Arguments> formattedValues()
  {
    return Stream.of(
        arguments((IntSupplier)() -> 0, noSpaceText("0")),
        arguments((IntSupplier)() -> MAX_VALUE, noSpaceText(Integer.toString(MAX_VALUE))),
        arguments((IntSupplier)() -> MIN_VALUE, noSpaceText(Integer.toString(MIN_VALUE))),
        arguments((IntSupplier)() -> 123456789, noSpaceText("123456789"))
    );
  }
}
