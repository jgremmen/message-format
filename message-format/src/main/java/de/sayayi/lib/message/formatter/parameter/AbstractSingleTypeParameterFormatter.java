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
package de.sayayi.lib.message.formatter.parameter;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


/**
 * Convenience base class for parameter formatters that support exactly one {@link FormattableType}.
 * <p>
 * Subclasses only need to implement {@link #getFormattableType()} (singular) instead of
 * {@link ParameterFormatter#getFormattableTypes() getFormattableTypes()}, which is implemented
 * as a {@code final} method delegating to the former.
 * <p>
 * This class inherits the {@code null}/empty value handling from {@link AbstractParameterFormatter},
 * so subclasses only need to provide {@link #formatValue(ParameterFormatterContext, Object)}.
 *
 * @param <T>  the parameter value type handled by this formatter
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public abstract class AbstractSingleTypeParameterFormatter<T> extends AbstractParameterFormatter<T>
{
  /**
   * Returns a singleton set containing the {@link FormattableType} returned by
   * {@link #getFormattableType()}.
   * <p>
   * This method is {@code final}; subclasses must override {@link #getFormattableType()} instead.
   *
   * @return  an immutable singleton set with the sole supported type, never {@code null}
   */
  @Override
  public final @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(getFormattableType());
  }


  /**
   * Returns the sole java type which is supported by this formatter.
   * <p>
   * On registration via {@link FormatterService.WithRegistry#addFormatter(ParameterFormatter)}, existing formatters
   * for the same type will co-exist with this formatter. The {@linkplain FormattableType#getOrder() order} attribute
   * determines which formatter is preferred. If different formatters share the same type and order, precedence is
   * determined by the class name. The behavior is deterministic, but it is encouraged to select different order
   * values for those cases.
   *
   * @return  the supported java type for this formatter, not {@code null}
   *
   * @see #getFormattableTypes()
   */
  @Contract(pure = true)
  protected abstract @NotNull FormattableType getFormattableType();
}
