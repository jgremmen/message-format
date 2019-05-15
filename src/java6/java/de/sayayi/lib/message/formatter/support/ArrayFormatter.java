package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
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
  public String format(Object array, String format, Parameters parameters, ParameterData data)
  {
    final int length;

    if (array == null || (length = getLength(array)) == 0)
      return null;

    final StringBuilder s = new StringBuilder();
    final Class<?> arrayType = array.getClass();
    final ParameterFormatter formatter =
        arrayType.isPrimitive() ? parameters.getFormatter(format, arrayType.getComponentType()) : null;
    final ResourceBundle bundle = getBundle("Formatter", parameters.getLocale());

    for(int i = 0; i < length; i++)
    {
      final Object value = get(array, i);

      if (s.length() > 0)
        s.append(", ");

      if (value == array)
        s.append(bundle.getString("thisArray"));
      else if (formatter != null)
        s.append(formatter.format(value, format, parameters, data));
      else if (value != null)
      {
        s.append(parameters.getFormatter(format, value.getClass())
            .format(value, format, parameters, data));
      }
    }

    return s.toString();
  }


  @Override
  public Set<Class<?>> getFormattableTypes()
  {
    return new HashSet<Class<?>>(Arrays.<Class<?>>asList(
        Object[].class,
        short[].class,
        int[].class,
        long[].class,
        float[].class,
        double[].class,
        boolean[].class));
  }
}
