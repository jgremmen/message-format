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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
class ImmutableArrayMapTest
{
  @Test
  @DisplayName("Empty map construction")
  void testEmptyConstruct()
  {
    val iam = new ImmutableArrayMap<>(emptyMap());

    assertEquals(0, iam.size());
    assertTrue(iam.isEmpty());
    assertEquals(0, iam.hashCode());
    assertEquals("{}", iam.toString());
    assertEquals(emptyMap(), iam);
    assertEquals(emptySet(), iam.keySet());
    assertEquals(emptyList(), iam.values());
    assertEquals(emptySet(), iam.entrySet());
  }


  @Test
  @DisplayName("Map construction with various typed values")
  void testConstruct()
  {
    val testMap = createTestMap();
    val iam = new ImmutableArrayMap<>(testMap);

    assertEquals(5, iam.size());
    assertFalse(iam.isEmpty());

    assertTrue(iam.containsKey(true));
    assertTrue(iam.containsKey(null));
    assertTrue(iam.containsKey(45));
    assertTrue(iam.containsKey("String"));
    assertTrue(iam.containsKey(Long.MIN_VALUE));

    assertTrue(iam.containsValue("boolean"));
    assertTrue(iam.containsValue(emptyList()));
    assertTrue(iam.containsValue(-3.1415d));
    assertTrue(iam.containsValue(null));
    assertTrue(iam.containsValue('A'));

    assertEquals(testMap, iam);
    assertEquals(iam, testMap);
  }


  @Test
  @DisplayName("Immutable Map specific forEach implementation")
  void testForEach()
  {
    val swapMap = new LinkedHashMap<>();

    new ImmutableArrayMap<>(createTestMap()).forEach((key,value) -> swapMap.put(value, key));

    val iam = new ImmutableArrayMap<>(swapMap);

    assertEquals(new LinkedHashSet<>(asList("boolean", emptyList(), -3.1415d, null, 'A')),
        iam.keySet());
    assertArrayEquals(new Object[] { true, null, 45, "String", Long.MIN_VALUE },
        iam.values().toArray());
  }


  @Test
  @DisplayName("Get values by key")
  void testGet()
  {
    val iam = new ImmutableArrayMap<>(createTestMap());

    assertEquals("boolean", iam.get(true));
    assertEquals(emptyList(), iam.get(null));
    assertEquals(-3.1415d, iam.get(45));
    assertNull(iam.get("String"));
    assertEquals('A', iam.get(Long.MIN_VALUE));

    assertEquals("<unknown>", iam.getOrDefault(45L, "<unknown>"));
  }


  @Test
  @DisplayName("Large map (~1000 entries)")
  @SuppressWarnings("UnnecessaryBoxing")
  void testLargeMap()
  {
    val referenceMap = new LinkedHashMap<String,Long>();
    val random = new Random();

    for(int n = 0; n < 12500; n += abs(random.nextInt() % 25) + 1)
      referenceMap.put("#" + n, Long.valueOf(n));

    val iam = new ImmutableArrayMap<>(referenceMap);

    assertEquals(referenceMap.size(), iam.size());

    assertEquals(referenceMap, iam);
    assertEquals(iam, referenceMap);

    assertEquals(referenceMap.entrySet(), iam.entrySet());
    assertEquals(iam.entrySet(), referenceMap.entrySet());
  }


  private @NotNull Map<Object,Object> createTestMap()
  {
    val map = new LinkedHashMap<>();

    map.put(true, "boolean");
    map.put(null, emptyList());
    map.put(45, -3.1415d);
    map.put("String", null);
    map.put(Long.MIN_VALUE, 'A');

    return map;
  }
}
