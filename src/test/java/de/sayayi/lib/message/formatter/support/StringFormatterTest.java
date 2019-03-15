package de.sayayi.lib.message.formatter.support;

import static de.sayayi.lib.message.MessageFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;


/**
 * @author Jeroen Gremmen
 */
public class StringFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertTrue(new StringFormatter().getFormattableTypes().contains(Object.class));
  }


  @Test
  public void testFormat()
  {
    final StringFormatter formatter = new StringFormatter();
    final Context context = MessageContext.builder().buildContext();

    assertEquals("text", formatter.format("a", " text ", null, context, null));
    assertEquals("RUNTIME", formatter.format("b", RetentionPolicy.RUNTIME, null, context, null));
    assertEquals("hello", formatter.format("c", new Object() {
      @Override
      public String toString() {
        return " hello";
      }
    }, null, context, null));
  }


  @Test
  public void testFormatter() throws Exception
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new StringFormatter());

    final Context context = MessageContext.builder()
        .withFormatterService(formatterRegistry)
        .withParameter("a", " a test ")
        .withParameter("b", null)
        .withParameter("c", Integer.valueOf(1234))
        .buildContext();
    final Message msg = parse("This is %{a} %{b} %{c}");

    assertEquals("This is a test 1234", msg.format(context));
  }
}
