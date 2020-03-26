package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import org.junit.Test;

import static de.sayayi.lib.message.MessageFactory.parse;
import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class BoolFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    assertFormatterForType(new BoolFormatter(), boolean.class);
    assertFormatterForType(new BoolFormatter(), Boolean.class);
  }


  @Test
  public void testFormat()
  {
    final BoolFormatter formatter = new BoolFormatter();
    final Parameters parameters = ParameterFactory.createFor("de-DE").noParameters();

    assertEquals("wahr", formatter.format(Boolean.TRUE, null, parameters, null));
    assertEquals("falsch", formatter.format(0.0d, null, parameters, null));
    assertEquals("wahr", formatter.format(-0.0001f, null, parameters, null));
    assertEquals("falsch", formatter.format("FALSE", null, parameters, null));
    assertEquals("wahr", formatter.format("TrUe", null, parameters, null));
    assertEquals("wahr", formatter.format(-4, null, parameters, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new BoolFormatter());
    ParameterFactory factory = ParameterFactory.createFor(ENGLISH, formatterRegistry);

    final Parameters parameters = factory
        .with("a", Boolean.FALSE)
        .with("b", Boolean.TRUE)
        .with("c", Integer.valueOf(1234))
        .with("d", Integer.valueOf(0))
        .with("e", Double.valueOf(3.14d));
    final Message msg = parse("%{a} %{b} %{c} %{c,bool} %{d,bool,{true->'yes',false->'no'}} %{e}");

    assertEquals("false true 1234 true no 3.14", msg.format(parameters));
  }
}
