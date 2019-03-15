package de.sayayi.lib.message.formatter.support;

import java.util.Collections;
import java.util.Set;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterMap;


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
  public String format(String parameter, Object value, String format, Context context, ParameterData data)
  {
    if (!(data instanceof ParameterMap))
      throw new IllegalArgumentException("missing choice map for parameter " + parameter);

    return data.format(context, value);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.emptySet();
  }
}
