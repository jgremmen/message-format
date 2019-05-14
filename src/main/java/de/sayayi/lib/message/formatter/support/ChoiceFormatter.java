package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.data.ParameterMap;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public final class ChoiceFormatter implements NamedParameterFormatter
{
  @Override
  public String getName() {
    return "choice";
  }


  @Override
  public String format(String parameter, Object value, String format, Parameters parameters, ParameterData data)
  {
    if (!(data instanceof ParameterMap))
      throw new IllegalArgumentException("missing choice map for data " + parameter);

    if (value != null && !(value instanceof Serializable))
      throw new IllegalArgumentException("value must be serializable");

    return data.format(parameters, (Serializable)value);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.emptySet();
  }
}
