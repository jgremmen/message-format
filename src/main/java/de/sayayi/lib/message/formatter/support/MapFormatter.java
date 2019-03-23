package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public class MapFormatter implements ParameterFormatter
{
  @Override
  public String format(String parameter, Object map, String format, Context context, ParameterData data)
  {
    if (map == null)
      return null;

    return context.getFormatter(null, Set.class)
        .format(parameter, ((Map<?,?>)map).entrySet(), format, context, data);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Map.class);
  }
}
