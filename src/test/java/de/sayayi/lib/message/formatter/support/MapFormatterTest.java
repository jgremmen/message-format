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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import org.junit.Test;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;


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
    GenericFormatterRegistry registry = new GenericFormatterRegistry();
    registry.addFormatter(new MapFormatter());

    ParameterFactory factory = ParameterFactory.createFor(Locale.UK);

    Message message = MessageFactory.parse("%{map1} %{map2,{sep:'   -> '}} %{map3,':  '}");

    assertEquals("key=value",
        message.format(factory.with("map1", Collections.singletonMap("key", "value"))));

    assertEquals("key -> value",
        message.format(factory.with("map2", Collections.singletonMap("key", "value"))));

    assertEquals("key: value",
        message.format(factory.with("map3", Collections.singletonMap("key", "value"))));
  }


  @Test
  public void testNullKeyValue()
  {
    GenericFormatterRegistry registry = new GenericFormatterRegistry();
    registry.addFormatter(new MapFormatter());

    ParameterFactory factory = ParameterFactory.createFor(Locale.UK);

    Message message = MessageFactory.parse("%{map1} %{map2,{null-key:'key',null-value:'value'}}");

    assertEquals("(null)=(null)",
        message.format(factory.with("map1", Collections.singletonMap(null, null))));

    assertEquals("key=value",
        message.format(factory.with("map2", Collections.singletonMap(null, null))));
  }
}
