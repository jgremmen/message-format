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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.util.TimeZone;


/**
 * Parameter formatter for {@link TimeZone} values.
 * <p>
 * This formatter renders the time zone using its locale-specific display name obtained from
 * {@link TimeZone#getDisplayName(java.util.Locale)}.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class TimeZoneFormatter extends AbstractSingleTypeParameterFormatter<TimeZone>
{
  /**
   * {@inheritDoc}
   * <p>
   * Formats the time zone as its locale-specific display name.
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull TimeZone timeZone) {
    return context.format(timeZone.getDisplayName(context.getLocale()));
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link TimeZone}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(TimeZone.class);
  }
}
