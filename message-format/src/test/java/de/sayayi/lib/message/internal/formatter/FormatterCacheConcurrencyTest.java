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
package de.sayayi.lib.message.internal.formatter;

import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import lombok.val;
import org.junit.jupiter.api.*;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import static de.sayayi.lib.message.util.ConcurrentTestSupport.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the concurrency behavior of {@link FormatterCache}: formatters must not be built while its lock is held.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("Formatter cache concurrency")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class FormatterCacheConcurrencyTest
{
  private static final ParameterFormatter[] EMPTY = new ParameterFormatter[0];


  @RepeatedTest(10)
  @DisplayName("Concurrent lookups for different types do not corrupt cache")
  void concurrentLookupsForDifferentTypes() throws Exception
  {
    val cache = new FormatterCache(16);
    val types = new Class<?>[] {
        String.class, Integer.class, Long.class, Double.class,
        Float.class, Boolean.class, Byte.class, Short.class,
        List.class, Map.class, Set.class, Queue.class,
        Comparable.class, Serializable.class, Iterable.class, Collection.class
    };

    runConcurrently(types.length, idx -> assertNotNull(cache.lookup(types[idx], t -> EMPTY)));

    // verify all types are retrievable
    for(var type: types)
      assertArrayEquals(EMPTY, cache.lookup(type, t -> fail("should be cached")));
  }


  @RepeatedTest(10)
  @DisplayName("Concurrent lookups for the same type return the formatters cached first")
  void concurrentLookupsForSameType() throws Exception
  {
    val cache = new FormatterCache(8);

    // several threads build formatters for the same type at the same time, but only the first result is cached
    val results = getConcurrently(20, idx -> cache.lookup(String.class, t -> {
      try {
        Thread.sleep(10);
      } catch(InterruptedException ignored) {
      }

      return new ParameterFormatter[0];
    }));

    val cached = cache.lookup(String.class, t -> fail("should be cached"));

    for(var result: results)
      assertSame(cached, result);
  }


  @RepeatedTest(10)
  @DisplayName("Concurrent lookups with cache eviction do not corrupt state")
  void concurrentLookupsWithEviction() throws Exception
  {
    val cache = new FormatterCache(8);

    // each thread inserts a unique set of types that will cause eviction
    val allTypes = new Class<?>[][] {
        { String.class, Integer.class, Long.class, Double.class, Float.class, Boolean.class, Byte.class, Short.class, List.class },
        { Map.class, Set.class, Queue.class, Comparable.class, Serializable.class, Iterable.class, Collection.class, Deque.class },
    };

    runConcurrently(16, idx -> {
      for(var type: allTypes[idx % 2])
        cache.lookup(type, t -> EMPTY);
    });

    // cache should still be functional
    assertNotNull(cache.lookup(String.class, t -> EMPTY));
    assertDoesNotThrow(cache::toString);
  }


  @RepeatedTest(10)
  @DisplayName("Clear during concurrent lookups does not cause exceptions")
  void clearDuringConcurrentLookups() throws Exception
  {
    val cache = new FormatterCache(8);
    val types = new Class<?>[] {
        String.class, Integer.class, Long.class, Double.class,
        Float.class, Boolean.class, Byte.class, Short.class
    };

    // pre-fill cache
    for(var type: types)
      cache.lookup(type, t -> EMPTY);

    // repeatedly clear the cache while lookups are happening
    runWithReaders(1, writer -> {
      for(var i = 0; i < 100; i++)
      {
        cache.clear();
        Thread.yield();
      }
    }, 8, reader -> assertNotNull(cache.lookup(types[reader % types.length], t -> EMPTY)));
  }


  @RepeatedTest(10)
  @DisplayName("toString during concurrent modifications does not throw")
  void toStringDuringConcurrentModifications() throws Exception
  {
    val cache = new FormatterCache(8);
    val types = new Class<?>[] {
        String.class, Integer.class, Long.class, Double.class,
        Float.class, Boolean.class, Byte.class, Short.class
    };

    // the writer continuously adds and evicts, the reader continuously calls toString
    runWithReaders(1, writer -> {
      for(var i = 0; i < 2000; i++)
      {
        cache.lookup(types[i % types.length], t -> EMPTY);
        if (i % 10 == 0)
          cache.clear();
      }
    }, 1, reader -> {
      val s = cache.toString();

      assertTrue(s.startsWith("["));
      assertTrue(s.endsWith("]"));
    });
  }


  @Test
  @DisplayName("buildFormatters is not called under lock (no deadlock with slow builder)")
  void buildFormattersNotCalledUnderLock() throws Exception
  {
    val cache = new FormatterCache(8);
    val buildStarted = new CountDownLatch(1);
    val lookupDone = new CountDownLatch(1);

    // thread 1: lookup with a slow buildFormatters
    val thread1 = new Thread(() -> cache.lookup(String.class, t -> {
      buildStarted.countDown();

      try {
        Thread.sleep(200);
      } catch(InterruptedException ignored) {
      }

      return EMPTY;
    }));

    // thread 2: lookup for a different type while thread 1's buildFormatters is running
    val thread2 = new Thread(() -> {
      try {
        buildStarted.await();

        // this should not block because buildFormatters runs outside the lock
        cache.lookup(Integer.class, t -> EMPTY);
        lookupDone.countDown();
      } catch(InterruptedException ignored) {
      }
    });

    thread1.start();
    thread2.start();

    // thread 2 should complete quickly (within 100ms) even though thread 1's build takes 200ms
    assertTrue(lookupDone.await(150, MILLISECONDS),
        "Lookup blocked while another thread's buildFormatters was executing");

    thread1.join(5000);
    thread2.join(5000);
  }


  @RepeatedTest(5)
  @DisplayName("High contention stress test with many threads")
  @SuppressWarnings("ResultOfMethodCallIgnored")
  void highContentionStressTest() throws Exception
  {
    val cache = new FormatterCache(8);
    val types = new Class<?>[] {
        String.class, Integer.class, Long.class, Double.class,
        Float.class, Boolean.class, Byte.class, Short.class,
        List.class, Map.class, Set.class, Queue.class
    };

    runConcurrently(32, idx -> {
      val rng = ThreadLocalRandom.current();

      for(var i = 0; i < 500; i++)
      {
        assertNotNull(cache.lookup(types[rng.nextInt(types.length)], t -> EMPTY));

        // occasionally clear
        if (rng.nextInt(100) == 0)
          cache.clear();

        // occasionally toString
        if (rng.nextInt(50) == 0)
          cache.toString();
      }
    });
  }
}
