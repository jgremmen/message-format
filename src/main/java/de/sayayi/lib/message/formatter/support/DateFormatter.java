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

import static java.text.DateFormat.FULL;
import static java.text.DateFormat.LONG;
import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.SHORT;
import static java.text.DateFormat.getDateInstance;


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
      formatter = getDateInstance(MEDIUM, locale);

    return formatter.format(date);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Date.class);
  }


  protected DateFormat getFormatter(String format, Locale locale)
  {
    if ("full".equals(format))
      return getDateInstance(FULL, locale);

    if ("long".equals(format))
      return getDateInstance(LONG, locale);

    if (format == null || format.isEmpty() || "medium".equals(format))
      return getDateInstance(MEDIUM, locale);

    if ("short".equals(format))
      return getDateInstance(SHORT, locale);

    return new SimpleDateFormat(format, locale);
  }
}
