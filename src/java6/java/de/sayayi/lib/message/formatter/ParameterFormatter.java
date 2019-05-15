package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;

import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterFormatter
{
  String format(Object value, String format, Parameters parameters, ParameterData data);

  Set<Class<?>> getFormattableTypes();
}
