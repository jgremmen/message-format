package de.sayayi.lib.message.formatter.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;


/**
 * @author Jeroen Gremmen
 */
public class BoolFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertTrue(new BoolFormatter().getFormattableTypes().contains(boolean.class));
  }


  @Test
  public void testFormat()
  {
    final BoolFormatter formatter = new BoolFormatter();
    final Context context = MessageContext.builder().buildContext();

    assertEquals("true", formatter.format("a", Boolean.TRUE, null, context, null));
    assertEquals("false", formatter.format("b", new Double(0.0), null, context, null));
    assertEquals("true", formatter.format("c", new Float(-0.0001), null, context, null));
    assertEquals("false", formatter.format("d", "FALSE", null, context, null));
    assertEquals("true", formatter.format("e", "TrUe", null, context, null));
    assertEquals("true", formatter.format("f", Integer.valueOf(-4), null, context, null));
  }


  @Test
  public void testFormatter() throws Exception
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new BoolFormatter());

    final Context context = MessageContext.builder()
        .withFormatterService(formatterRegistry)
        .withParameter("a", Boolean.FALSE)
        .withParameter("b", Boolean.TRUE)
        .withParameter("c", Integer.valueOf(1234))
        .withParameter("d", Integer.valueOf(0))
        .withParameter("e", Double.valueOf(3.14d))
        .buildContext();
    final Message msg = MessageFactory.parse("%{a} %{b} %{c} %{c,bool} %{d,bool,{true->'yes',false->'no'}} %{e}");

    assertEquals("false true 1234 true no 3.14", msg.format(context));
  }
}
