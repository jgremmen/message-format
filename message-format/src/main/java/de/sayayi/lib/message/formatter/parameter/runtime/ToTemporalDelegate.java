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
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.nio.file.attribute.FileTime;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_TEMPORAL;
import static de.sayayi.lib.message.part.MapKey.EMPTY_NULL_TYPE;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;


/**
 * This parameter formatter converts legacy date/time objects, like {@link Calendar} and {@link Date}, into a
 * {@link Temporal} representation and delegates formatting. In order to actually format those objects, a formatter
 * for {@link Instant}, {@link LocalDate} and {@link LocalTime} must be registered.
 * {@link TemporalFormatter}, which is part of this library, covers all three types out of the box.
 * <p>
 * <b>Examples:</b>
 * <p>
 * Legacy values can be used like their {@link Temporal} equivalents. The {@code date} configuration key of the {@link
 * TemporalFormatter} therefore applies to them as well:
 * <pre>{@code
 * messageSupport
 *     .message("%{d,date:long}")
 *     .with("d", java.sql.Date.valueOf("2026-05-01"))
 *     .locale(Locale.US)
 *     .format();
 * // "May 1, 2026"
 *
 * messageSupport
 *     .message("%{t,date:'HH:mm'}")
 *     .with("t", java.sql.Time.valueOf("14:30:00"))
 *     .format();
 * // "14:30"
 * }</pre>
 * <p>
 * {@link Date}, {@link java.nio.file.attribute.FileTime FileTime} and {@link InstantSource} values are instants, which
 * are formatted using the system default time zone. A {@link Calendar} is formatted in its own
 * {@linkplain Calendar#getTimeZone() time zone}:
 * <pre>{@code
 * var calendar = new GregorianCalendar(TimeZone.getTimeZone("Asia/Tokyo"));
 * calendar.set(2026, Calendar.MAY, 1, 8, 30, 0);
 *
 * messageSupport
 *     .message("%{c,date:'yyyy-MM-dd HH:mm z'}")
 *     .with("c", calendar)
 *     .locale(Locale.US)
 *     .format();
 * // "2026-05-01 08:30 JST"
 * }</pre>
 * <p>
 * {@code null} is matched by the {@code null} key:
 * <pre>{@code
 * messageSupport.message("%{d,null:'never'}").with("d", (Date)null).format();
 * // "never"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.12.0  (renamed in 0.20.0)
 */
public final class ToTemporalDelegate implements ParameterFormatter
{
  /**
   * Tells whether the optional {@code java.sql} module is available and readable. The {@code java.sql.Date} and
   * {@code java.sql.Time} classes are only loaded if it is, as they cannot occur as values otherwise.
   */
  private static final boolean JAVA_SQL_AVAILABLE = ModuleLayer
      .boot()
      .findModule("java.sql")
      .filter(ToTemporalDelegate.class.getModule()::canRead)
      .isPresent();


  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@link ClassifierContext#CLASSIFIER_TEMPORAL temporal} classifier.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier(CLASSIFIER_TEMPORAL);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Converts the given date/time value to its {@link Temporal} equivalent and delegates formatting. {@code null} values
   * are handled using the null/empty map key from the parameter configuration.
   */
  @Override
  @SuppressWarnings("IfCanBeSwitch")
  public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value)
  {
    if (value == null)
    {
      // handle empty, !empty, null and !null first
      final var msg = context
          .getMapMessage(null, EMPTY_NULL_TYPE)
          .orElse(null);

      return msg != null ? context.format(msg) : nullText();
    }

    if (JAVA_SQL_AVAILABLE)
    {
      if (value instanceof java.sql.Time sqlTime)
        return context.format(sqlTime.toLocalTime(), LocalTime.class);

      if (value instanceof java.sql.Date sqlDate)
        return context.format(sqlDate.toLocalDate(), LocalDate.class);
    }

    if (value instanceof Calendar calendar)
    {
      return context.format(
          ZonedDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId()),
          ZonedDateTime.class);
    }

    if (value instanceof InstantSource)
      value = ((InstantSource)value).instant();
    else if (value instanceof Date)
      value = ((Date)value).toInstant();
    else
      value = ((FileTime)value).toInstant();

    return context.format(value, Instant.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing formattable types for {@link Calendar}, {@link Date}, {@link FileTime} and
   *          {@link InstantSource}, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return Set.of(
        new FormattableType(Calendar.class),
        new FormattableType(Date.class),
        new FormattableType(FileTime.class),
        new FormattableType(InstantSource.class));
  }
}
