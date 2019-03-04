package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterMap;


/**
 * @author Jeroen Gremmen
 */
public final class ChoiceFormatter implements ParameterFormatter
{
  @Override
  public String getName() {
    return "choice";
  }


  @Override
  public String format(String parameter, Context context, ParameterData data)
  {
    final Object key = context.getParameterValue(parameter);
    final ParameterMap map = (ParameterMap)data;

    return map.format(context, key);
  }


  @Override
  public Class<?>[] getAutodetectTypes() {
    return new Class<?>[0];
  }
}
