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
package de.sayayi.lib.message.internal;

import org.junit.jupiter.api.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static de.sayayi.lib.message.util.ConcurrentTestSupport.getConcurrently;
import static de.sayayi.lib.message.util.ConcurrentTestSupport.runConcurrently;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link BoundedCache}: lookup, least-recently-used eviction, failing value creation and concurrent access.
 *
 * @author Jeroen Gremmen
 * @since 0.25.1
 */
@DisplayName("Bounded cache")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class BoundedCacheTest
{
  @Test
  @DisplayName("Cached value is returned without creating it again")
  void cachedValue()
  {
    var cache = new BoundedCache<String,String>(4);

    assertEquals("A", cache.get("a", String::toUpperCase));
    assertEquals("A", cache.get("a", k -> fail("should be cached")));
    assertEquals(1, cache.size());
  }


  @Test
  @DisplayName("Least recently used entry is evicted")
  void evictLeastRecentlyUsed()
  {
    var cache = new BoundedCache<String,String>(3);

    cache.get("a", String::toUpperCase);
    cache.get("b", String::toUpperCase);
    cache.get("c", String::toUpperCase);

    // use a, so b is the least recently used entry
    cache.get("a", k -> fail("should be cached"));

    // evicts b
    cache.get("d", String::toUpperCase);

    assertEquals(3, cache.size());
    assertEquals("A", cache.get("a", k -> fail("should be cached")));
    assertEquals("C", cache.get("c", k -> fail("should be cached")));
    assertEquals("D", cache.get("d", k -> fail("should be cached")));
    assertEquals("b2", cache.get("b", k -> "b2"));
  }


  @Test
  @DisplayName("Entry used after the latest insert is more recent than that insert")
  void hitAfterInsertIsMoreRecent()
  {
    var cache = new BoundedCache<String,String>(2);

    cache.get("a", String::toUpperCase);
    cache.get("b", String::toUpperCase);

    // use a, so b is the least recently used entry, although b was added last
    cache.get("a", k -> fail("should be cached"));

    // evicts b
    cache.get("c", String::toUpperCase);

    assertEquals("A", cache.get("a", k -> fail("should be cached")));
    assertEquals("b2", cache.get("b", k -> "b2"));
  }


  @Test
  @DisplayName("Failing value creation is not cached")
  void failingCreationIsNotCached()
  {
    var cache = new BoundedCache<String,String>(4);

    assertThrows(IllegalArgumentException.class, () -> cache.get("a", k -> {
      throw new IllegalArgumentException();
    }));
    assertEquals(0, cache.size());
    assertEquals("A", cache.get("a", String::toUpperCase));
  }


  @Test
  @DisplayName("Non-positive maximum size is rejected")
  void invalidMaxSize()
  {
    assertThrows(IllegalArgumentException.class, () -> new BoundedCache<String,String>(0));
    assertThrows(IllegalArgumentException.class, () -> new BoundedCache<String,String>(-1));
  }


  @RepeatedTest(10)
  @DisplayName("Concurrent lookups for the same key return the value cached first")
  void concurrentLookupsForSameKey() throws Exception
  {
    var cache = new BoundedCache<String,Object>(8);

    // slow value creation, so the threads create their values at the same time
    var results = getConcurrently(16, idx -> cache.get("key", k -> {
      try {
        Thread.sleep(10);
      } catch(InterruptedException ignored) {
      }

      return new Object();
    }));

    var cached = cache.get("key", k -> fail("should be cached"));

    for(var result: results)
      assertSame(cached, result);
  }


  @RepeatedTest(5)
  @DisplayName("Concurrent lookups with eviction keep the cache bounded and consistent")
  void concurrentLookupsWithEviction() throws Exception
  {
    var maxSize = 16;
    var threads = 16;
    var cache = new BoundedCache<Integer,Integer>(maxSize);
    var creations = new AtomicInteger();

    runConcurrently(threads, idx -> {
      var rng = ThreadLocalRandom.current();

      for(var i = 0; i < 2000; i++)
      {
        var key = rng.nextInt(64);

        assertEquals(key * 2, cache.get(key, k -> {
          creations.incrementAndGet();
          return k * 2;
        }));
      }
    });

    // a few entries may exceed the maximum size while other threads are evicting
    assertTrue(cache.size() <= maxSize + threads, "cache size " + cache.size() + " exceeds bound");
    assertTrue(creations.get() > 64, "eviction never happened");
  }
}
