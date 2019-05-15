package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public class OptionalFormatter implements ParameterFormatter
{
  @Override
  public String format(Object value, String format, Parameters parameters, ParameterData data)
  {
    Optional<?> optional = (Optional<?>)value;
    if (optional == null || !optional.isPresent())
      return null;

    value = optional.get();

    return parameters.getFormatter(format, value.getClass()).format(value, format, parameters, data);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(Optional.class);
  }
}
