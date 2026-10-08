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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


/**
 * Base class for parameter formatters that format values of one {@link FormattableType}.
 * <p>
 * Subclasses provide the supported type and implement {@link #formatValue(ParameterFormatterContext, Object)} to
 * format values of that type. Null and empty value handling is provided by {@link AbstractParameterFormatter}.
 *
 * @param <T>  the parameter value type handled by this formatter
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public abstract class AbstractSingleTypeParameterFormatter<T> extends AbstractParameterFormatter<T>
{
  /**
   * Updates the classifiers for a value handled by this formatter.
   *
   * @param context  classifier context to update, not {@code null}
   * @param value    value to classify, not {@code null}
   *
   * @return  {@code true} if classification is complete, {@code false} to allow other formatters to add classifiers
   *
   * @since 0.21.0
   */
  @Override
  @SuppressWarnings("unchecked")
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value) {
    return updateTypedClassifiers(context, (T)value);
  }


  /**
   * Updates classifiers for a value of this formatter's type.
   * <p>
   * Override this method to add classifiers specific to values handled by this formatter.
   *
   * @param context  classifier context to update, not {@code null}
   * @param value    value to classify, not {@code null}
   *
   * @return  {@code true} if classification is complete, {@code false} to allow other formatters to add classifiers
   *
   * @since 0.21.0
   */
  @Contract(pure = true)
  protected boolean updateTypedClassifiers(@NotNull ClassifierContext context, @NotNull T value) {
    return false;
  }


  /**
   * Returns the single supported type declared by {@link #getFormattableType()}.
   *
   * @return  a set containing the supported type, never {@code null}
   *
   * @see #getFormattableType()
   */
  @Override
  public final @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(getFormattableType());
  }


  /**
   * Returns the type supported by this formatter.
   *
   * @return  the supported type, not {@code null}
   *
   * @see #getFormattableTypes()
   */
  @Contract(pure = true)
  protected abstract @NotNull FormattableType getFormattableType();
}
