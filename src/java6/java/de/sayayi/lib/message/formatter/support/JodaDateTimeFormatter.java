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
public class JodaDateTimeFormatter implements ParameterFormatter
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
  public String format(Object value, String format, @NotNull Parameters parameters, ParameterData data)
  {
    final Locale locale = parameters.getLocale();

    if (value instanceof ReadableDateTime)
      return getFormatter(format, data).withLocale(locale).print((ReadableInstant)value);

    final DateTimeFormatter formatter = getFormatter((BaseLocal)value, format, data);

    return (formatter == null) ? null : formatter.withLocale(locale).print((ReadablePartial)value).trim();
  }


  protected DateTimeFormatter getFormatter(String format, ParameterData data)
  {
    if (format == null && data instanceof ParameterString)
      return DateTimeFormat.forPattern(((ParameterString)data).getValue());

    final String style = STYLE.get(format);

    return DateTimeFormat.forStyle((style == null) ? "MM" : style);
  }


  protected DateTimeFormatter getFormatter(BaseLocal datetime, String format, ParameterData data)
  {
    if (format == null && data instanceof ParameterString)
      return DateTimeFormat.forPattern(((ParameterString)data).getValue());

    final String styleStr = STYLE.get(format);
    final char[] style = (styleStr != null) ? styleStr.toCharArray() : "MM".toCharArray();

    if (datetime instanceof LocalDate)
      style[1] = '-';
    else if (datetime instanceof LocalTime)
      style[0] = '-';

    return (style[0] == '-' && style[1] == '-') ? null : DateTimeFormat.forStyle(new String(style));
  }


  @NotNull
  @Override
  public Set<Class<?>> getFormattableTypes() {
    return new HashSet<Class<?>>(Arrays.<Class<?>>asList(BaseLocal.class, ReadableDateTime.class));
  }
}
