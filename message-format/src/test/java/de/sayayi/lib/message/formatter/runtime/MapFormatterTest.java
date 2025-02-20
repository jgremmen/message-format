/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.named.BoolFormatter;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.LinkedHashMap;
import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Collections.singletonMap;
import static java.util.Locale.UK;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("Map formatter")
class MapFormatterTest extends AbstractFormatterTest
{
  @Test
  @DisplayName("Formattable types")
  void testFormattableTypes() {
    assertFormatterForType(new MapFormatter(), Map.class);
  }


  @Test
  @DisplayName("Key/value separator")
  void testSeparator()
  {
    val message = MessageSupportFactory
        .create(createFormatterService(new MapFormatter(), new ArrayFormatter()),
            NO_CACHE_INSTANCE)
        .setLocale(UK)
        .message("%{map1} %{map2,map-kv:'%{key}   -> %{value}'} %{map3,map-kv:' %{key}:  %{value} '}");

    assertEquals("key=value",
        message.with("map1", Map.of("key", "value")).format());
    assertEquals("key -> value",
        message.clear().with("map2", Map.of("key", "value")).format());
    assertEquals("key: value",
        message.clear().with("map3", Map.of("key", "value")).format());
  }


  @Test
  @DisplayName("Null key and value with/without configuration")
  void testNullKeyValue()
  {
    val message = MessageSupportFactory
        .create(createFormatterService(new MapFormatter(), new ArrayFormatter()),
            NO_CACHE_INSTANCE)
        .setLocale(UK)
        .message("%{map1} %{map2,map-kv:'%{key,null:key}=%{value,null:value}'}");

    assertEquals("(null)=(null)", message
        .with("map1", singletonMap(null, null)).format());
    assertEquals("key=value",
        message.clear().with("map2", singletonMap(null, null)).format());
  }


  @Test
  @DisplayName("Empty map")
  void testEmpty()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new MapFormatter(), new ArrayFormatter()), NO_CACHE_INSTANCE)
        .setLocale(UK);
    val parameters = Map.<String,Object>of("map", Map.of());

    assertEquals("", messageSupport.message("%{map}").with(parameters).format());
    assertEquals("empty", messageSupport.message("%{map,empty:empty}").with(parameters).format());
  }


  @Test
  void testMultiEntry()
  {
    val message = MessageSupportFactory
        .create(createFormatterService(
            new MapFormatter(), new ArrayFormatter(), new NumberFormatter()),
            NO_CACHE_INSTANCE)
        .setLocale(UK)
        .message("%{map,map-kv:'%{key} -> %{value,number:\"0000\"}',list-sep:', ',list-sep-last:' and '}");

    val map = new LinkedHashMap<String,Integer>();
    map.put("map1", 1);
    map.put("map2", -1234);
    map.put("map3", 8);

    assertEquals("map1 -> 0001, map2 -> -1234 and map3 -> 0008",
        message.with("map", map).format());
  }


  @Test
  @DisplayName("Custom key format")
  void testKeyFormat()
  {
    val message = MessageSupportFactory
        .create(createFormatterService(
            new MapFormatter(), new ArrayFormatter(), new BoolFormatter()),
            NO_CACHE_INSTANCE)
        .message("%{map,map-kv:'%{key,bool}:%{value}',list-sep:' / '}");

    val map = new LinkedHashMap<Integer,Integer>();
    map.put(10, 1);
    map.put(0, -1234);
    map.put(-4, 0);

    assertEquals("true:1 / false:-1234 / true:0",
        message.with("map", map).format());
  }
}
