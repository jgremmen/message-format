/*
 * Copyright 2024 Jeroen Gremmen
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

import de.sayayi.lib.message.Message.Parameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;


/**
 * A lightweight {@link Parameters} implementation that holds exactly one named parameter whose value can be updated
 * in-place via {@link #setValue(Object)}.
 * <p>
 * Requesting the value of any parameter name other than the one provided at construction time will return {@code null}.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class SingletonParameters implements Parameters
{
  private final Locale locale;
  private final String parameterName;

  private Object value;


  /**
   * Creates a new singleton parameters instance for the given locale and parameter name.
   * The initial parameter value is {@code null}.
   *
   * @param locale         the locale to use for message formatting, not {@code null}
   * @param parameterName  the name of the single parameter, not {@code null}
   */
  public SingletonParameters(@NotNull Locale locale, @NotNull String parameterName)
  {
    this.locale = locale;
    this.parameterName = parameterName;
  }


  /**
   * Sets the value for the single parameter held by this instance.
   * <p>
   * This method returns {@code this} to allow for a fluent usage pattern, e.g.
   * {@code message.format(accessor, parameters.setValue(element))}.
   *
   * @param value  the new parameter value, may be {@code null}
   *
   * @return  this instance, never {@code null}
   */
  @Contract(value = "_ -> this")
  public @NotNull SingletonParameters setValue(Object value)
  {
    this.value = value;
    return this;
  }


  /**
   * {@inheritDoc}
   *
   * @return  the locale provided at construction time, never {@code null}
   */
  @Override
  public @NotNull Locale getLocale() {
    return locale;
  }


  /**
   * Returns the parameter value if {@code parameter} matches the name provided at construction time, or {@code null}
   * otherwise.
   *
   * @param parameter  parameter name, not {@code null}
   *
   * @return  the current value if the name matches, {@code null} otherwise
   */
  @Override
  public Object getParameterValue(@NotNull String parameter) {
    return parameterName.equals(parameter) ? value : null;
  }


  /**
   * Returns an immutable singleton set containing the parameter name provided at construction time.
   *
   * @return  a singleton set with the parameter name, never {@code null}
   */
  @Override
  public @NotNull Set<String> getParameterNames() {
    return Set.of(parameterName);
  }


  @Override
  public String toString() {
    return "Parameters(locale=" + locale + ",{" + parameterName + '=' + value + "})";
  }
}
