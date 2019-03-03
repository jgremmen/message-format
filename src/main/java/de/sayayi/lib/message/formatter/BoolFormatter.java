package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterData.Type;
import de.sayayi.lib.message.parameter.ParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterMap;


/**
 * @author Jeroen Gremmen
 */
public class BoolFormatter implements ParameterFormatter
{
  @Override
  public String getName() {
    return "bool";
  }


  @Override
  public String format(String parameter, MessageContext context, ParameterData data)
  {
    final Object obj = context.getParameterValue(parameter);
    Boolean value;

    if (obj instanceof Boolean)
      value = (Boolean)obj;
    else if (obj instanceof Number)
      value = Boolean.valueOf(((Number)obj).longValue() != 0);
    else
      value = Boolean.valueOf(String.valueOf(obj));

    if (data.getType() == Type.MAP)
    {
      final ParameterMap map = (ParameterMap)data;
      final Message message = map.getMessageForKey(value);

      if (message != null)
        return message.format(context);
    }

    return value.toString();
  }
}
