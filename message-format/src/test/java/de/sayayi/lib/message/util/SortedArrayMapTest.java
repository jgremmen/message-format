/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.util;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 * @since 0.9.2
 */
class SortedArrayMapTest
{
  @Test
  @DisplayName("Empty map")
  void testEmptyMap()
  {
    val map = new SortedArrayMap<String,Integer>(Map.of());

    assertTrue(map.isEmpty());
    assertEquals(0, map.size());
    assertFalse(map.iterator().hasNext());
    assertNull(map.findValue(null));
    assertNull(map.findValue("hello"));
    assertArrayEquals(new String[0], map.getKeys(String.class));
  }


  @Test
  @DisplayName("Single entry map with null key")
  void testSingleEntryNull()
  {
    val map = new SortedArrayMap<String,Integer>(singletonMap(null, -1));

    assertFalse(map.isEmpty());
    assertEquals(1, map.size());
    assertEquals(-1, map.findValue(null));
    assertNull(map.findValue("hello"));
    assertArrayEquals(new String[] { null }, map.getKeys(String.class));

    val nullEntry = map.iterator().next();

    assertNull(nullEntry.getKey());
    assertEquals(-1, nullEntry.getValue());
    assertThrows(UnsupportedOperationException.class, () -> nullEntry.setValue(100));
  }


  @Test
  @DisplayName("Sorted map keys (null first)")
  void testSortedKeys()
  {
    val map = new SortedArrayMap<>(new HashMap<String,Integer>() {{
      put("X", 5);
      put("D", -9);
      put(null, 100);
      put("B", 45);
      put("A", -32);
    }});

    assertEquals(5, map.size());
    assertEquals("null,A,B,D,X", String.join(",", map.getKeys(String.class)));
    assertEquals("A#B#D#X", map
        .stream()
        .map(Map.Entry::getKey)
        .filter(Objects::nonNull)
        .collect(joining("#")));

    assertEquals(-32, map.findValue("A"));
    assertEquals(45, map.findValue("B"));
    assertEquals(-9, map.findValue("D"));
    assertEquals(100, map.findValue(null));
    assertEquals(5, map.findValue("X"));
  }
}
