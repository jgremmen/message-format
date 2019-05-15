package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;


/**
 * @author Jeroen Gremmen
 */
public class SupplierFormatter implements ParameterFormatter
{
  @Override
  public String format(Object value, String format, Parameters parameters, ParameterData data)
  {
    Supplier<?> supplier = (Supplier<?>)value;
    if (supplier == null)
      return null;

    value = supplier.get();
    if (value == null)
      return null;

    return parameters.getFormatter(format, value.getClass()).format(value, format, parameters, data);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(Supplier.class);
  }
}
