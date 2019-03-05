package de.sayayi.lib.message.formatter.support;

import java.util.Collections;
import java.util.Set;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.parameter.ParameterData;


/**
 * @author Jeroen Gremmen
 */
public final class StringFormatter implements ParameterFormatter
{
  @Override
  public String format(String parameter, Object value, String format, Context context, ParameterData data) {
    return (value == null) ? null : String.valueOf(value).trim();
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Object.class);
  }
}
