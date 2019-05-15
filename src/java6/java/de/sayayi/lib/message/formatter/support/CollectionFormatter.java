package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public class CollectionFormatter implements ParameterFormatter
{
  @Override
  public String format(Object collection, String format, Parameters parameters, ParameterData data)
  {
    if (collection == null)
      return null;

    final ResourceBundle bundle = getBundle("Formatter", parameters.getLocale());
    final StringBuilder s = new StringBuilder();

    for(Object value: (Collection)collection)
    {
      if (s.length() > 0)
        s.append(", ");

      if (value == collection)
        s.append(bundle.getString("thisCollection"));
      else if (value != null)
        s.append(parameters.getFormatter(format, value.getClass()).format(value, format, parameters, data));
    }

    return s.toString();
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Collection.class);
  }
}
