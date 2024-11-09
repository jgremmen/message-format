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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.time.format.DateTimeFormatter.*;
import static java.time.format.FormatStyle.*;
import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
public final class TemporalFormatter extends AbstractParameterFormatter<Temporal>
{
  private static final Map<String,String> STYLE = Map.ofEntries(
      Map.entry("short", "SS"),
      Map.entry("medium", "MM"),
      Map.entry("long", "LL"),
      Map.entry("full", "FF"),
      Map.entry("date", "M-"),
      Map.entry("time", "-M"));

  private static final Map<String,DateTimeFormatter> FORMATTER = Map.ofEntries(
      Map.entry("SS", ofLocalizedDateTime(SHORT, SHORT)),
      Map.entry("S-", ofLocalizedDate(SHORT)),
      Map.entry("-S", ofLocalizedTime(SHORT)),

      Map.entry("MM", ofLocalizedDateTime(MEDIUM, MEDIUM)),
      Map.entry("M-", ofLocalizedDate(MEDIUM)),
      Map.entry("-M", ofLocalizedTime(MEDIUM)),

      Map.entry("LL", ofLocalizedDateTime(LONG, LONG)),
      Map.entry("LM", ofLocalizedDateTime(LONG, MEDIUM)),
      Map.entry("L-", ofLocalizedDate(LONG)),
      Map.entry("-L", ofLocalizedTime(LONG)),

      Map.entry("FF", ofLocalizedDateTime(FULL, FULL)),
      Map.entry("F-", ofLocalizedDate(FULL)),
      Map.entry("-F", ofLocalizedTime(FULL)));


  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Temporal value)
  {
    final String format = context.getConfigValueString("date").orElse(null);
    final DateTimeFormatter formatter;

    if (format != null && !STYLE.containsKey(format))
      formatter = ofPattern(requireNonNull(format));
    else
    {
      final char[] style = (format == null ? "MM" : STYLE.get(format)).toCharArray();

      if (value instanceof LocalDate)
        style[1] = '-';
      else if (value instanceof LocalTime)
        style[0] = '-';

      if ((formatter = FORMATTER.get(new String(style))) == null)
        return emptyText();
    }

    String text = formatter
        .withZone(ZoneId.systemDefault())
        .withLocale(context.getLocale())
        .format(value).trim();
/*
    // strip trailing timezone for local time
    if (value instanceof LocalTime && ("long".equals(format) || "full".equals(format)))
    {
      int idx = text.lastIndexOf(' ');
      if (idx > 0)
        text = text.substring(0, idx);
    }
*/
    return noSpaceText(text);
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return Set.of(
        new FormattableType(LocalDate.class),
        new FormattableType(LocalTime.class),
        new FormattableType(LocalDateTime.class),
        new FormattableType(Temporal.class));
  }
}
