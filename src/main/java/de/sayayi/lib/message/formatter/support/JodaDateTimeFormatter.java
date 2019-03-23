package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.data.ParameterString;
import de.sayayi.lib.message.formatter.ParameterFormatter;
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
  public String format(String parameter, Object value, String format, Context context, ParameterData data)
  {
    final Locale locale = context.getLocale();

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


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return new HashSet<Class<?>>(Arrays.<Class<?>>asList(BaseLocal.class, ReadableDateTime.class));
  }
}
