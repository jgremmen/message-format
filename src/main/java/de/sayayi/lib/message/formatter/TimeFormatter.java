package de.sayayi.lib.message.formatter;

import java.sql.Time;
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
public class TimeFormatter implements ParameterFormatter
{
  @Override
  public String getName() {
    return "time";
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
      formatter = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);

    return formatter.format(date);
  }


  protected DateFormat getFormatter(String format, Locale locale)
  {
    if ("FULL".equalsIgnoreCase(format))
      return DateFormat.getTimeInstance(DateFormat.FULL, locale);

    if ("LONG".equalsIgnoreCase(format))
      return DateFormat.getTimeInstance(DateFormat.LONG, locale);

    if (format.isEmpty() || "MEDIUM".equalsIgnoreCase(format))
      return DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);

    if ("SHORT".equalsIgnoreCase(format))
      return DateFormat.getTimeInstance(DateFormat.SHORT, locale);

    return new SimpleDateFormat(format, locale);
  }


  @Override
  public Class<?>[] getAutodetectTypes() {
    return new Class<?>[] { Time.class };
  }
}
