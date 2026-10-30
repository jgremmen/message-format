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

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link SupplierDelegate}: caching of values including {@code null}, retry after exceptions, concurrent calls
 * and recursive initialization.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Supplier delegate")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("SameParameterValue")
final class SupplierDelegateTest
{
  @Test
  @DisplayName("Delegate is invoked once and its value is cached")
  void testValueCached()
  {
    val calls = new AtomicInteger();
    val supplier = SupplierDelegate.of(() -> new Object[] { calls.incrementAndGet() });
    val value = supplier.get();

    assertSame(value, supplier.get());
    assertSame(value, supplier.get());
    assertEquals(1, calls.get());
  }


  @Test
  @DisplayName("Null value is cached")
  void testNullValueCached()
  {
    val calls = new AtomicInteger();
    val supplier = SupplierDelegate.of(() -> {
      calls.incrementAndGet();
      return null;
    });

    assertNull(supplier.get());
    assertNull(supplier.get());
    assertEquals(1, calls.get());
  }


  @Test
  @DisplayName("Delegate is invoked again after an exception")
  void testRetryAfterException()
  {
    val calls = new AtomicInteger();
    val supplier = SupplierDelegate.of(() -> {
      if (calls.incrementAndGet() == 1)
        throw new IllegalStateException("first call fails");

      return "value";
    });

    assertEquals("first call fails", assertThrows(IllegalStateException.class, supplier::get).getMessage());
    assertEquals("value", supplier.get());
    assertEquals("value", supplier.get());
    assertEquals(2, calls.get());
  }


  @Test
  @DisplayName("Null delegate is rejected")
  @SuppressWarnings("DataFlowIssue")
  void testNullDelegate() {
    assertThrows(NullPointerException.class, () -> SupplierDelegate.of(null));
  }


  @Test
  @DisplayName("Concurrent first calls invoke the delegate once")
  void testConcurrentFirstCalls() throws Exception
  {
    val threads = 8;

    for(var round = 0; round < 50; round++)
    {
      val calls = new AtomicInteger();
      val supplier = SupplierDelegate.of(() -> {
        calls.incrementAndGet();
        Thread.yield();
        return new Object();
      });

      val values = getConcurrently(threads, supplier);

      assertEquals(1, calls.get());
      for(val value: values)
        assertSame(values.getFirst(), value);
    }
  }


  @Test
  @DisplayName("Concurrent calls after an exception invoke the delegate until it succeeds")
  void testConcurrentCallsAfterException() throws Exception
  {
    val threads = 8;
    val calls = new AtomicInteger();
    val value = new Object();
    val supplier = SupplierDelegate.of(() -> {
      if (calls.incrementAndGet() <= 3)
        throw new IllegalStateException();

      return value;
    });

    for(var n = 0; n < 3; n++)
      assertThrows(IllegalStateException.class, supplier::get);

    for(val v: getConcurrently(threads, supplier))
      assertSame(value, v);

    assertEquals(4, calls.get());
  }


  @Test
  @DisplayName("Recursive call from the delegate fails instead of recursing endlessly")
  void testRecursiveCall()
  {
    val calls = new AtomicInteger();
    val self = new AtomicReference<Supplier<Object>>();

    // without detection, the re-entrant lock lets the delegate run again and again until the stack overflows
    self.set(SupplierDelegate.of(() -> {
      calls.incrementAndGet();
      return self.get().get();
    }));

    assertEquals("recursive SupplierDelegate initialization",
        assertThrows(IllegalStateException.class, self.get()::get).getMessage());
    assertEquals(1, calls.get());
  }


  @Test
  @DisplayName("Delegate is invoked again after a recursive call failed")
  void testRetryAfterRecursiveCall()
  {
    val calls = new AtomicInteger();
    val recurse = new AtomicBoolean(true);
    val self = new AtomicReference<Supplier<Object>>();
    val value = new Object();

    self.set(SupplierDelegate.of(() -> {
      calls.incrementAndGet();
      return recurse.get() ? self.get().get() : value;
    }));

    assertThrows(IllegalStateException.class, self.get()::get);

    recurse.set(false);

    assertSame(value, self.get().get());
    assertSame(value, self.get().get());
    assertEquals(2, calls.get());
  }


  @Test
  @DisplayName("Caught recursive call does not invoke the delegate twice")
  void testCaughtRecursiveCall()
  {
    val calls = new AtomicInteger();
    val self = new AtomicReference<Supplier<Object>>();
    val innerValue = new AtomicReference<>();

    // the delegate re-enters only once; without detection, the nested invocation caches a value which is then
    // overwritten by the outer invocation, so callers see two different values
    self.set(SupplierDelegate.of(() -> {
      if (calls.incrementAndGet() == 1)
      {
        try {
          innerValue.set(self.get().get());
        } catch(IllegalStateException ignored) {
        }
      }

      return new Object();
    }));

    val value = self.get().get();

    assertNull(innerValue.get());
    assertSame(value, self.get().get());
    assertEquals(1, calls.get());
  }


  private static <T> @NotNull List<T> getConcurrently(int threads, @NotNull Supplier<T> supplier) throws Exception
  {
    val start = new CountDownLatch(1);
    val tasks = new ArrayList<Callable<T>>();

    for(var t = 0; t < threads; t++)
    {
      tasks.add(() -> {
        start.await();
        return supplier.get();
      });
    }

    try(val executor = Executors.newFixedThreadPool(threads)) {
      val futures = tasks.stream().map(executor::submit).toList();
      start.countDown();

      val values = new ArrayList<T>();
      for(val future: futures)
        values.add(future.get(30, SECONDS));

      return values;
    }
  }
}
