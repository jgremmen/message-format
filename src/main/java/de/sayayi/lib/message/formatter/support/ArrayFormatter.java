package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public class ArrayFormatter implements ParameterFormatter
{
  @Override
  public String format(String parameter, Object array, String format, Context context, ParameterData data)
  {
    if (array == null)
      return null;

    final StringBuilder s = new StringBuilder();
    final Class<?> baseType = array.getClass().getComponentType();
    final ParameterFormatter formatter = baseType.isPrimitive() ? context.getFormatter(format, baseType) : null;
    final ResourceBundle bundle = getBundle("Formatter", context.getLocale());

    for(int i = 0, length = getLength(array); i < length; i++)
    {
      final Object value = get(array, i);

      if (i > 0)
        s.append(", ");

      if (formatter != null)
        s.append(formatter.format(null, value, format, context, data));
      else if (value == array)
        s.append(bundle.getString("thisArray"));
      else if (value != null)
        s.append(context.getFormatter(format, baseType).format(null, value, format, context, data));
    }

    return s.toString();
  }


  @Override
  public Set<Class<?>> getFormattableTypes()
  {
    return new HashSet<Class<?>>(Arrays.<Class<?>>asList(
        Object[].class,
        char[].class,
        short[].class,
        int[].class,
        long[].class,
        float[].class,
        double[].class,
        boolean[].class));
  }
}
