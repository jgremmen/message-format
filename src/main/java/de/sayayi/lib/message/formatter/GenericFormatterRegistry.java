package de.sayayi.lib.message.formatter;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Jeroen Gremmen
 */
public class GenericFormatterRegistry implements FormatterRegistry
{
  private final Map<String,NamedParameterFormatter> namedFormatters = new HashMap<String,NamedParameterFormatter>();
  private final Map<Class<?>,ParameterFormatter> typeFormatters = new HashMap<Class<?>,ParameterFormatter>();


  @Override
  public void addFormatterForType(Class<?> type, ParameterFormatter formatter) {
    typeFormatters.put(type, formatter);
  }


  @Override
  public void addFormatter(ParameterFormatter formatter)
  {
    if (formatter instanceof NamedParameterFormatter)
    {
      final String format = ((NamedParameterFormatter)formatter).getName();
      if (format == null || format.length() == 0)
        throw new IllegalArgumentException("formatter name must not be empty");

      namedFormatters.put(format, (NamedParameterFormatter)formatter);
    }

    for(final Class<?> type: formatter.getFormattableTypes())
      typeFormatters.put(type, formatter);
  }


  @Override
  public ParameterFormatter getFormatter(String format, Class<?> type)
  {
    ParameterFormatter formatter = namedFormatters.get(format);

    while(formatter == null && type != null)
      if ((formatter = typeFormatters.get(type)) == null)
        type = type.getSuperclass();

    return formatter;
  }
}
