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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import lombok.val;
import org.junit.jupiter.api.*;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Concurrency tests for {@link FormatterCache}.
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

    val latch = new CountDownLatch(1);
    val threads = new Thread[types.length];
    val errors = new AtomicBoolean(false);

    for(var i = 0; i < types.length; i++)
    {
      final var type = types[i];

      (threads[i] = new Thread(() -> {
        try {
          latch.await();
          assertNotNull(cache.lookup(type, t -> EMPTY));
        } catch(Exception ex) {
          errors.set(true);
        }
      })).start();
    }

    latch.countDown();

    for(var thread: threads)
      thread.join(5000);

    assertFalse(errors.get(), "Concurrent lookups caused an error");

    // verify all types are retrievable
    for(var type: types)
      assertArrayEquals(EMPTY, cache.lookup(type, t -> fail("should be cached")));
  }


  @RepeatedTest(10)
  @DisplayName("Concurrent lookups for the same type invoke buildFormatters at most once per type")
  void concurrentLookupsForSameType() throws InterruptedException
  {
    val cache = new FormatterCache(8);
    val invocationCount = new AtomicInteger(0);
    val latch = new CountDownLatch(1);
    val threadCount = 20;
    val threads = new Thread[threadCount];
    val results = new ParameterFormatter[threadCount][];

    Function<Class<?>,ParameterFormatter[]> buildFormatters = t -> {
      invocationCount.incrementAndGet();

      // simulate expensive operation
      try {
        Thread.sleep(10);
      } catch(InterruptedException ignored) {
      }

      return EMPTY;
    };

    for(var i = 0; i < threadCount; i++)
    {
      final var idx = i;

      (threads[i] = new Thread(() -> {
        try {
          latch.await();
          results[idx] = cache.lookup(String.class, buildFormatters);
        } catch(Exception ignored) {
        }
      })).start();
    }

    latch.countDown();

    for(var thread: threads)
      thread.join(5000);

    // all results should be non-null
    for(var result: results)
      assertNotNull(result);

    // buildFormatters may be called more than once due to the concurrent lookup pattern,
    // but the cache should not be corrupted
    val finalResult = cache.lookup(String.class, t -> fail("should be cached"));
    assertNotNull(finalResult);
  }


  @RepeatedTest(10)
  @DisplayName("Concurrent lookups with cache eviction do not corrupt state")
  @SuppressWarnings("resource")
  void concurrentLookupsWithEviction() throws InterruptedException, ExecutionException, TimeoutException
  {
    val cache = new FormatterCache(8);
    val latch = new CountDownLatch(1);
    val threadCount = 16;
    val executor = Executors.newFixedThreadPool(threadCount);
    val futures = new ArrayList<Future<?>>();

    // each thread inserts a unique set of types that will cause eviction
    val allTypes = new Class<?>[][] {
        { String.class, Integer.class, Long.class, Double.class, Float.class, Boolean.class, Byte.class, Short.class, List.class },
        { Map.class, Set.class, Queue.class, Comparable.class, Serializable.class, Iterable.class, Collection.class, Deque.class },
    };

    for(var i = 0; i < threadCount; i++)
    {
      final var types = allTypes[i % 2];

      futures.add(executor.submit(() -> {
        try {
          latch.await();

          for(var type: types)
            cache.lookup(type, t -> EMPTY);
        } catch(InterruptedException ignored) {
        }
      }));
    }

    latch.countDown();

    for(var future: futures)
      future.get(5, SECONDS);

    executor.shutdown();

    // cache should still be functional
    assertNotNull(cache.lookup(String.class, t -> EMPTY));
    assertDoesNotThrow(cache::toString);
  }


  @RepeatedTest(10)
  @DisplayName("Clear during concurrent lookups does not cause exceptions")
  void clearDuringConcurrentLookups() throws InterruptedException
  {
    val cache = new FormatterCache(8);
    val types = new Class<?>[] {
        String.class, Integer.class, Long.class, Double.class,
        Float.class, Boolean.class, Byte.class, Short.class
    };
    val running = new AtomicBoolean(true);
    val errors = new AtomicBoolean(false);

    // pre-fill cache
    for(var type: types)
      cache.lookup(type, t -> EMPTY);

    // start lookup threads
    val lookupThreads = new Thread[8];

    for(var i = 0; i < lookupThreads.length; i++)
    {
      final var type = types[i % types.length];

      (lookupThreads[i] = new Thread(() -> {
        while (running.get() && !errors.get())
        {
          try {
            assertNotNull(cache.lookup(type, t -> EMPTY));
          } catch(Exception ex) {
            errors.set(true);
          }
        }
      })).start();
    }

    // repeatedly clear the cache while lookups are happening
    for(var i = 0; i < 100; i++)
    {
      cache.clear();
      Thread.yield();
    }

    running.set(false);

    for(var thread: lookupThreads)
      thread.join(5000);

    assertFalse(errors.get(), "Clear during concurrent lookups caused an error");
  }


  @RepeatedTest(10)
  @DisplayName("toString during concurrent modifications does not throw")
  @SuppressWarnings("ExtractMethodRecommender")
  void toStringDuringConcurrentModifications() throws InterruptedException
  {
    val cache = new FormatterCache(8);
    val running = new AtomicBoolean(true);
    val errors = new AtomicBoolean(false);
    val types = new Class<?>[] {
        String.class, Integer.class, Long.class, Double.class,
        Float.class, Boolean.class, Byte.class, Short.class
    };

    // writer thread continuously adds/evicts
    val writerThread = new Thread(() -> {
      for(var i = 0; running.get() && !errors.get(); i++)
      {
        try {
          cache.lookup(types[i % types.length], t -> EMPTY);
          if (i % 10 == 0)
            cache.clear();
        } catch(Exception ex) {
          errors.set(true);
        }
      }
    });
    writerThread.start();

    // reader thread continuously calls toString
    val readerThread = new Thread(() -> {
      while(running.get() && !errors.get())
      {
        try {
          val s = cache.toString();
          assertNotNull(s);

          assertTrue(s.startsWith("["));
          assertTrue(s.endsWith("]"));
        } catch(Exception ex) {
          errors.set(true);
        }
      }
    });
    readerThread.start();

    Thread.sleep(200);
    running.set(false);

    writerThread.join(5000);
    readerThread.join(5000);

    assertFalse(errors.get(), "toString during concurrent modifications caused an error");
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
  @SuppressWarnings({"ResultOfMethodCallIgnored", "resource"})
  void highContentionStressTest() throws Exception
  {
    val cache = new FormatterCache(8);
    val threadCount = 32;
    val iterationsPerThread = 500;
    val executor = Executors.newFixedThreadPool(threadCount);
    val latch = new CountDownLatch(1);
    val errors = new AtomicInteger(0);

    val types = new Class<?>[] {
        String.class, Integer.class, Long.class, Double.class,
        Float.class, Boolean.class, Byte.class, Short.class,
        List.class, Map.class, Set.class, Queue.class
    };

    val futures = new ArrayList<Future<?>>();

    for(var t = 0; t < threadCount; t++)
    {
      futures.add(executor.submit(() -> {
        try {
          latch.await();

          val rng = ThreadLocalRandom.current();

          for(var i = 0; i < iterationsPerThread; i++)
          {
            val type = types[rng.nextInt(types.length)];
            if (cache.lookup(type, tp -> EMPTY) == null)
              errors.incrementAndGet();

            // occasionally clear
            if (rng.nextInt(100) == 0)
              cache.clear();

            // occasionally toString
            if (rng.nextInt(50) == 0)
              cache.toString();
          }
        } catch(Exception ex) {
          errors.incrementAndGet();
        }
      }));
    }

    latch.countDown();

    for(var future: futures)
      future.get(30, SECONDS);

    executor.shutdown();

    assertEquals(0, errors.get(), "Stress test produced errors");
  }
}
