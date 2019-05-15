package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.data.ParameterString;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public class MapFormatter implements ParameterFormatter
{
  @Override
  public String format(Object value, String format, Parameters parameters, ParameterData data)
  {
    final Map<?,?> map = (Map<?,?>)value;
    if (map == null || map.isEmpty())
      return null;

    final ResourceBundle bundle = getBundle("Formatter", parameters.getLocale());
    final String separator = (data instanceof ParameterString) ? ((ParameterString)data).getValue() : "=";
    final StringBuilder s = new StringBuilder();

    for(Entry<?,?> entry: map.entrySet())
    {
      if (s.length() > 0)
        s.append(", ");

      Object key = entry.getKey();
      if (key == value)
        s.append(bundle.getString("thisMap"));
      else if (key != null)
        s.append(parameters.getFormatter(null, key.getClass()).format(key, null, parameters, null));
      else
        s.append("(null)");

      s.append(separator);

      Object val = entry.getValue();
      if (val == value)
        s.append(bundle.getString("thisMap"));
      else if (val != null)
        s.append(parameters.getFormatter(null, val.getClass()).format(val, null, parameters, null));
      else
        s.append("(null)");
    }

    return s.toString();
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Map.class);
  }
}
