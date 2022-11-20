/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.*;
import static java.time.format.DateTimeFormatter.*;
import static java.time.format.FormatStyle.*;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class Java8DateTimeFormatter extends AbstractParameterFormatter
{
  private static final Map<String,String> STYLE = new HashMap<>();
  private static final Map<String,DateTimeFormatter> FORMATTER = new HashMap<>();

  static
  {
    STYLE.put("short", "SS");
    STYLE.put("medium", "MM");
    STYLE.put("long", "LL");
    STYLE.put("full", "FF");
    STYLE.put("date", "M-");
    STYLE.put("time", "-M");

    FORMATTER.put("SS", ofLocalizedDateTime(SHORT, SHORT));
    FORMATTER.put("S-", ofLocalizedDate(SHORT));
    FORMATTER.put("-S", ofLocalizedTime(SHORT));

    FORMATTER.put("MM", ofLocalizedDateTime(MEDIUM, MEDIUM));
    FORMATTER.put("M-", ofLocalizedDate(MEDIUM));
    FORMATTER.put("-M", ofLocalizedTime(MEDIUM));

    FORMATTER.put("LL", ofLocalizedDateTime(LONG, LONG));
    FORMATTER.put("LM", ofLocalizedDateTime(LONG, MEDIUM));
    FORMATTER.put("L-", ofLocalizedDate(LONG));
    FORMATTER.put("-L", ofLocalizedTime(LONG));

    FORMATTER.put("FF", ofLocalizedDateTime(FULL, FULL));
    FORMATTER.put("F-", ofLocalizedDate(FULL));
    FORMATTER.put("-F", ofLocalizedTime(FULL));

    FORMATTER.put("--", null);
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, DataMap map)
  {
    if (value == null)
      return nullText();

    if (!STYLE.containsKey(format))
      format = getConfigValueString(messageContext, "date", parameters, map);

    final DateTimeFormatter formatter;

    if (format != null && !STYLE.containsKey(format))
      formatter = DateTimeFormatter.ofPattern(format);
    else
    {
      final char[] style = (format == null ? "MM" : STYLE.get(format)).toCharArray();

      if (value instanceof LocalDate)
        style[1] = '-';
      else if (value instanceof LocalTime || value instanceof OffsetTime)
        style[0] = '-';

      if ((formatter = FORMATTER.get(new String(style))) == null)
        return emptyText();
    }

    String text = formatter
        .withZone(ZoneId.systemDefault())
        .withLocale(parameters.getLocale())
        .format((Temporal)value).trim();

    // strip trailing timezone for local time
    if (value instanceof LocalTime && ("long".equals(format) || "full".equals(format)))
    {
      int idx = text.lastIndexOf(' ');
      if (idx > 0)
        text = text.substring(0, idx);
    }

    return noSpaceText(text);
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return singleton(Temporal.class);
  }
}