package de.sayayi.lib.message;

import de.sayayi.lib.message.Message.Parameters;
import org.junit.Test;

import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class FormatContextTest
{
  @Test
  public void testContext()
  {
    ParameterFactory parameterFactory = ParameterFactory.createFor(Locale.CHINA);

    final Parameters ctx = parameterFactory.parameters()
        .withLocale(Locale.CHINA)
        .with("name", "message")
        .with("count", 3)
        .with("today", new Date())
        .with("flag", true);

    assertNotNull(ctx);
    assertEquals(Locale.CHINA, ctx.getLocale());
    assertEquals("message", ctx.getParameterValue("name"));
    assertEquals(3, ctx.getParameterValue("count"));
    assertTrue(ctx.getParameterValue("today") instanceof Date);
    assertEquals(Boolean.TRUE, ctx.getParameterValue("flag"));
  }
}
