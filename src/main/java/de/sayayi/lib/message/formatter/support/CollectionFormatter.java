package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterData;

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
  public String format(String parameter, Object collection, String format, Context context, ParameterData data)
  {
    if (collection == null)
      return null;

    final ResourceBundle bundle = getBundle("Formatter", context.getLocale());
    final StringBuilder s = new StringBuilder();
    boolean first = true;

    for(Object value: (Collection)collection)
    {
      if (first)
        first = false;
      else
        s.append(", ");

      if (value == collection)
        s.append(bundle.getString("thisCollection"));
      else if (value != null)
        s.append(context.getFormatter(format, value.getClass()).format(null, value, format, context, data));
    }

    return s.toString();
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Collection.class);
  }
}
