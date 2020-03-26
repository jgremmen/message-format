package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.data.ParameterString;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import org.junit.Test;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class SupplierFormatterTest
{
  @Test
  public void testBooleanSupplier()
  {
    GenericFormatterRegistry registry = new GenericFormatterRegistry();
    registry.addFormatter(new BoolFormatter());
    registry.addFormatter(new BooleanSupplierFormatter());

    Parameters noParameters = ParameterFactory.createFor("de-DE", registry).noParameters();

    Object value = (BooleanSupplier) () -> true;

    assertEquals("wahr", registry.getFormatter(null, value.getClass())
        .format(value, null, noParameters, null));
  }


  @Test
  public void testLongSupplier()
  {
    GenericFormatterRegistry registry = new GenericFormatterRegistry();
    registry.addFormatter(new NumberFormatter());
    registry.addFormatter(new LongSupplierFormatter());

    Parameters noParameters = ParameterFactory.createFor("en", registry).noParameters();

    Object value = (LongSupplier) () -> 1234567890L;

    assertEquals("1,234,567,890", registry.getFormatter(null, value.getClass())
        .format(value, null, noParameters, new ParameterString("###,###,###,###")));
  }
}
