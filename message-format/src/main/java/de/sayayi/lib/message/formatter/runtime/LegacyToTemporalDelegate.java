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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.nio.file.attribute.FileTime;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.EMPTY_NULL_TYPE;


/**
 * This parameter formatter converts legacy date/time objects, like {@link Calendar} and {@link Date}, into a
 * {@link Temporal} representation and delegates formatting. In order to actually format those objects a
 * {@link Temporal} or more specific formatter is required.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class LegacyToTemporalDelegate implements ParameterFormatter
{
  @Override
  public @NotNull Text format(@NotNull FormatterContext context, Object value)
  {
    if (value == null)
    {
      // handle empty, !empty, null and !null first
      var msg = context
          .getConfigMapMessage(null, EMPTY_NULL_TYPE)
          .orElse(null);

      return msg != null ? context.format(msg) : nullText();
    }

    if (value instanceof java.sql.Time)
      value = ((java.sql.Time)value).toLocalTime();  // sql.Time has no instant
    else if (value instanceof java.sql.Date)
      value = ((java.sql.Date)value).toLocalDate();  // sql.Date has no instant
    else if (value instanceof Date)
      value = ((Date)value).toInstant();
    else if (value instanceof FileTime)
      value = ((FileTime)value).toInstant();
    else
      value = ((Calendar)value).toInstant();

    return context.format(value);
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return Set.of(
        new FormattableType(Calendar.class),
        new FormattableType(Date.class),
        new FormattableType(FileTime.class));
  }
}
