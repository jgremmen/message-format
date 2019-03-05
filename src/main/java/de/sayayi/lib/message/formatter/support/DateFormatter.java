package de.sayayi.lib.message.formatter.support;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterString;


/**
 * @author Jeroen Gremmen
 */
public class DateFormatter implements ParameterFormatter
{
  @Override
  public String format(String parameter, Object value, String format, Context context, ParameterData data)
  {
    final Date date = (Date)context.getParameterValue(parameter);
    if (date == null)
      return null;

    final Locale locale = context.getLocale();
    DateFormat formatter;

    if (data instanceof ParameterString)
      formatter = getFormatter(((ParameterString)data).getValue(), locale);
    else
      formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);

    return formatter.format(date);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Date.class);
  }


  protected DateFormat getFormatter(String format, Locale locale)
  {
    if ("full".equals(format))
      return DateFormat.getDateInstance(DateFormat.FULL, locale);

    if ("long".equals(format))
      return DateFormat.getDateInstance(DateFormat.LONG, locale);

    if (format.isEmpty() || "medium".equals(format))
      return DateFormat.getDateInstance(DateFormat.MEDIUM, locale);

    if ("short".equals(format))
      return DateFormat.getDateInstance(DateFormat.SHORT, locale);

    return new SimpleDateFormat(format, locale);
  }
}
