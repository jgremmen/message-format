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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static java.util.Collections.emptySet;


/**
 * Parameter formatter implementing this interface can be selected by specifying the formatter
 * name in a message parameter. If, for instance, this formatter is named {@code xyz}, it can
 * be addressed in a message as follows: {@code 'text %{p,xyz}.'}
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public interface NamedParameterFormatter extends ParameterFormatter
{
  /**
   * Tells the name of this parameter formatter
   *
   * @return  parameter formatter name, never {@code null}
   */
  @Contract(pure = true)
  @NotNull String getName();


  /**
   * Tells whether this formatter is capable of formatting values of {@code type}.
   * <p>
   * If a parameter format is specified this method is invoked with the type of the value to
   * be formatted. The return value determines whether this formatter is used ({@code true}) or
   * another formatter is selected ({@code false}).
   * <p>
   * If the value to be formatted is {@code null} a special
   * {@link ParameterFormatter#NULL_TYPE NULL_TYPE} is passed to this method.
   *
   * @param type  type to check, not {@code null}
   *
   * @return  {@code true} if this formatter is capable of formatting values of {@code type},
   *          {@code false} otherwise
   *
   * @since 0.8.0
   */
  @Contract(pure = true)
  default boolean canFormat(@NotNull Class<?> type) {
    return true;
  }


  /**
   * {@inheritDoc}
   *
   * @return  unmodifiable empty set, never {@code null}
   */
  @Override
  default @NotNull Set<FormattableType> getFormattableTypes() {
    return emptySet();
  }
}
