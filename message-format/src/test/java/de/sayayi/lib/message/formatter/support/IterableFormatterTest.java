/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.internal.part.TextPart;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class IterableFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertFormatterForType(new IterableFormatter(), Iterable.class);
  }


  @Test
  public void testObjectArray()
  {
    final GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new IterableFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, "de-DE");
    final Parameters noParameters = context.noParameters();

    assertEquals(new TextPart("Test, true, -6"), registry.getFormatter(null, List.class)
        .format(context, Arrays.asList("Test", true, null, -6), null , noParameters, null));
  }


  @Test
  public void testEmptyOrCollection()
  {
    final GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new IterableFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE);
    final Message message = context.getMessageFactory().parse("%{c,{null:'null',empty:'empty'}}");

    assertEquals("null", message.format(context, context.parameters().with("c", null)));
    assertEquals("empty", message.format(context, context.parameters().with("c", emptySet())));
  }
}
