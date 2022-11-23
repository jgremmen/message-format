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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Collections.singletonMap;
import static java.util.Locale.UK;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class MapFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertFormatterForType(new MapFormatter(), Map.class);
  }


  @Test
  public void testSeparator()
  {
    GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new MapFormatter());
    registry.addFormatter(new IterableFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, UK);

    Message message = context.getMessageFactory()
        .parse("%{map1} %{map2,map-kv-sep:'   -> '} %{map3,map-kv-sep:':  '}");

    assertEquals("key=value",
        message.format(context, context.parameters().with("map1", singletonMap("key", "value"))));

    assertEquals("key -> value",
        message.format(context, context.parameters().with("map2", singletonMap("key", "value"))));

    assertEquals("key: value",
        message.format(context, context.parameters().with("map3", singletonMap("key", "value"))));
  }


  @Test
  public void testNullKeyValue()
  {
    GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new MapFormatter());
    registry.addFormatter(new IterableFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, UK);

    Message message = context.getMessageFactory()
        .parse("%{map1} %{map2,map-null-key:'key',map-null-value:'value'}");

    assertEquals("(null)=(null)",
        message.format(context, context.parameters().with("map1", singletonMap(null, null))));

    assertEquals("key=value",
        message.format(context, context.parameters().with("map2", singletonMap(null, null))));
  }
}