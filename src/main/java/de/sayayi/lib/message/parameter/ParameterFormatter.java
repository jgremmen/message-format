package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.MessageContext;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterFormatter
{
  String getName();

  String format(String parameter, MessageContext context, ParameterData data);
}
