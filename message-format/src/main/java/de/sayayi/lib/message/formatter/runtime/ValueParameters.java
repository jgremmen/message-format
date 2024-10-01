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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.Message.Parameters;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
final class ValueParameters implements Parameters
{
  private final Locale locale;
  private final String parameterName;
  Object value;


  ValueParameters(@NotNull Locale locale, @NotNull String parameterName)
  {
    this.locale = locale;
    this.parameterName = parameterName;
  }


  @Override
  public @NotNull Locale getLocale() {
    return locale;
  }


  @Override
  public Object getParameterValue(@NotNull String parameter) {
    return parameterName.equals(parameter) ? value : null;
  }


  @Override
  public @NotNull Set<String> getParameterNames() {
    return Set.of(parameterName);
  }


  @Override
  public String toString() {
    return "Parameters(locale='" + locale + "',{value=" + value + "})";
  }
}
