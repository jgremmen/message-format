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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * {@link Parameters} implementation that contains no parameters. All parameter lookups return {@code null} and the
 * parameter name set is always empty. Only the {@linkplain #getLocale() locale} is provided.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@SuppressWarnings("ClassCanBeRecord")
public final class NoParameters implements Parameters
{
  private final Locale locale;


  /**
   * Constructs a new object with no parameters for the given {@code locale}.
   *
   * @param locale  locale, not {@code null}
   */
  public NoParameters(@NotNull Locale locale) {
    this.locale = requireNonNull(locale, "locale must not be null");
  }


  @Override
  public @NotNull Locale getLocale() {
    return locale;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@code null}
   */
  @Override
  @Contract("_ -> null")
  public Object getParameterValue(@NotNull String parameter) {
    return null;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always an empty set
   */
  @Override
  public @NotNull Set<String> getParameterNames() {
    return Set.of();
  }


  @Override
  public String toString() {
    return "Parameters(locale='" + locale + "')";
  }
}
