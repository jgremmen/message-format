/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message;

import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.lang.Boolean.TRUE;
import static java.util.Locale.CHINA;
import static java.util.Locale.UK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class FormatContextTest
{
  @Test
  public void testContext()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(),
        NO_CACHE_INSTANCE, UK);
    final Parameters ctx = context.parameters()
        .withLocale(CHINA)
        .with("name", "message")
        .with("count", 3)
        .with("today", new Date())
        .with("flag", true);

    assertNotNull(ctx);
    assertEquals(CHINA, ctx.getLocale());
    assertEquals("message", ctx.getParameterValue("name"));
    assertEquals(3, ctx.getParameterValue("count"));
    assertTrue(ctx.getParameterValue("today") instanceof Date);
    assertEquals(TRUE, ctx.getParameterValue("flag"));
  }
}
