package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collections;
import java.util.Set;
import java.util.function.IntSupplier;


/**
 * @author Jeroen Gremmen
 */
public class IntSupplierFormatter implements ParameterFormatter
{
  @Override
  public String format(Object value, String format, Parameters parameters, ParameterData data)
  {
    IntSupplier supplier = (IntSupplier)value;
    if (supplier == null)
      return null;

    return parameters.getFormatter(format, int.class).format(supplier.getAsInt(), format, parameters, data);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(IntSupplier.class);
  }
}
