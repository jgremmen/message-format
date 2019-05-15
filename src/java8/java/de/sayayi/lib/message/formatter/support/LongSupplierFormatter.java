package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collections;
import java.util.Set;
import java.util.function.LongSupplier;


/**
 * @author Jeroen Gremmen
 */
public class LongSupplierFormatter implements ParameterFormatter
{
  @Override
  public String format(Object value, String format, Parameters parameters, ParameterData data)
  {
    LongSupplier supplier = (LongSupplier)value;
    if (supplier == null)
      return null;

    return parameters.getFormatter(format, long.class).format(supplier.getAsLong(), format, parameters, data);
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(LongSupplier.class);
  }
}
