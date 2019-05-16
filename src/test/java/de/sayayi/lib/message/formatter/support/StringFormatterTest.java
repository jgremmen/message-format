package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import org.junit.Test;

import java.lang.annotation.RetentionPolicy;

import static de.sayayi.lib.message.MessageFactory.parse;
import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class StringFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    assertFormatterForType(new StringFormatter(), CharSequence.class);
    assertFormatterForType(new StringFormatter(), char[].class);
  }


  @Test
  public void testFormat()
  {
    final StringFormatter formatter = new StringFormatter();
    final ParameterFactory factory = ParameterFactory.DEFAULT;

    assertEquals("text", formatter.format(" text ", null, factory, null));
    assertEquals("RUNTIME", formatter.format(RetentionPolicy.RUNTIME, null, factory, null));
    assertEquals("hello", formatter.format(new Object() {
      @Override
      public String toString() {
        return " hello";
      }
    }, null, factory, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new StringFormatter());
    final ParameterFactory factory = ParameterFactory.createFor(formatterRegistry);

    final Parameters parameters = factory.parameters()
        .with("a", " a test ")
        .with("b", null)
        .with("c", Integer.valueOf(1234));
    final Message msg = parse("This is %{a} %{b} %{c}");

    assertEquals("This is a test 1234", msg.format(parameters));
  }
}
