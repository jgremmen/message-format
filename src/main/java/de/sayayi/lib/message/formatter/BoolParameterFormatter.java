package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterFormatter;


/**
 * @author Jeroen Gremmen
 */
public class BoolParameterFormatter implements ParameterFormatter
{
  @Override
  public String getName() {
    return "bool";
  }


  @Override
  public String format(String parameter, MessageContext context, ParameterData data)
  {
    final Object value = context.getParameterValue(parameter);

    if (value instanceof Boolean)
      return value.toString();

    if (value instanceof Number)
      return Boolean.toString( ((Number)value).longValue() != 0);

    return null;
  }
}
