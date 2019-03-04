package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterFormatter;


/**
 * @author Jeroen Gremmen
 */
public class StringFormatter implements ParameterFormatter
{
  @Override
  public String getName() {
    return "string";
  }


  @Override
  public String format(String parameter, Context context, ParameterData data)
  {
    final Object text = context.getParameterValue(parameter);
    return (text == null) ? null : String.valueOf(text).trim();
  }


  @Override
  public Class<?>[] getAutodetectTypes() {
    return new Class<?>[] { String.class, CharSequence.class, Object.class };
  }
}
