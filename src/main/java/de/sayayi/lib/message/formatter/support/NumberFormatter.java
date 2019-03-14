package de.sayayi.lib.message.formatter.support;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterString;


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

    if ((format == null || "integer".equals(format)) && data == null)
    {
      if (value instanceof BigInteger)
        return value.toString();

      if (value instanceof Long || value instanceof Integer || value instanceof Short)
        return Long.toString(value.longValue());
    }

    // special case: show number as bool
    if (BOOL_FORMATTER.getName().equals(format))
      return BOOL_FORMATTER.format(parameter, value, format, context, data);

    return getFormatter(format, data, context.getLocale()).format(value);
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
