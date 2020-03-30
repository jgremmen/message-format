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
import de.sayayi.lib.message.data.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.base.BaseLocal;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public final class JodaDateTimeFormatter extends AbstractParameterFormatter
{
  private static final Map<String,String> STYLE = new HashMap<String,String>();


  static
  {
    STYLE.put("short", "SS");
    STYLE.put("medium", "MM");
    STYLE.put("long", "LL");
    STYLE.put("full", "FF");
    STYLE.put("date", "M-");
    STYLE.put("time", "-M");
  }


  @Override
  @Contract(pure = true)
  public String format(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return formatNull(parameters, data);

    if (!STYLE.containsKey(format))
      format = getConfigValueString("format", data, true, null);

    final Locale locale = parameters.getLocale();
    final DateTimeFormatter formatter;

    if (format != null && !STYLE.containsKey(format))
      formatter = DateTimeFormat.forPattern(format).withLocale(locale);
    else
    {
      final char[] style = (format == null ? "MM" : STYLE.get(format)).toCharArray();

      if (value instanceof LocalDate)
        style[1] = '-';
      else if (value instanceof LocalTime)
        style[0] = '-';

      if (style[0] == '-' && style[1] == '-')
        return formatEmpty(parameters, data);

      formatter = DateTimeFormat.forStyle(new String(style)).withLocale(locale);
    }

    return (value instanceof ReadablePartial
        ? formatter.print((ReadablePartial)value)
        : formatter.print((ReadableInstant)value)).trim();
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return new HashSet<Class<?>>(Arrays.<Class<?>>asList(BaseLocal.class, ReadableDateTime.class));
  }
}
