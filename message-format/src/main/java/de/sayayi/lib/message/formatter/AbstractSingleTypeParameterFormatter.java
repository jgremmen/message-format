/*
 * Copyright 2023 Jeroen Gremmen
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


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public abstract class AbstractSingleTypeParameterFormatter<T> extends AbstractParameterFormatter<T>
{
  @Override
  public final @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(getFormattableType());
  }


  /**
   * Returns the sole java type which is supported by this formatter.
   * <p>
   * On registration {@link FormatterService.WithRegistry#addFormatter(ParameterFormatter)}
   * existing types which are also supported by this formatter will co-exist with each other.
   * The order attribute determines which formatter is preferred before the other. If different
   * formatters have both the same type and order, the formatter precedence is determined by
   * the class name. The behavior is deterministic, but it is encouraged to select different
   * order values for those cases.
   *
   * @return  the supported java type for this formatter, not {@code null}
   *
   * @see FormattableType#compareTo(FormattableType)
   * @see #getFormattableTypes()
   */
  @Contract(pure = true)
  public abstract @NotNull FormattableType getFormattableType();
}
