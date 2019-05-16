package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.support.StringFormatter;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Jeroen Gremmen
 */
public class GenericFormatterRegistry implements FormatterRegistry
{
  private static final Comparator<Class<?>> CLASS_SORTER = new Comparator<Class<?>>() {
    @Override
    public int compare(Class<?> o1, Class<?> o2) {
      return (o1 == o2) ? 0 : o1.getName().compareTo(o2.getName());
    }
  };

  private final Map<String,NamedParameterFormatter> namedFormatters =
      new ConcurrentHashMap<String,NamedParameterFormatter>();
  private final Map<Class<?>,ParameterFormatter> typeFormatters =
      new ConcurrentHashMap<Class<?>,ParameterFormatter>();
  private final Map<Class<?>,ParameterFormatter> cachedFormatters =
      Collections.synchronizedMap(new FixedSizeCacheMap<Class<?>,ParameterFormatter>(CLASS_SORTER, 128));


  public GenericFormatterRegistry()
  {
    StringFormatter stringFormatter = new StringFormatter();

    addFormatter(stringFormatter);
    addFormatterForType(Object.class, stringFormatter);
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
    ParameterFormatter formatter = (format == null) ? null : namedFormatters.get(format);

    if (formatter == null && (formatter = cachedFormatters.get(type)) == null)
    {
      for(Class<?> t = type; formatter == null && t != null; t = t.getSuperclass())
        formatter = getFormatterForType(t);

      if (formatter == null)
        formatter = getFormatter(null, Object.class);

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
