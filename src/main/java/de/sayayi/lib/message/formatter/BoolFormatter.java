package de.sayayi.lib.message.formatter;

import java.math.BigDecimal;
import java.math.BigInteger;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.parameter.ParameterData;
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
  public String format(String parameter, Context context, ParameterData data)
  {
    final Object obj = context.getParameterValue(parameter);
    Boolean value;

    if (obj instanceof Boolean)
      value = (Boolean)obj;
    else if (obj instanceof BigInteger)
      value = Boolean.valueOf(((BigInteger)obj).signum() != 0);
    else if (obj instanceof BigDecimal)
      value = Boolean.valueOf(((BigDecimal)obj).signum() != 0);
    else if (obj instanceof Number)
      value = Boolean.valueOf(((Number)obj).longValue() != 0);
    else
      value = Boolean.valueOf(String.valueOf(obj));

    // allow custom messages for true/false value?
    if (data instanceof ParameterMap)
    {
      final Message message = ((ParameterMap)data).getMessageForKey(value);
      if (message != null)
        return message.format(context);
    }

    return value.toString();
  }
}
