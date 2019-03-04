package de.sayayi.lib.message.formatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterString;


/**
 * @author Jeroen Gremmen
 */
public class DateFormatter implements ParameterFormatter
{
  @Override
  public String getName() {
    return "date";
  }


  @Override
  public String format(String parameter, Context context, ParameterData data)
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
  public Class<?>[] getAutodetectTypes() {
    return new Class<?>[] { Date.class };
  }


  protected DateFormat getFormatter(String format, Locale locale)
  {
    if ("FULL".equalsIgnoreCase(format))
      return DateFormat.getDateInstance(DateFormat.FULL, locale);

    if ("LONG".equalsIgnoreCase(format))
      return DateFormat.getDateInstance(DateFormat.LONG, locale);

    if (format.isEmpty() || "MEDIUM".equalsIgnoreCase(format))
      return DateFormat.getDateInstance(DateFormat.MEDIUM, locale);

    if ("SHORT".equalsIgnoreCase(format))
      return DateFormat.getDateInstance(DateFormat.SHORT, locale);

    return new SimpleDateFormat(format, locale);
  }
}
