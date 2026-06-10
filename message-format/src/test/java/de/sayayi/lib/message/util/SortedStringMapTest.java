/*
 * Copyright 2026 Jeroen Gremmen
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("SortedStringMap")
class SortedStringMapTest
{
  // ---- Construction ----

  @Nested
  @DisplayName("Construction")
  class ConstructionTest
  {
    @Test
    @DisplayName("Empty constructor creates empty unsealed map")
    @SuppressWarnings({"ConstantValue", "MismatchedQueryAndUpdateOfCollection"})
    void emptyConstructor()
    {
      var map = new SortedStringMap<String>();

      assertTrue(map.isEmpty());
      assertEquals(0, map.size());
    }


    @Test
    @DisplayName("Construct from null map")
    @SuppressWarnings("ConstantValue")
    void constructFromNullMap()
    {
      var map = new SortedStringMap<String>(null);

      assertTrue(map.isEmpty());
      assertEquals(0, map.size());
    }


    @Test
    @DisplayName("Construct from null map sealed")
    void constructFromNullMapSealed()
    {
      var map = new SortedStringMap<String>(null, true);

      assertTrue(map.isEmpty());
      assertThrows(UnsupportedOperationException.class, () -> map.put("a", "1"));
    }


    @Test
    @DisplayName("Construct from HashMap with 5 entries")
    void constructFromHashMap()
    {
      var source = new LinkedHashMap<String,Integer>();

      source.put("echo", 5);
      source.put("alpha", 1);
      source.put("charlie", 3);
      source.put("bravo", 2);
      source.put("delta", 4);

      var map = new SortedStringMap<>(source);

      assertEquals(5, map.size());
      assertEquals(1, map.get("alpha"));
      assertEquals(2, map.get("bravo"));
      assertEquals(3, map.get("charlie"));
      assertEquals(4, map.get("delta"));
      assertEquals(5, map.get("echo"));
    }


    @Test
    @DisplayName("Construct from another SortedStringMap")
    void constructFromSortedStringMap()
    {
      var original = new SortedStringMap<String>();

      original.put("alpha", "1");
      original.put("bravo", "2");
      original.put("charlie", "3");
      original.put("delta", "4");
      original.put("echo", "5");

      var copy = new SortedStringMap<>(original);

      assertEquals(5, copy.size());
      assertEquals("1", copy.get("alpha"));
      assertEquals("5", copy.get("echo"));

      // Verify independence
      copy.put("foxtrot", "6");
      assertEquals(5, original.size());
      assertEquals(6, copy.size());
    }


    @Test
    @DisplayName("Construct sealed from map")
    void constructSealedFromMap()
    {
      var source = Map.of("alpha", 1, "bravo", 2, "charlie", 3, "delta", 4, "echo", 5);
      var map = new SortedStringMap<>(source, true);

      assertEquals(5, map.size());
      assertEquals(1, map.get("alpha"));
      assertThrows(UnsupportedOperationException.class, () -> map.put("foxtrot", 6));
    }


    @Test
    @DisplayName("Construct from map containing null key throws")
    void constructFromMapWithNullKey()
    {
      var source = new HashMap<String,String>();

      source.put("alpha", "1");
      source.put(null, "2");

      assertThrows(NullPointerException.class, () -> new SortedStringMap<>(source));
    }


    @Test
    @DisplayName("Construct sealed from map containing null key throws")
    void constructSealedFromMapWithNullKey()
    {
      var source = new HashMap<String,String>();

      source.put("alpha", "1");
      source.put(null, "2");

      assertThrows(NullPointerException.class, () -> new SortedStringMap<>(source, true));
    }
  }




  // ---- Seal ----

  @Nested
  @DisplayName("Seal")
  class SealTest
  {
    @Test
    @DisplayName("Seal empty map")
    void sealEmptyMap()
    {
      var map = new SortedStringMap<String>();

      map.seal();

      assertTrue(map.isEmpty());
      assertThrows(UnsupportedOperationException.class, () -> map.put("a", "1"));
    }


    @Test
    @DisplayName("Seal non-empty map prevents modifications")
    void sealNonEmptyMap()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      map.seal();

      assertThrows(UnsupportedOperationException.class, () -> map.put("foxtrot", "6"));
      assertThrows(UnsupportedOperationException.class, () -> map.remove("alpha"));
      assertThrows(UnsupportedOperationException.class, map::clear);

      // Reading is still allowed
      assertEquals(5, map.size());
      assertEquals("1", map.get("alpha"));
    }
  }




  // ---- Size / isEmpty ----

  @Nested
  @DisplayName("Size and isEmpty")
  class SizeTest
  {
    @Test
    @DisplayName("Empty map")
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    void emptyMap()
    {
      var map = new SortedStringMap<String>();

      assertEquals(0, map.size());
      assertTrue(map.isEmpty());
    }


    @Test
    @DisplayName("Size decreases after remove")
    void sizeAfterRemove()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      map.remove("charlie");

      assertEquals(4, map.size());
    }
  }




  // ---- containsKey ----

  @Nested
  @DisplayName("containsKey")
  class ContainsKeyTest
  {
    @Test
    @DisplayName("Contains existing keys")
    void containsExistingKeys()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertTrue(map.containsKey("alpha"));
      assertTrue(map.containsKey("charlie"));
      assertTrue(map.containsKey("echo"));
    }


    @Test
    @DisplayName("Does not contain missing key")
    void doesNotContainMissingKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");

      assertFalse(map.containsKey("zulu"));
      assertFalse(map.containsKey(null));
    }


    @Test
    @DisplayName("Non-string key returns false")
    @SuppressWarnings("SuspiciousMethodCalls")
    void nonStringKeyReturnsFalse()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      assertFalse(map.containsKey(42));
    }
  }




  // ---- containsValue ----

  @Nested
  @DisplayName("containsValue")
  class ContainsValueTest
  {
    @Test
    @DisplayName("Contains existing values")
    void containsExistingValues()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertTrue(map.containsValue("1"));
      assertTrue(map.containsValue("3"));
      assertTrue(map.containsValue("5"));
    }


    @Test
    @DisplayName("Does not contain missing value")
    void doesNotContainMissingValue()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");

      assertFalse(map.containsValue("99"));
    }


    @Test
    @DisplayName("Contains null value")
    void containsNullValue()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", null);
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertTrue(map.containsValue(null));
    }


    @Test
    @DisplayName("Does not contain null value when absent")
    void doesNotContainNullValue()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");

      assertFalse(map.containsValue(null));
    }
  }




  // ---- get / getOrDefault ----

  @Nested
  @DisplayName("get and getOrDefault")
  class GetTest
  {
    @Test
    @DisplayName("Get existing key")
    void getExistingKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertEquals("3", map.get("charlie"));
    }


    @Test
    @DisplayName("Get missing key returns null")
    void getMissingKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      assertNull(map.get("zulu"));
    }


    @Test
    @DisplayName("Get null key when absent returns null")
    @SuppressWarnings("ConstantValue")
    void getNullKeyAbsent()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      assertNull(map.get(null));
    }


    @Test
    @DisplayName("getOrDefault with existing key")
    void getOrDefaultExistingKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertEquals("1", map.getOrDefault("alpha", "default"));
    }


    @Test
    @DisplayName("getOrDefault with missing key returns default")
    void getOrDefaultMissingKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      assertEquals("default", map.getOrDefault("zulu", "default"));
    }


    @Test
    @DisplayName("getOrDefault with null key absent")
    void getOrDefaultNullKeyAbsent()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      assertEquals("default", map.getOrDefault(null, "default"));
    }


    @Test
    @DisplayName("getOrDefault with non-string key returns default")
    @SuppressWarnings("SuspiciousMethodCalls")
    void getOrDefaultNonStringKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      assertEquals("default", map.getOrDefault(42, "default"));
    }
  }




  // ---- put ----

  @Nested
  @DisplayName("put")
  class PutTest
  {
    @Test
    @DisplayName("Put new keys returns null")
    void putNewKeys()
    {
      var map = new SortedStringMap<String>();

      assertNull(map.put("charlie", "3"));
      assertNull(map.put("alpha", "1"));
      assertNull(map.put("echo", "5"));
      assertNull(map.put("bravo", "2"));
      assertNull(map.put("delta", "4"));

      assertEquals(5, map.size());
    }


    @Test
    @DisplayName("Put existing key returns previous value")
    void putExistingKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertEquals("3", map.put("charlie", "33"));
      assertEquals("33", map.get("charlie"));
      assertEquals(5, map.size());
    }


    @Test
    @DisplayName("Put on sealed map throws")
    void putOnSealedMap()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      map.seal();

      assertThrows(UnsupportedOperationException.class, () -> map.put("bravo", "2"));
    }


    @Test
    @DisplayName("Put triggers array growth")
    void putTriggersArrayGrowth()
    {
      var map = new SortedStringMap<String>();

      // Initial capacity is 16 (8 entries), so adding more should trigger growth
      for(int i = 0; i < 12; i++)
        map.put("key" + String.format("%02d", i), "val" + i);

      assertEquals(12, map.size());
      for(int i = 0; i < 12; i++)
        assertEquals("val" + i, map.get("key" + String.format("%02d", i)));
    }


    @Test
    @DisplayName("Put null key throws NullPointerException")
    void putNullKeyThrows()
    {
      var map = new SortedStringMap<String>();

      assertThrows(NullPointerException.class, () -> map.put(null, "value"));
      assertTrue(map.isEmpty());
    }


    @Test
    @DisplayName("Put wildly mixed keys maintains sorted order")
    @SuppressWarnings("ExtractMethodRecommender")
    void putMixedKeys()
    {
      var map = new SortedStringMap<Integer>();

      map.put("mango", 1);
      map.put("apple", 2);
      map.put("zebra", 3);
      map.put("cherry", 4);
      map.put("walnut", 5);
      map.put("banana", 6);
      map.put("quince", 7);
      map.put("olive", 8);
      map.put("fig", 9);
      map.put("tomato", 10);
      map.put("date", 11);
      map.put("ugli", 12);
      map.put("kiwi", 13);
      map.put("grape", 14);
      map.put("salak", 15);
      map.put("nance", 16);
      map.put("lemon", 17);
      map.put("plum", 18);
      map.put("yuzu", 19);
      map.put("hip", 20);

      assertEquals(20, map.size());

      assertArrayEquals(
          new String[] {
              "apple", "banana", "cherry", "date", "fig",
              "grape", "hip", "kiwi", "lemon", "mango",
              "nance", "olive", "plum", "quince", "salak",
              "tomato", "ugli", "walnut", "yuzu", "zebra"
          },
          map.getKeys());

      assertEquals(2, map.get("apple"));
      assertEquals(9, map.get("fig"));
      assertEquals(17, map.get("lemon"));
      assertEquals(15, map.get("salak"));
      assertEquals(3, map.get("zebra"));
    }
  }




  // ---- remove ----

  @Nested
  @DisplayName("remove")
  class RemoveTest
  {
    @Test
    @DisplayName("Remove existing key")
    void removeExistingKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertEquals("3", map.remove("charlie"));
      assertEquals(4, map.size());
      assertFalse(map.containsKey("charlie"));
    }


    @Test
    @DisplayName("Remove first key")
    void removeFirstKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertEquals("1", map.remove("alpha"));
      assertEquals(4, map.size());
    }


    @Test
    @DisplayName("Remove last key")
    void removeLastKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertEquals("5", map.remove("echo"));
      assertEquals(4, map.size());
    }


    @Test
    @DisplayName("Remove missing key returns null")
    void removeMissingKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");

      assertNull(map.remove("zulu"));
      assertEquals(2, map.size());
    }


    @Test
    @DisplayName("Remove null key when absent returns null")
    void removeNullKeyAbsent()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      assertNull(map.remove(null));
    }


    @Test
    @DisplayName("Remove non-string key returns null")
    @SuppressWarnings("SuspiciousMethodCalls")
    void removeNonStringKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      assertNull(map.remove(42));
    }


    @Test
    @DisplayName("Remove on sealed map throws")
    void removeOnSealedMapThrows()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");
      map.seal();

      assertThrows(UnsupportedOperationException.class, () -> map.remove("alpha"));
    }
  }




  // ---- clear ----

  @Nested
  @DisplayName("clear")
  class ClearTest
  {
    @Test
    @DisplayName("Clear non-empty map")
    @SuppressWarnings("ConstantValue")
    void clearNonEmptyMap()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      map.clear();

      assertEquals(0, map.size());
      assertTrue(map.isEmpty());
      assertFalse(map.containsKey("alpha"));
    }


    @Test
    @DisplayName("Clear empty map does nothing")
    @SuppressWarnings("ConstantValue")
    void clearEmptyMap()
    {
      var map = new SortedStringMap<String>();

      map.clear();

      assertEquals(0, map.size());
    }


    @Test
    @DisplayName("Clear empty sealed map does not throw")
    void clearEmptySealedMap()
    {
      var map = new SortedStringMap<String>();

      map.seal();

      assertDoesNotThrow(map::clear);
    }


    @Test
    @DisplayName("Clear non-empty sealed map throws")
    void clearNonEmptySealedMapThrows()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      map.seal();

      assertThrows(UnsupportedOperationException.class, map::clear);
    }
  }




  // ---- getKeys ----

  @Nested
  @DisplayName("getKeys")
  class GetKeysTest
  {
    @Test
    @DisplayName("Keys are sorted")
    void keysAreSorted()
    {
      var map = new SortedStringMap<String>();

      map.put("echo", "5");
      map.put("alpha", "1");
      map.put("charlie", "3");
      map.put("bravo", "2");
      map.put("delta", "4");

      assertArrayEquals(new String[] { "alpha", "bravo", "charlie", "delta", "echo" }, map.getKeys());
    }


    @Test
    @DisplayName("Empty map keys")
    void emptyMapKeys() {
      assertArrayEquals(new String[0], new SortedStringMap<String>().getKeys());
    }
  }




  // ---- clone ----

  @Nested
  @DisplayName("clone")
  class CloneTest
  {
    @Test
    @DisplayName("Clone produces equal map")
    void cloneProducesEqualMap()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var cloned = map.clone();

      assertEquals(map, cloned);
      assertEquals(5, cloned.size());
    }


    @Test
    @DisplayName("Clone of sealed map is unsealed")
    void cloneOfSealedMapIsUnsealed()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");
      map.seal();

      var cloned = map.clone();

      assertDoesNotThrow(() -> cloned.put("foxtrot", "6"));
      assertEquals(6, cloned.size());
    }


    @Test
    @DisplayName("Clone is independent from original")
    void cloneIsIndependent()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var cloned = map.clone();

      cloned.put("foxtrot", "6");
      cloned.remove("alpha");

      assertEquals(5, map.size());
      assertTrue(map.containsKey("alpha"));
      assertFalse(map.containsKey("foxtrot"));
    }
  }




  // ---- forEach ----

  @Nested
  @DisplayName("forEach")
  class ForEachTest
  {
    @Test
    @DisplayName("forEach visits all entries in sorted order")
    void forEachVisitsAllEntries()
    {
      var map = new SortedStringMap<String>();

      map.put("echo", "5");
      map.put("alpha", "1");
      map.put("charlie", "3");
      map.put("bravo", "2");
      map.put("delta", "4");

      var keys = new ArrayList<String>();
      var values = new ArrayList<String>();

      map.forEach((k, v) -> {
        keys.add(k);
        values.add(v);
      });

      assertEquals(List.of("alpha", "bravo", "charlie", "delta", "echo"), keys);
      assertEquals(List.of("1", "2", "3", "4", "5"), values);
    }


    @Test
    @DisplayName("forEach on empty map")
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    void forEachEmptyMap()
    {
      var map = new SortedStringMap<String>();
      var count = new int[] { 0 };

      map.forEach((k, v) -> count[0]++);

      assertEquals(0, count[0]);
    }
  }




  // ---- stream ----

  @Nested
  @DisplayName("stream")
  class StreamTest
  {
    @Test
    @DisplayName("Stream collects all entries")
    void streamCollectsAllEntries()
    {
      var map = new SortedStringMap<Integer>();

      map.put("echo", 5);
      map.put("alpha", 1);
      map.put("charlie", 3);
      map.put("bravo", 2);
      map.put("delta", 4);

      var result = map
          .stream()
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

      assertEquals(5, result.size());
      assertEquals(1, result.get("alpha"));
      assertEquals(5, result.get("echo"));
    }


    @Test
    @DisplayName("Stream preserves sorted order")
    void streamPreservesOrder()
    {
      var map = new SortedStringMap<String>();

      map.put("echo", "5");
      map.put("alpha", "1");
      map.put("charlie", "3");
      map.put("bravo", "2");
      map.put("delta", "4");

      var keys = map
          .stream()
          .map(Entry::getKey)
          .collect(Collectors.toList());

      assertEquals(List.of("alpha", "bravo", "charlie", "delta", "echo"), keys);
    }
  }




  // ---- entrySet ----

  @Nested
  @DisplayName("entrySet")
  class EntrySetTest
  {
    @Test
    @DisplayName("entrySet size matches map size")
    @SuppressWarnings("RedundantCollectionOperation")
    void entrySetSize()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertEquals(5, map.entrySet().size());
      assertFalse(map.entrySet().isEmpty());
    }


    @Test
    @DisplayName("entrySet isEmpty for empty map")
    @SuppressWarnings("RedundantCollectionOperation")
    void entrySetIsEmpty() {
      assertTrue(new SortedStringMap<String>().entrySet().isEmpty());
    }


    @Test
    @DisplayName("entrySet iterator returns entries in sorted order")
    void entrySetIterator()
    {
      var map = new SortedStringMap<String>();

      map.put("echo", "5");
      map.put("alpha", "1");
      map.put("charlie", "3");
      map.put("bravo", "2");
      map.put("delta", "4");

      var iterator = map.entrySet().iterator();
      var keys = new ArrayList<String>();

      while(iterator.hasNext())
        keys.add(iterator.next().getKey());

      assertEquals(List.of("alpha", "bravo", "charlie", "delta", "echo"), keys);
    }


    @Test
    @DisplayName("entrySet iterator throws NoSuchElementException after last")
    void entrySetIteratorExhausted()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      var iterator = map.entrySet().iterator();
      iterator.next();

      assertThrows(NoSuchElementException.class, iterator::next);
    }


    @Test
    @DisplayName("entrySet clear delegates to map clear")
    @SuppressWarnings({"RedundantCollectionOperation", "WriteOnlyObject", "ConstantValue"})
    void entrySetClear()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      map.entrySet().clear();

      assertTrue(map.isEmpty());
    }


    @Test
    @DisplayName("entrySet remove throws UnsupportedOperationException")
    @SuppressWarnings({"WriteOnlyObject", "MismatchedQueryAndUpdateOfCollection"})
    void entrySetRemoveThrows()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      assertThrows(UnsupportedOperationException.class,
          () -> map.entrySet().remove(Map.entry("alpha", "1")));
    }


    @Test
    @DisplayName("entrySet removeIf throws UnsupportedOperationException")
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    void entrySetRemoveIfThrows()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      assertThrows(UnsupportedOperationException.class,
          () -> map.entrySet().removeIf(e -> true));
    }


    @Test
    @DisplayName("entrySet toArray returns all entries")
    void entrySetToArray()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var array = map.entrySet().toArray();

      assertEquals(5, array.length);
      //noinspection unchecked
      assertEquals("alpha", ((Entry<String,String>)array[0]).getKey());
    }


    @Test
    @DisplayName("entrySet toArray with generator")
    void entrySetToArrayWithGenerator()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var array = map.entrySet().toArray(Entry[]::new);

      assertEquals(5, array.length);
      assertEquals("alpha", array[0].getKey());
      assertEquals("1", array[0].getValue());
      assertEquals("echo", array[4].getKey());
      assertEquals("5", array[4].getValue());
    }


    @Test
    @DisplayName("entrySet toString")
    void entrySetToString()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");

      assertEquals("[alpha=1, bravo=2]", map.entrySet().toString());
    }


    @Test
    @DisplayName("entrySet toString for empty map")
    void entrySetToStringEmpty() {
      assertEquals("[]", new SortedStringMap<String>().entrySet().toString());
    }


    @Test
    @DisplayName("entrySet spliterator characteristics")
    void entrySetSpliterator()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var spliterator = map.entrySet().spliterator();

      assertEquals(5, spliterator.estimateSize());
      assertNull(spliterator.trySplit());
      assertTrue(spliterator.hasCharacteristics(Spliterator.ORDERED));
      assertTrue(spliterator.hasCharacteristics(Spliterator.DISTINCT));
      assertTrue(spliterator.hasCharacteristics(Spliterator.NONNULL));
      assertTrue(spliterator.hasCharacteristics(Spliterator.SIZED));
      assertTrue(spliterator.hasCharacteristics(Spliterator.IMMUTABLE));
      assertNull(spliterator.getComparator());
    }


    @Test
    @DisplayName("entrySet spliterator tryAdvance")
    void entrySetSpliteratorTryAdvance()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var spliterator = map.entrySet().spliterator();
      var keys = new ArrayList<String>();

      //noinspection StatementWithEmptyBody
      while(spliterator.tryAdvance(e -> keys.add(e.getKey())))
        ; // exhaust

      assertEquals(List.of("alpha", "bravo", "charlie", "delta", "echo"), keys);
    }


    @Test
    @DisplayName("entrySet spliterator tryAdvance on empty map returns false")
    void entrySetSpliteratorTryAdvanceEmpty()
    {
      var map = new SortedStringMap<String>();
      var spliterator = map.entrySet().spliterator();

      assertFalse(spliterator.tryAdvance(e -> fail("should not be called")));
    }
  }




  // ---- EntryDelegate ----

  @Nested
  @DisplayName("EntryDelegate")
  class EntryDelegateTest
  {
    @Test
    @DisplayName("Entry getKey and getValue")
    void entryGetKeyAndValue()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var entry = map.entrySet().iterator().next();

      assertEquals("alpha", entry.getKey());
      assertEquals("1", entry.getValue());
    }


    @Test
    @DisplayName("Entry setValue always throws")
    void entrySetValueThrows()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var entry = map.entrySet().iterator().next();

      assertThrows(UnsupportedOperationException.class, () -> entry.setValue("11"));
    }


    @Test
    @DisplayName("Entry equals")
    @SuppressWarnings({"MisorderedAssertEqualsArguments", "AssertBetweenInconvertibleTypes"})
    void entryEquals()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var entry = map.entrySet().iterator().next();

      assertEquals(entry, Map.entry("alpha", "1"));
      assertNotEquals(entry, Map.entry("alpha", "99"));
      assertNotEquals(entry, Map.entry("bravo", "1"));
      assertNotEquals(entry, "not an entry");
    }


    @Test
    @DisplayName("Entry hashCode consistent with Map.Entry contract")
    void entryHashCode()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      var entry = map.entrySet().iterator().next();
      var expected = "alpha".hashCode() ^ "1".hashCode();

      assertEquals(expected, entry.hashCode());
    }


    @Test
    @DisplayName("Entry toString")
    void entryToString()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");

      var entry = map.entrySet().iterator().next();

      assertEquals("alpha=1", entry.toString());
    }
  }




  // ---- equals ----

  @Nested
  @DisplayName("equals")
  class EqualsTest
  {
    @Test
    @DisplayName("Equal to itself")
    @SuppressWarnings("EqualsWithItself")
    void equalToItself()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      assertEquals(map, map);
    }


    @Test
    @DisplayName("Equal to HashMap with same entries")
    void equalToHashMap()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var hashMap = new HashMap<String,String>();

      hashMap.put("echo", "5");
      hashMap.put("alpha", "1");
      hashMap.put("bravo", "2");
      hashMap.put("charlie", "3");
      hashMap.put("delta", "4");

      assertEquals(map, hashMap);
      assertEquals(hashMap, map);
    }


    @Test
    @DisplayName("Not equal to map with different size")
    void notEqualDifferentSize()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");

      var other = Map.of("alpha", "1");

      assertNotEquals(map, other);
    }


    @Test
    @DisplayName("Not equal to map with different values")
    void notEqualDifferentValues()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var other = Map.of("alpha", "1", "bravo", "2", "charlie", "99", "delta", "4", "echo", "5");

      assertNotEquals(map, other);
    }


    @Test
    @DisplayName("Not equal to non-map")
    @SuppressWarnings({"MisorderedAssertEqualsArguments", "AssertBetweenInconvertibleTypes"})
    void notEqualToNonMap()
    {
      var map = new SortedStringMap<String>();

      assertNotEquals(map, "not a map");
    }


    @Test
    @DisplayName("Equal maps with null values")
    void equalWithNullValues()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", null);
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var hashMap = new HashMap<String,String>();

      hashMap.put("alpha", null);
      hashMap.put("bravo", "2");
      hashMap.put("charlie", "3");
      hashMap.put("delta", "4");
      hashMap.put("echo", "5");

      assertEquals(map, hashMap);
    }


    @Test
    @DisplayName("Not equal when null value vs non-null value")
    void notEqualNullVsNonNull()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", null);

      var other = Map.of("alpha", "1");

      assertNotEquals(map, other);
    }
  }




  // ---- hashCode ----

  @Nested
  @DisplayName("hashCode")
  class HashCodeTest
  {
    @Test
    @DisplayName("hashCode consistent with HashMap")
    void hashCodeConsistentWithHashMap()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var hashMap = new HashMap<String,String>();

      hashMap.put("alpha", "1");
      hashMap.put("bravo", "2");
      hashMap.put("charlie", "3");
      hashMap.put("delta", "4");
      hashMap.put("echo", "5");

      assertEquals(hashMap.hashCode(), map.hashCode());
    }


    @Test
    @DisplayName("hashCode for empty map is 0")
    void hashCodeEmptyMap() {
      assertEquals(0, new SortedStringMap<String>().hashCode());
    }
  }




  // ---- toString ----

  @Nested
  @DisplayName("toString")
  class ToStringTest
  {
    @Test
    @DisplayName("toString for empty map")
    void toStringEmptyMap() {
      assertEquals("{}", new SortedStringMap<String>().toString());
    }


    @Test
    @DisplayName("toString for non-empty map in sorted order")
    void toStringNonEmptyMap()
    {
      var map = new SortedStringMap<String>();

      map.put("bravo", "2");
      map.put("alpha", "1");

      assertEquals("{alpha=1, bravo=2}", map.toString());
    }
  }




  // ---- putAll (inherited from AbstractMap) ----

  @Nested
  @DisplayName("putAll")
  class PutAllTest
  {
    @Test
    @DisplayName("putAll from HashMap")
    void putAllFromHashMap()
    {
      var map = new SortedStringMap<String>();
      var source = Map.of("echo", "5", "alpha", "1", "charlie", "3", "bravo", "2", "delta", "4");

      map.putAll(source);

      assertEquals(5, map.size());
      assertEquals("1", map.get("alpha"));
      assertEquals("2", map.get("bravo"));
      assertEquals("3", map.get("charlie"));
      assertEquals("4", map.get("delta"));
      assertEquals("5", map.get("echo"));
    }


    @Test
    @DisplayName("putAll on sealed map throws")
    @SuppressWarnings("RedundantCollectionOperation")
    void putAllOnSealedMap()
    {
      var map = new SortedStringMap<String>();

      map.seal();

      assertThrows(UnsupportedOperationException.class,
          () -> map.putAll(Map.of("alpha", "1")));
    }
  }




  // ---- keySet / values (inherited from AbstractMap) ----

  @Nested
  @DisplayName("keySet and values")
  class KeySetValuesTest
  {
    @Test
    @DisplayName("keySet contains all keys")
    void keySetContainsAllKeys()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var keySet = map.keySet();

      assertEquals(5, keySet.size());
      assertTrue(keySet.contains("alpha"));
      assertTrue(keySet.contains("echo"));
    }


    @Test
    @DisplayName("values contains all values")
    void valuesContainsAllValues()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      var values = map.values();

      assertEquals(5, values.size());
      assertTrue(values.contains("1"));
      assertTrue(values.contains("5"));
    }
  }




  // ---- Sorted order integrity ----

  @Nested
  @DisplayName("Sorted order integrity")
  class SortedOrderTest
  {
    @Test
    @DisplayName("Entries remain sorted after interleaved puts and removes")
    void sortedAfterMixedOperations()
    {
      var map = new SortedStringMap<String>();

      map.put("delta", "4");
      map.put("alpha", "1");
      map.put("echo", "5");
      map.put("bravo", "2");
      map.put("charlie", "3");

      map.remove("charlie");
      map.put("foxtrot", "6");
      map.remove("alpha");
      map.put("golf", "7");

      assertArrayEquals(
          new String[] { "bravo", "delta", "echo", "foxtrot", "golf" },
          map.getKeys());
    }


    @Test
    @DisplayName("Re-add removed key maintains sorted order")
    void reAddRemovedKey()
    {
      var map = new SortedStringMap<String>();

      map.put("alpha", "1");
      map.put("bravo", "2");
      map.put("charlie", "3");
      map.put("delta", "4");
      map.put("echo", "5");

      map.remove("charlie");
      map.put("charlie", "33");

      assertArrayEquals(
          new String[] { "alpha", "bravo", "charlie", "delta", "echo" },
          map.getKeys());
      assertEquals("33", map.get("charlie"));
    }
  }
}
