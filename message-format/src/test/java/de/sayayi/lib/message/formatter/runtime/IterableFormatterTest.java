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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;


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
    val context = new MessageContext(createFormatterService(new IterableFormatter()), NO_CACHE_INSTANCE, "de-DE");

    assertEquals(noSpaceText("Test, true, -6"), format(context, asList("Test", true, null, -6)));
  }


  @Test
  public void testEmptyOrCollection()
  {
    val context = new MessageContext(createFormatterService(new IterableFormatter()), NO_CACHE_INSTANCE);
    val message = context.getMessageFactory().parse("%{c,null:'null',empty:'empty'}");

    assertEquals("null", message.format(context, context.parameters().with("c", null)));
    assertEquals("empty", message.format(context, context.parameters().with("c", emptySet())));
  }


  @Test
  public void testSeparator()
  {
    val context = new MessageContext(createFormatterService(new IterableFormatter()), NO_CACHE_INSTANCE);

    assertEquals("1, 2, 3, 4 and 5", context.getMessageFactory()
        .parse("%{c,list-sep:', ',list-sep-last:' and '}")
        .format(context, context.parameters().with("c", asList(1, 2, 3, 4, 5))));

    assertEquals("1.2.3.4.5", context.getMessageFactory()
        .parse("%{c,list-sep:'.'}")
        .format(context, context.parameters().with("c", asList(1, 2, 3, 4, 5))));
  }
}