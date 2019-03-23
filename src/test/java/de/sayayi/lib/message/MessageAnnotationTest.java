package de.sayayi.lib.message;

import de.sayayi.lib.message.annotation.Message;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.impl.EmptyMessageWithCode;
import org.junit.Test;

import java.text.ParseException;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class MessageAnnotationTest
{
  @Message(texts={})
  private static final char FIELD = 0;


  @Test
  public void testEmptyMessageNoCode() throws Exception
  {
    final MessageWithCode msg = MessageFactory.parseAnnotation(
        MessageAnnotationTest.class.getDeclaredField("FIELD"));

    assertTrue(msg instanceof EmptyMessageWithCode);
    assertNull(msg.getCode());
  }


  @Test
  @Message(code="MSG-052", texts={})
  public void testEmptyMessageWithCode() throws Exception
  {
    final MessageWithCode msg = MessageFactory.parseAnnotation(
        MessageAnnotationTest.class.getDeclaredMethod("testEmptyMessageWithCode"));

    assertTrue(msg instanceof EmptyMessageWithCode);
    assertEquals("MSG-052", msg.getCode());
  }


  @Test
  @Message(texts=@Text(text="m3"))
  public void testMessageWithoutLocale() throws Exception
  {
    final MessageWithCode msg = MessageFactory.parseAnnotation(
        MessageAnnotationTest.class.getDeclaredMethod("testMessageWithoutLocale"));

    assertEquals("m3", msg.format(MessageContext.builder().buildContext()));
    assertEquals("m3", msg.format(MessageContext.builder().withLocale(Locale.ROOT).buildContext()));
    assertEquals("m3", msg.format(MessageContext.builder().withLocale(Locale.US).buildContext()));
    assertEquals("m3", msg.format(MessageContext.builder().withLocale(new Locale("xx", "YY")).buildContext()));
  }


  @Test(expected=ParseException.class)
  @Message(texts=@Text(locale="123", text=""))
  public void testInvalidLocale() throws Exception {
    MessageFactory.parseAnnotation(MessageAnnotationTest.class.getDeclaredMethod("testInvalidLocale"));
  }


  @Test
  @Message(texts=@Text(locale="nl-NL", text="nl"))
  public void testSingleMessageWithLocale() throws Exception
  {
    final MessageWithCode msg = MessageFactory.parseAnnotation(
        MessageAnnotationTest.class.getDeclaredMethod("testSingleMessageWithLocale"));

    assertEquals("nl", msg.format(MessageContext.builder().buildContext()));
    assertEquals("nl", msg.format(MessageContext.builder().withLocale(Locale.ROOT).buildContext()));
    assertEquals("nl", msg.format(MessageContext.builder().withLocale(Locale.US).buildContext()));
    assertEquals("nl", msg.format(MessageContext.builder().withLocale(new Locale("xx", "YY")).buildContext()));
  }


  @Test
  @Message(texts={
      @Text(locale="en-US", text="us"),
      @Text(locale="nl", text="nl"),
      @Text(locale="en-GB", text="uk"),
      @Text(locale="de-DE", text="de")
  })
  public void testLocaleSelection() throws Exception
  {
    final MessageWithCode msg = MessageFactory.parseAnnotation(
        MessageAnnotationTest.class.getDeclaredMethod("testLocaleSelection"));

    assertEquals("us", msg.format(MessageContext.builder().withLocale(Locale.ROOT).buildContext()));
    assertEquals("uk", msg.format(MessageContext.builder().withLocale(Locale.UK).buildContext()));
    assertEquals("nl", msg.format(MessageContext.builder().withLocale(new Locale("nl", "BE")).buildContext()));
    assertEquals("us", msg.format(MessageContext.builder().withLocale(Locale.CHINESE).buildContext()));
    assertEquals("de", msg.format(MessageContext.builder().withLocale(new Locale("de", "AT")).buildContext()));
  }
}
