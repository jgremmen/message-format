package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collections;
import java.util.Set;
import java.util.function.DoubleSupplier;


/**
 * @author Jeroen Gremmen
 */
public class DoubleSupplierFormatter implements ParameterFormatter
{
  @Override
  public String format(Object value, String format, Parameters parameters, ParameterData data)
  {
    DoubleSupplier supplier = (DoubleSupplier)value;
    if (supplier == null)
      return null;

    return parameters.getFormatter(format, double.class).format(supplier.getAsDouble(), format, parameters, data);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(DoubleSupplier.class);
  }
}
