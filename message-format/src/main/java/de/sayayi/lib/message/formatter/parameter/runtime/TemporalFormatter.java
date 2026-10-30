/*
 * Copyright 2019 Jeroen Gremmen
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
import de.sayayi.lib.message.formatter.parameter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQueries;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_TEMPORAL;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.time.format.DateTimeFormatter.*;
import static java.time.format.FormatStyle.*;
import static java.time.temporal.ChronoField.*;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Objects.requireNonNull;


/**
 * Parameter formatter for {@link Temporal} values (e.g. {@link java.time.LocalDate}, {@link java.time.LocalDateTime},
 * {@link java.time.Instant}).
 * <p>
 * The output format is controlled by the {@code date} configuration key, which accepts a predefined style name or a
 * custom {@link DateTimeFormatter} pattern.
 * <p>
 * The formatter automatically detects whether the temporal value supports date fields, time fields, or both and
 * adjusts the output accordingly. All formatting is locale-aware using the formatting context's locale. Values with
 * an explicit zone or offset retain it; zone-less values and instants use the system default zone.
 * <p>
 * Formatting never throws an exception. If the temporal value lacks a field required by the selected style or custom
 * pattern (e.g. a {@link java.time.Year} or {@link java.time.YearMonth} formatted with a date style that requires the
 * day of month, or a {@link java.time.LocalTime} formatted with pattern {@code "yyyy"}), or if the custom pattern is
 * invalid, the result is an empty text.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the temporal formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code date}</td>
 *     <td>
 *       {@code short}, {@code medium}, {@code long}, {@code full}, {@code date}, {@code time}, {@link
 *       DateTimeFormatter} pattern
 *     </td>
 *     <td>{@code medium}</td>
 *     <td>
 *       The first four values format the date and time in the given style, limited to the fields supported by the
 *       value. {@code date} and {@code time} format the date or the time only, in medium style. Any other value
 *       is used as a pattern, e.g. {@code yyyy-MM-dd}.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * Format a date in a predefined style. The output depends on the locale of the message:
 * <pre>{@code
 * var date = LocalDate.of(2026, 5, 1);
 *
 * messageSupport.message("%{d,date:long}").with("d", date).locale(Locale.US).format();
 * // "May 1, 2026"
 *
 * messageSupport.message("%{d,date:full}").with("d", date).locale(Locale.GERMANY).format();
 * // "Freitag, 1. Mai 2026"
 * }</pre>
 * <p>
 * Use a custom pattern, which also has access to the offset of the value:
 * <pre>{@code
 * var timestamp = OffsetDateTime.of(2026, 5, 1, 14, 30, 0, 0, ZoneOffset.ofHours(2));
 *
 * messageSupport
 *     .message("%{ts,date:\"yyyy-MM-dd'T'HH:mmxxx\"}")
 *     .with("ts", timestamp)
 *     .format();
 * // "2026-05-01T14:30+02:00"
 * }</pre>
 * <p>
 * Only the date of a date and time value:
 * <pre>{@code
 * messageSupport
 *     .message("Created on %{ts,date:date}")
 *     .with("ts", LocalDateTime.of(2026, 5, 1, 14, 30))
 *     .locale(Locale.UK)
 *     .format();
 * // "Created on 1 May 2026"
 * }</pre>
 * <p>
 * A value lacking a field required by the pattern results in an empty text, which the {@code empty} key can handle. The
 * {@code null} key handles a missing value:
 * <pre>{@code
 * var message = "Time: %{t,date:'HH:mm',empty:'not available',null:'not set'}";
 *
 * messageSupport.message(message).with("t", LocalTime.of(9, 5)).format();
 * // "Time: 09:05"
 *
 * messageSupport.message(message).with("t", YearMonth.of(2026, 5)).format();
 * // "Time: not available"
 *
 * messageSupport.message(message).with("t", null).format();
 * // "Time: not set"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class TemporalFormatter extends AbstractParameterFormatter<Temporal>
{
  /**
   * Maps style names to style codes. A style code consists of the date style followed by the time style, where
   * {@code S}, {@code M}, {@code L} and {@code F} denote short, medium, long and full and {@code -} denotes absence.
   */
  private static final Map<String,String> STYLE = ofEntries(
      entry("short", "SS"),
      entry("medium", "MM"),
      entry("long", "LL"),
      entry("full", "FF"),
      entry("date", "M-"),
      entry("time", "-M"));

  /**
   * Localized date/time formatters, keyed by style code.
   *
   * @see #STYLE
   */
  private static final Map<String,DateTimeFormatter> FORMATTER = ofEntries(
      entry("SS", ofLocalizedDateTime(SHORT, SHORT)),
      entry("S-", ofLocalizedDate(SHORT)),
      entry("-S", ofLocalizedTime(SHORT)),

      entry("MM", ofLocalizedDateTime(MEDIUM, MEDIUM)),
      entry("M-", ofLocalizedDate(MEDIUM)),
      entry("-M", ofLocalizedTime(MEDIUM)),

      entry("LL", ofLocalizedDateTime(LONG, LONG)),
      entry("LM", ofLocalizedDateTime(LONG, MEDIUM)),
      entry("L-", ofLocalizedDate(LONG)),
      entry("-L", ofLocalizedTime(LONG)),

      entry("FF", ofLocalizedDateTime(FULL, FULL)),
      entry("F-", ofLocalizedDate(FULL)),
      entry("-F", ofLocalizedTime(FULL)));


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
   * Formats the temporal value using the style or pattern specified by the {@code date} configuration key. If no
   * configuration is provided, the medium date/time style is used.
   * <p>
   * If the temporal value does not support all fields required by the style or pattern, or if the pattern is invalid,
   * an empty text is returned instead of throwing an exception.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Temporal temporal)
  {
    final var format = context.getConfigValueString("date").orElse(null);
    final DateTimeFormatter formatter;

    if (format != null && !STYLE.containsKey(format))
    {
      try {
        formatter = ofPattern(requireNonNull(format));
      } catch(IllegalArgumentException ex) {
        return emptyText();  // invalid pattern
      }
    }
    else
    {
      final var style = (format == null ? "MM" : STYLE.get(format)).toCharArray();

      if (!temporal.isSupported(YEAR) &&
          !temporal.isSupported(DAY_OF_MONTH) &&
          !temporal.isSupported(DAY_OF_WEEK) &&
          !temporal.isSupported(INSTANT_SECONDS))
        style[0] = '-';

      if (!temporal.isSupported(HOUR_OF_DAY) &&
          !temporal.isSupported(MILLI_OF_DAY) &&
          !temporal.isSupported(INSTANT_SECONDS))
        style[1] = '-';

      if ((formatter = FORMATTER.get(new String(style))) == null)
        return emptyText();
    }

    final var formatterWithZone = temporal.query(TemporalQueries.zone()) == null
        ? formatter.withZone(ZoneId.systemDefault())
        : formatter;

    try {
      return noSpaceText(formatterWithZone.withLocale(context.getLocale()).format(temporal));
    } catch(DateTimeException ex) {
      // temporal lacks a field required by the style or pattern (e.g. day of month for Year or YearMonth)
      return emptyText();
    }
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link Temporal} formattable type, never {@code null}
   */
  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Temporal.class));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "date"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("date");
  }
}
