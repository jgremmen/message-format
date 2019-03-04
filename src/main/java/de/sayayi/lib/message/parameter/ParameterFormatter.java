package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message.Context;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterFormatter
{
  /**
   * Tells the name of this parameter formatter
   *
   * @return  parameter formatter name, never {@code null}
   */
  String getName();


  String format(String parameter, Context context, ParameterData data);
}
