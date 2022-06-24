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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
class SizeFormatterTest
{
  @Test
  public void testFormat()
  {
    final GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new SizeFormatter());
    registry.addFormatter(new IterableFormatter());
    registry.addFormatter(new ArrayFormatter());
    registry.addFormatter(new MapFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, Locale.UK);
    final Message message = context.getMessageFactory()
        .parse("%{c,size} %{c,size,{0:'empty',1:'singleton','multiple'}}");

    assertEquals("0 empty", message.format(context,
        context.parameters().with("c", emptyList())));

    assertEquals("1 singleton", message.format(context,
        context.parameters().with("c", singletonMap("a", "b"))));

    assertEquals("4 multiple", message.format(context,
        context.parameters().with("c", new int[] { 4, -45, 8, 1 })));
  }


  @Test
  public void testFormatNoSizeQueryable()
  {
    final GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new SizeFormatter());
    registry.addFormatter(new BoolFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, Locale.UK);
    final Message message = context.getMessageFactory().parse("%{c,size}");

    assertEquals("0", message.format(context,
        context.parameters().with("c", true)));
  }


  @Test
  public void testFormatDefaultFormatter()
  {
    final GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new SizeFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, Locale.UK);
    final Message message = context.getMessageFactory().parse("%{c,size}");

    assertEquals("0", message.format(context,
        context.parameters().with("c", new byte[] { 'a', 'b' })));
  }
}