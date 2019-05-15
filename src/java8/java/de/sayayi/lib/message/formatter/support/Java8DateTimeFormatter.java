package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.data.ParameterString;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.time.format.FormatStyle.FULL;
import static java.time.format.FormatStyle.LONG;
import static java.time.format.FormatStyle.MEDIUM;
import static java.time.format.FormatStyle.SHORT;


/**
 * @author Jeroen Gremmen
 */
public class Java8DateTimeFormatter implements ParameterFormatter
{
  private static final Map<String,String> STYLE = new HashMap<>();
  private static final Map<String,DateTimeFormatter> FORMATTER = new HashMap<>();

  static
  {
    STYLE.put("short", "SS");
    STYLE.put("medium", "MM");
    STYLE.put("long", "LL");
    STYLE.put("full", "FF");
    STYLE.put("date", "M-");
    STYLE.put("time", "-M");

    FORMATTER.put("SS", DateTimeFormatter.ofLocalizedDateTime(SHORT, SHORT));
    FORMATTER.put("S-", DateTimeFormatter.ofLocalizedDate(SHORT));
    FORMATTER.put("-S", DateTimeFormatter.ofLocalizedTime(SHORT));

    FORMATTER.put("MM", DateTimeFormatter.ofLocalizedDateTime(MEDIUM, MEDIUM));
    FORMATTER.put("M-", DateTimeFormatter.ofLocalizedDate(MEDIUM));
    FORMATTER.put("-M", DateTimeFormatter.ofLocalizedTime(MEDIUM));

    FORMATTER.put("LL", DateTimeFormatter.ofLocalizedDateTime(LONG, LONG));
    FORMATTER.put("L-", DateTimeFormatter.ofLocalizedDate(LONG));
    FORMATTER.put("-L", DateTimeFormatter.ofLocalizedTime(LONG));

    FORMATTER.put("FF", DateTimeFormatter.ofLocalizedDateTime(FULL, FULL));
    FORMATTER.put("F-", DateTimeFormatter.ofLocalizedDate(FULL));
    FORMATTER.put("-F", DateTimeFormatter.ofLocalizedTime(FULL));

    FORMATTER.put("--", null);
  }


  @Override
  public String format(String parameter, Object value, String format, Parameters parameters, ParameterData data)
  {
    final DateTimeFormatter formatter = getFormatter((Temporal)value, format, data);

    return (formatter == null) ? null : formatter.withLocale(parameters.getLocale()).format((Temporal)value).trim();
  }


  private DateTimeFormatter getFormatter(Temporal datetime, String format, ParameterData data)
  {
    if (format == null && data instanceof ParameterString)
      return DateTimeFormatter.ofPattern(((ParameterString)data).getValue());

    final String styleStr = STYLE.get(format);
    final char[] style = (styleStr != null) ? styleStr.toCharArray() : "MM".toCharArray();

    if (datetime instanceof LocalDate)
      style[1] = '-';
    else if (datetime instanceof LocalTime || datetime instanceof OffsetTime)
      style[0] = '-';

    return FORMATTER.get(new String(style));
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(Temporal.class);
  }
}
