package de.sayayi.lib.message.formatter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.sayayi.lib.message.formatter.support.StringFormatter;


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

    cachedFormatters.clear();
  }


  @Override
  public ParameterFormatter getFormatter(String format, Class<?> type)
  {
    ParameterFormatter formatter = namedFormatters.get(format);

    if (formatter == null)
    {
      boolean cacheResult = false;
      Class<?> walkType = type;

      formatter = cachedFormatters.get(type);

      while(formatter == null && walkType != null)
      {
        if ((formatter = getFormatterForType(walkType)) == null)
        {
          cacheResult = true;
          walkType = walkType.getSuperclass();
        }
        else if (cacheResult)
          cachedFormatters.put(type, formatter);
      }
    }

    return formatter;
  }


  protected ParameterFormatter getFormatterForType(Class<?> type)
  {
    ParameterFormatter formatter = typeFormatters.get(type);

    if (formatter == null)
    {
      for(final Class<?> interfaceType: type.getInterfaces())
        if ((formatter = typeFormatters.get(interfaceType)) != null)
          break;
    }

    return formatter;
  }
}
