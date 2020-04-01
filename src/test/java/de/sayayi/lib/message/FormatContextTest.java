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

    final Parameters ctx = parameterFactory
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
