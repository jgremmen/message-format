package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message.Context;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterFormatter
{
  String getName();

  String format(String parameter, Context context, ParameterData data);
}
