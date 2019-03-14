package de.sayayi.lib.message.formatter;

import java.util.Set;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.parameter.ParameterData;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterFormatter
{
  String format(String parameter, Object value, String format, Context context, ParameterData data);

  Set<Class<?>> getFormattableTypes();
}
