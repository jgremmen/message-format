package de.sayayi.lib.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

import de.sayayi.lib.message.Message.Context;


/**
 * @author Jeroen Gremmen
 */
public class MessageContextTest
{
  @Test
  public void testContext() throws ParseException
  {
    final Context ctx = MessageContext.builder()
        .withLocale(Locale.CHINA)
        .withParameter("name", "message")
        .withParameter("count", 3)
        .withParameter("today", new Date())
        .withParameter("flag", true)
        .buildContext();

    assertNotNull(ctx);
    assertEquals(Locale.CHINA, ctx.getLocale());
    assertEquals("message", ctx.getParameterValue("name"));
    assertEquals(Integer.valueOf(3), ctx.getParameterValue("count"));
    assertTrue(ctx.getParameterValue("today") instanceof Date);
    assertEquals(Boolean.TRUE, ctx.getParameterValue("flag"));
  }
}
