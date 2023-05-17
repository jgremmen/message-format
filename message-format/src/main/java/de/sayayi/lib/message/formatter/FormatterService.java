/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.runtime.StringFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;


/**
 * This class provides parameter formatters for all java types.
 *
 * @author Jeroen Gremmen
 */
public interface FormatterService
{
  /**
   * Returns a list of parameter formatters for the given {@code format} and {@code type}.
   * <p>
   * Implementing classes must make sure that for any combination of {@code format} and {@code type}
   * this function always returns at least 1 formatter. A good choice for a default formatter would
   * be {@link StringFormatter}.
   *
   * @param format  name of the formatter or {@code null}
   * @param type    type of the value to format
   *
   * @return  unmodifiable list of prioritized parameter formatters,
   *          never {@code null} and never empty
   *
   * @see GenericFormatterService
   */
  @Contract(pure = true)
  @NotNull List<ParameterFormatter> getFormatters(String format, @NotNull Class<?> type);




  /**
   * Add registry functionality to a formatter service.
   */
  @SuppressWarnings("UnstableApiUsage")
  interface WithRegistry extends FormatterService
  {
    @Contract(mutates = "this")
    void addFormatterForType(@NotNull FormattableType formattableType,
                             @NotNull ParameterFormatter formatter);


    /**
     * Add parameter formatter to the formatter service.
     *
     * @param formatter  parameter formatter
     */
    @Contract(mutates = "this")
    void addFormatter(@NotNull ParameterFormatter formatter);
  }
}
