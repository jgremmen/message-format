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
import de.sayayi.lib.message.formatter.ParameterFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author Jeroen Gremmen
 */
public final class NumberFormatter extends AbstractParameterFormatter
{
  private static final BoolFormatter BOOL_FORMATTER = new BoolFormatter();


  @Override
  @Contract(pure = true)
  public String formatValue(Object v, String format, @NotNull Parameters parameters, Data data)
  {
    if (v == null)
      return null;

    final Number value = (Number)v;

    if (data == null && (format == null || "integer".equals(format)) &&
        (value instanceof BigInteger || value instanceof Long || value instanceof Integer || value instanceof Short ||
         value instanceof Byte || value instanceof AtomicInteger || value instanceof AtomicLong))
      return value.toString();

    // special case: show number as bool
    if ("bool".equals(format))
      return formatBoolean(value, parameters, data);

    return getFormatter(format, parameters, data).format(value);
  }


  private String formatBoolean(Number value, Parameters parameters, Data data)
  {
    ParameterFormatter formatter = parameters.getFormatter("bool", boolean.class);
    Set<Class<?>> types = formatter.getFormattableTypes();

    // if we got some default formatter, use a specific one instead
    if (!types.contains(Boolean.class) && !types.contains(boolean.class))
      formatter = BOOL_FORMATTER;

    return formatter.format(value, "bool", parameters, data);
  }


  protected NumberFormat getFormatter(String format, Parameters parameters, Data data)
  {
    if ("integer".equals(format))
      return NumberFormat.getIntegerInstance(parameters.getLocale());

    if ("percent".equals(format))
      return NumberFormat.getPercentInstance(parameters.getLocale());

    if ("currency".equals(format))
      return NumberFormat.getCurrencyInstance(parameters.getLocale());

    String customFormat = getConfigValueString("format", parameters, data, true, null);
    if (customFormat != null)
      return new DecimalFormat(customFormat, new DecimalFormatSymbols(parameters.getLocale()));

    return NumberFormat.getNumberInstance(parameters.getLocale());
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Number.class);
  }
}
