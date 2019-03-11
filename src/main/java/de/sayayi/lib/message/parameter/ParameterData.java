package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message.Context;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterData
{
  String format(Context context, Object key);

  String format(Context context);

  Object asObject();
}
