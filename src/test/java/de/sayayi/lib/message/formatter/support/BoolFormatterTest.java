package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.MessageContext;
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
    final Context context = MessageContext.builder().withLocale("de", "DE").buildContext();

    assertEquals("wahr", formatter.format("a", Boolean.TRUE, null, context, null));
    assertEquals("falsch", formatter.format("b", 0.0d, null, context, null));
    assertEquals("wahr", formatter.format("c", -0.0001f, null, context, null));
    assertEquals("falsch", formatter.format("d", "FALSE", null, context, null));
    assertEquals("wahr", formatter.format("e", "TrUe", null, context, null));
    assertEquals("wahr", formatter.format("f", -4, null, context, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new BoolFormatter());

    final Context context = MessageContext.builder()
        .withFormatterService(formatterRegistry)
        .withLocale(ENGLISH)
        .withParameter("a", Boolean.FALSE)
        .withParameter("b", Boolean.TRUE)
        .withParameter("c", Integer.valueOf(1234))
        .withParameter("d", Integer.valueOf(0))
        .withParameter("e", Double.valueOf(3.14d))
        .buildContext();
    final Message msg = parse("%{a} %{b} %{c} %{c,bool} %{d,bool,{true->'yes',false->'no'}} %{e}");

    assertEquals("false true 1234 true no 3.14", msg.format(context));
  }
}
