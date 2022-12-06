/*
 * Copyright 2022 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
class AtomicBooleanFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertFormatterForType(new AtomicBooleanFormatter(), AtomicBoolean.class);
  }


  @Test
  public void testFormat()
  {
    final AtomicBooleanFormatter formatter = new AtomicBooleanFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(),
        NO_CACHE_INSTANCE, "de-DE");
    final MessageContext.Parameters parameters = context.noParameters();

    assertEquals(nullText(), formatter.format(context, null, parameters, null));
    assertEquals(noSpaceText("wahr"),
        formatter.format(context, new AtomicBoolean(true), parameters, null));
    assertEquals(noSpaceText("falsch"),
        formatter.format(context, new AtomicBoolean(false), parameters, null));
  }
}