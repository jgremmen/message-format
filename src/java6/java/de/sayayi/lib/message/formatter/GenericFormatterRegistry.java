package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.support.StringFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Jeroen Gremmen
 */
public class GenericFormatterRegistry implements FormatterRegistry
{
  private final Map<String,NamedParameterFormatter> namedFormatters = new HashMap<String,NamedParameterFormatter>();
  private final Map<Class<?>,ParameterFormatter> typeFormatters = new HashMap<Class<?>,ParameterFormatter>();
  private final Map<Class<?>,ParameterFormatter> cachedFormatters = new ConcurrentHashMap<Class<?>,ParameterFormatter>(32);


  public GenericFormatterRegistry() {
    addFormatter(new StringFormatter());
  }


  @Override
  public void addFormatterForType(Class<?> type, ParameterFormatter formatter)
  {
    typeFormatters.put(type, formatter);

    if (!cachedFormatters.isEmpty())
    {
      for(Class<?> interfaceClass: type.getInterfaces())
        cachedFormatters.remove(interfaceClass);

      while(type != null)
      {
        cachedFormatters.remove(type);
        type = type.getSuperclass();
      }
    }
  }


  @Override
  public void addFormatter(ParameterFormatter formatter)
  {
    if (formatter instanceof NamedParameterFormatter)
    {
      final String format = ((NamedParameterFormatter)formatter).getName();
      if (format == null || format.isEmpty())
        throw new IllegalArgumentException("formatter name must not be empty");

      namedFormatters.put(format, (NamedParameterFormatter)formatter);
    }

    for(final Class<?> type: formatter.getFormattableTypes())
      addFormatterForType(type, formatter);
  }


  @Override
  public ParameterFormatter getFormatter(String format, Class<?> type)
  {
    ParameterFormatter formatter = namedFormatters.get(format);

    if (formatter == null && (formatter = cachedFormatters.get(type)) == null)
    {
      for(Class<?> walkType = type; formatter == null && walkType != null; walkType = walkType.getSuperclass())
        formatter = getFormatterForType(walkType);

      cachedFormatters.put(type, formatter);
    }

    return formatter;
  }


  private ParameterFormatter getFormatterForType(Class<?> type)
  {
    ParameterFormatter formatter = typeFormatters.get(type);

    if (formatter == null)
      for(final Class<?> interfaceType: type.getInterfaces())
        if ((formatter = typeFormatters.get(interfaceType)) != null)
          break;

    return formatter;
  }
}
