package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public final class StringFormatter implements NamedParameterFormatter
{
  @Override
  public String getName() {
    return "string";
  }


  @Override
  public String format(Object value, String format, Parameters parameters, ParameterData data)
  {
    if (value == null)
      return null;

    if (value instanceof char[])
      value = new String((char[])value);

    return String.valueOf(value).trim();
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return new HashSet<Class<?>>(Arrays.asList(CharSequence.class, char[].class));
  }
}
