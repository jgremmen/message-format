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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.data.ParameterString;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.time.format.DateTimeFormatter.ofLocalizedDate;
import static java.time.format.DateTimeFormatter.ofLocalizedDateTime;
import static java.time.format.DateTimeFormatter.ofLocalizedTime;
import static java.time.format.FormatStyle.FULL;
import static java.time.format.FormatStyle.LONG;
import static java.time.format.FormatStyle.MEDIUM;
import static java.time.format.FormatStyle.SHORT;


/**
 * @author Jeroen Gremmen
 */
public final class Java8DateTimeFormatter implements ParameterFormatter
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
  public String format(Object value, String format, @NotNull Parameters parameters, ParameterData data)
  {
    final DateTimeFormatter formatter = getFormatter((Temporal)value, format, data);
    if (formatter == null)
      return null;

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

    return text;
  }


  private DateTimeFormatter getFormatter(Temporal datetime, String format, ParameterData data)
  {
    if (format == null && data instanceof ParameterString)
      return DateTimeFormatter.ofPattern(((ParameterString)data).getValue());

    final String styleStr = STYLE.get(format);
    final char[] style = (styleStr != null) ? styleStr.toCharArray() : "MM".toCharArray();

    if (datetime instanceof LocalDate)
      style[1] = '-';
    else if (datetime instanceof LocalTime || datetime instanceof OffsetTime)
      style[0] = '-';

    return FORMATTER.get(new String(style));
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(Temporal.class);
  }
}
