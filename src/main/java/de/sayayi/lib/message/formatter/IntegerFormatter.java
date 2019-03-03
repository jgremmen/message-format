package de.sayayi.lib.message.formatter;

import java.math.BigInteger;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterFormatter;


/**
 * @author Jeroen Gremmen
 */
public class IntegerFormatter implements ParameterFormatter
{
  @Override
  public String getName() {
    return "integer";
  }


  @Override
  public String format(String parameter, Context context, ParameterData data)
  {
    final Number value = (Number)context.getParameterValue(parameter);

    if (value instanceof BigInteger)
      return value.toString();

    if (value instanceof Number)
      return Long.toString(value.longValue());

    return null;
  }
}
