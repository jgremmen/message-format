/**
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public class NumberFormatter implements ParameterFormatter
{
  private static final BoolFormatter BOOL_FORMATTER = new BoolFormatter();


  @Override
  public String format(Object v, String format, Parameters parameters, ParameterData data)
  {
    if (v == null)
      return null;

    final Number value = (Number)v;

    if (data == null && (format == null || "integer".equals(format)) &&
        (value instanceof BigInteger || value instanceof Long || value instanceof Integer || value instanceof Short ||
         value instanceof Byte))
      return value.toString();

    // special case: show number as bool
    if ("bool".equals(format))
      return formatBoolean(value, parameters, data);

    return getFormatter(format, data, parameters.getLocale()).format(value);
  }


  private String formatBoolean(Number value, Parameters parameters, ParameterData data)
  {
    ParameterFormatter formatter = parameters.getFormatter("bool", Boolean.class);
    Set<Class<?>> types = formatter.getFormattableTypes();

    // if we got some default formatter, use a specific one instead
    if (!types.contains(Boolean.class) && !types.contains(boolean.class))
      formatter = BOOL_FORMATTER;

    return formatter.format(value, "bool", parameters, data);
  }


  protected NumberFormat getFormatter(String format, ParameterData data, Locale locale)
  {
    if (data instanceof ParameterString)
      return new DecimalFormat(((ParameterString)data).getValue(), new DecimalFormatSymbols(locale));

    if ("integer".equals(format))
      return NumberFormat.getIntegerInstance(locale);

    if ("percent".equals(format))
      return NumberFormat.getPercentInstance(locale);

    if ("currency".equals(format))
      return NumberFormat.getCurrencyInstance(locale);

    return NumberFormat.getNumberInstance(locale);
  }


  @Override
  public Set<Class<?>> getFormattableTypes()
  {
    return new HashSet<Class<?>>(Arrays.<Class<?>>asList(
        BigDecimal.class,
        BigInteger.class,
        Double.class, double.class,
        Float.class, float.class,
        Long.class, long.class,
        Integer.class, int.class,
        Short.class, short.class,
        Byte.class, byte.class));
  }
}
