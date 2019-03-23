package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Context;
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
  public String format(String parameter, Object v, String format, Context context, ParameterData data)
  {
    if (v == null)
      return null;

    final Number value = (Number)v;

    if (data == null && (format == null || "integer".equals(format)) &&
        (value instanceof BigInteger || value instanceof Long || value instanceof Integer || value instanceof Short))
      return value.toString();

    // special case: show number as bool
    if (BOOL_FORMATTER.getName().equals(format))
      return formatBoolean(parameter, value, context, data);

    return getFormatter(format, data, context.getLocale()).format(value);
  }


  private String formatBoolean(String parameter, Number value, Context context, ParameterData data)
  {
    ParameterFormatter formatter = context.getFormatter("bool", Boolean.class);
    Set<Class<?>> types = formatter.getFormattableTypes();

    // if we got some default formatter, use a specific one instead
    if (!types.contains(Boolean.class) && !types.contains(boolean.class))
      formatter = BOOL_FORMATTER;

    return formatter.format(parameter, value, "bool", context, data);
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
        Short.class, short.class));
  }
}
