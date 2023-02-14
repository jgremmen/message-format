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
package de.sayayi.lib.message.formatter;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 */
public class FixedSizeCacheMapTest
{
  @Test
  public void testPutAndKeyIterator()
  {
    FixedSizeCacheMap<Integer,String> map = new FixedSizeCacheMap<>(3);

    map.put(3, "3");
    map.put(45, "45");

    assertEquals(2, map.size());

    map.put(2, "2");
    map.put(16, "16");

    assertEquals(3, map.size());

    Iterator<Integer> keyIterator = map.keySet().iterator();

    assertEquals(16, keyIterator.next().intValue());
    assertEquals(2, keyIterator.next().intValue());
    assertEquals(45, keyIterator.next().intValue());
  }


  @Test
  public void testPutAndValueIterator()
  {
    FixedSizeCacheMap<Integer,String> map = new FixedSizeCacheMap<>(3);

    map.put(3, "3");
    map.put(45, "45");
    map.put(2, "2");
    map.put(16, "16");

    assertEquals("2", map.put(2, "2-new"));

    assertEquals(3, map.size());

    Iterator<String> valueIterator = map.values().iterator();

    assertEquals("2-new", valueIterator.next());
    assertEquals("16", valueIterator.next());
    assertEquals("45", valueIterator.next());
  }


  @Test
  public void testGetAndFirstValue()
  {
    FixedSizeCacheMap<Integer,String> map = new FixedSizeCacheMap<>(3);

    map.put(31, "31");
    map.put(8, "8");
    map.put(1, "1");

    assertEquals("1", map.values().iterator().next());

    assertEquals("31", map.get(31));
    assertEquals("31", map.values().iterator().next());

    assertTrue(map.containsKey(8));
    assertEquals("31", map.values().iterator().next());

    assertEquals("8", map.get(8));
    assertEquals("8", map.values().iterator().next());
  }


  @Test
  public void testTooSmallCacheSize() {
    assertThrows(IllegalArgumentException.class, () -> new FixedSizeCacheMap<String,String>(1));
  }


  @Test
  public void testClone()
  {
    FixedSizeCacheMap<Integer,String> map = new FixedSizeCacheMap<>(3);

    map.put(31, "31");
    map.put(8, "8");
    map.put(1, "1");

    Map<Integer,String> mc = map.clone();
    assertEquals(3, mc.size());

    map.put(12, "12");

    Iterator<String> valueIterator = mc.values().iterator();

    assertEquals("1", valueIterator.next());
    assertEquals("8", valueIterator.next());
    assertEquals("31", valueIterator.next());
    assertFalse(valueIterator.hasNext());
  }
}
