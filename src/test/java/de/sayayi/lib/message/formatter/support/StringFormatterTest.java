package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import org.junit.Test;

import java.lang.annotation.RetentionPolicy;

import static de.sayayi.lib.message.MessageFactory.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


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
    final Parameters noParameters = ParameterFactory.DEFAULT.noParameters();

    assertEquals("text", formatter.format(" text ", null, noParameters, null));
    assertEquals("RUNTIME", formatter.format(RetentionPolicy.RUNTIME, null, noParameters, null));
    assertEquals("hello", formatter.format(new Object() {
      @Override
      public String toString() {
        return " hello";
      }
    }, null, noParameters, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new StringFormatter());
    final ParameterFactory factory = ParameterFactory.createFor(formatterRegistry);

    final Parameters parameters = factory
        .with("a", " a test ")
        .with("b", null)
        .with("c", Integer.valueOf(1234));
    final Message msg = parse("This is %{a} %{b} %{c}");

    assertEquals("This is a test 1234", msg.format(parameters));
  }


  @Test
  public void testFormatterWithMap()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    final ParameterFactory factory = ParameterFactory.createFor(formatterRegistry);

    final Parameters parameters = factory
        .with("empty", "")
        .with("null", null)
        .with("spaces", "  ")
        .with("text", "hello  ");

    assertNull(parse("%{empty,{'!empty'->'nok'}}").format(parameters));
    assertEquals("ok", parse("%{empty,{'empty'->'ok'}}").format(parameters));
    assertEquals("ok", parse("%{null,{'empty'->'nok','null'->'ok'}}").format(parameters));
    assertEquals("ok", parse("%{null,{'empty'->'ok'}}").format(parameters));
    assertEquals("ok", parse("%{spaces,{'empty'->'ok'}}").format(parameters));
    assertEquals("ok", parse("%{spaces,{'!null'->'ok'}}").format(parameters));
    assertEquals("hello!", parse("%{text,{'!null'->'nok','!empty'->'%{text}!'}}").format(parameters));
    assertEquals("hello!", parse("%{text,{'!null'->'%{text}!'}}").format(parameters));
  }
}
