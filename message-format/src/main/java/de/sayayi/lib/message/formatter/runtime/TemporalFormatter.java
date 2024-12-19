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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.time.format.DateTimeFormatter.*;
import static java.time.format.FormatStyle.*;
import static java.time.temporal.ChronoField.*;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
public final class TemporalFormatter extends AbstractParameterFormatter<Temporal>
{
  private static final Map<String,String> STYLE = ofEntries(
      entry("short", "SS"),
      entry("medium", "MM"),
      entry("long", "LL"),
      entry("full", "FF"),
      entry("date", "M-"),
      entry("time", "-M"));

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


  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Temporal temporal)
  {
    var format = context.getConfigValueString("date").orElse(null);
    final DateTimeFormatter formatter;

    if (format != null && !STYLE.containsKey(format))
      formatter = ofPattern(requireNonNull(format));
    else
    {
      var style = (format == null ? "MM" : STYLE.get(format)).toCharArray();

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

    return noSpaceText(formatter
        .withZone(ZoneId.systemDefault())
        .withLocale(context.getLocale())
        .format(temporal));
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Temporal.class));
  }
}
