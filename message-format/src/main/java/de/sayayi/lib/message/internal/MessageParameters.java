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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.internal.MessageSupportImpl.Configurer;
import de.sayayi.lib.message.util.SortedStringMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Locale;
import java.util.Map;


/**
 * Immutable snapshot of the locale and parameter values from a {@link Configurer}, used to pass formatting context to
 * {@link de.sayayi.lib.message.Message#format Message.format(...)}.
 * <p>
 * Parameter names are kept in sorted order to allow efficient binary-search based lookup by name.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
final class MessageParameters implements Parameters
{
  private final Locale locale;
  private final Map<String,Object> parameters;


  /**
   * Creates a new parameters snapshot from the given configurer's current state.
   *
   * @param configurer  configurer to copy locale and parameters from, not {@code null}
   */
  MessageParameters(@NotNull Configurer<?> configurer)
  {
    locale = configurer.locale;
    parameters = new SortedStringMap<>(configurer.parameters, true);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Locale getLocale() {
    return locale;
  }


  /** {@inheritDoc} */
  @Override
  public Object getParameterValue(@NotNull String parameter) {
    return parameters.get(parameter);
  }


  /**
   * {@inheritDoc}
   *
   * @return  unmodifiable map view of the parameter names and values
   */
  @Override
  public @Unmodifiable @NotNull Map<String,Object> asParameterMap() {
    return parameters;
  }


  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (!(o instanceof Parameters))
      return false;

    var that = (Parameters)o;

    return
        locale.equals(that.getLocale()) &&
        parameters.equals(that.asParameterMap());
  }


  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return locale.hashCode() + parameters.hashCode();
  }


  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "Parameters(locale=" + locale + ',' + parameters + ')';
  }
}
