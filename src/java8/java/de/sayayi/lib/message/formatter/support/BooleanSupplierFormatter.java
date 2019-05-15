package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collections;
import java.util.Set;
import java.util.function.BooleanSupplier;


/**
 * @author Jeroen Gremmen
 */
public class BooleanSupplierFormatter implements ParameterFormatter
{
  @Override
  public String format(Object value, String format, Parameters parameters, ParameterData data)
  {
    BooleanSupplier supplier = (BooleanSupplier)value;
    if (supplier == null)
      return null;

    return parameters.getFormatter(format, boolean.class).format(supplier.getAsBoolean(), format, parameters, data);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(BooleanSupplier.class);
  }
}
