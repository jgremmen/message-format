/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

import static java.util.Collections.emptySet;


/**
 * @author Jeroen Gremmen
 */
public interface NamedParameterFormatter extends ParameterFormatter
{
  /**
   * Tells the name of this data formatter
   *
   * @return  data formatter name, never {@code null}
   */
  @Contract(pure = true)
  @NotNull String getName();


  /**
   * Tells whether this formatter is capable of formatting values of {@code type}.
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


  @Override
  @Unmodifiable
  default @NotNull Set<FormattableType> getFormattableTypes() {
    return emptySet();
  }
}