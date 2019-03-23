package de.sayayi.lib.message;

import de.sayayi.lib.message.Message.Context;
import org.junit.Test;

import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class MessageContextTest
{
  @Test
  public void testContext()
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
    assertEquals(3, ctx.getParameterValue("count"));
    assertTrue(ctx.getParameterValue("today") instanceof Date);
    assertEquals(Boolean.TRUE, ctx.getParameterValue("flag"));
  }
}
