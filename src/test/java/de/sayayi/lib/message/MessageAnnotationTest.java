package de.sayayi.lib.message;

import de.sayayi.lib.message.Message.LocaleAware;
import de.sayayi.lib.message.Message.WithCode;
import de.sayayi.lib.message.annotation.Message;
import de.sayayi.lib.message.annotation.Messages;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.impl.EmptyMessageWithCode;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class MessageAnnotationTest
{
  private MessageBundle bundle;


  @Before
  public void initialize() {
    bundle = new MessageBundle(MessageAnnotationTest.class);
  }


  @Test
  @Messages({
      @Message(code = "T4", texts = @Text(locale = "en", text = "Message %{p1}")),
      @Message(code = "T5", texts = {
          @Text(locale = "en", text = "English message"),
          @Text(locale = "de", text = "Deutsche Nachricht")
      })
  })
  public void testMultiMessageAnotation()
  {
    WithCode msg = bundle.getByCode("T4");

    assertEquals("T4", msg.getCode());
    assertTrue(msg.hasParameters());
    assertTrue(msg instanceof LocaleAware);

    msg = bundle.getByCode("T5");

    assertEquals("T5", msg.getCode());
    assertFalse(msg.hasParameters());
    assertTrue(msg instanceof LocaleAware);
  }


  @Test
  @Message(code="MSG-052", texts={})
  public void testEmptyMessageWithCode()
  {
    final WithCode msg = bundle.getByCode("MSG-052");

    assertTrue(msg instanceof EmptyMessageWithCode);
    assertEquals("MSG-052", msg.getCode());
  }


  @Test
  @Message(code="T3", texts=@Text("m3"))
  public void testMessageWithoutLocale()
  {
    final WithCode msg = bundle.getByCode("T3");
    ParameterFactory factory = ParameterFactory.DEFAULT;

    assertEquals("m3", msg.format(factory));
    assertEquals("m3", msg.format(factory.parameters().withLocale(Locale.ROOT)));
    assertEquals("m3", msg.format(factory.parameters().withLocale(Locale.US)));
    assertEquals("m3", msg.format(factory.parameters().withLocale("xx-YY")));
    assertFalse(msg instanceof LocaleAware);
  }


  @Test
  @Message(code="T2", texts=@Text(locale="nl-NL", text="nl"))
  public void testSingleMessageWithLocale()
  {
    final WithCode msg = bundle.getByCode("T2");
    ParameterFactory factory = ParameterFactory.DEFAULT;

    assertEquals("nl", msg.format(factory));
    assertEquals("nl", msg.format(factory.parameters().withLocale(Locale.ROOT)));
    assertEquals("nl", msg.format(factory.parameters().withLocale(Locale.US)));
    assertEquals("nl", msg.format(factory.parameters().withLocale("xx-YY")));
  }


  @Test
  @Message(code = "T1", texts={
      @Text(locale="en-US", text="us"),
      @Text(locale="nl", text="nl"),
      @Text(locale="en-GB", text="uk"),
      @Text(locale="de-DE", text="de")
  })
  public void testLocaleSelection()
  {
    final WithCode msg = bundle.getByCode("T1");
    ParameterFactory factory = ParameterFactory.DEFAULT;

    assertEquals("us", msg.format(factory.parameters().withLocale(Locale.ROOT)));
    assertEquals("uk", msg.format(factory.parameters().withLocale(Locale.UK)));
    assertEquals("nl", msg.format(factory.parameters().withLocale("nl-BE")));
    assertEquals("us", msg.format(factory.parameters().withLocale(Locale.CHINESE)));
    assertEquals("de", msg.format(factory.parameters().withLocale("de-AT")));
  }
}
