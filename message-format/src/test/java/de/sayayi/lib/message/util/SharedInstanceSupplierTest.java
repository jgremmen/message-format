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

import org.junit.jupiter.api.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static de.sayayi.lib.message.util.ConcurrentTestSupport.getConcurrently;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link SharedInstanceSupplier}: lazy creation, recursion detection, failing creation and concurrent first
 * access.
 *
 * @author Jeroen Gremmen
 * @since 0.26.0
 */
@DisplayName("Shared instance")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class SharedInstanceSupplierTest
{
  @Test
  @DisplayName("Instance is created on first access only")
  void createdOnce()
  {
    final var created = new AtomicInteger();
    final var shared = new SharedInstanceSupplier<>(() -> "instance-" + created.incrementAndGet());

    assertEquals(0, created.get());
    assertEquals("instance-1", shared.get());
    assertEquals("instance-1", shared.get());
    assertEquals(1, created.get());
  }


  @Test
  @DisplayName("Recursive access on the same thread is rejected")
  void recursiveAccess()
  {
    final var holder = new AtomicReference<SharedInstanceSupplier<String>>();
    holder.set(new SharedInstanceSupplier<>(() -> holder.get().get()));

    final var ex = assertThrows(IllegalStateException.class, () -> holder.get().get());

    assertEquals("recursive shared instance initialization", ex.getMessage());
  }


  @Test
  @DisplayName("Failing creation is retried on the next access")
  void failingCreation()
  {
    final var attempts = new AtomicInteger();
    final var shared = new SharedInstanceSupplier<>(() -> {
      if (attempts.incrementAndGet() == 1)
        throw new IllegalArgumentException("first attempt fails");

      return "instance";
    });

    assertThrows(IllegalArgumentException.class, shared::get);
    assertEquals("instance", shared.get());
    assertEquals(2, attempts.get());
  }


  @Test
  @DisplayName("Factory returning null is rejected")
  void nullInstance()
  {
    final var shared = new SharedInstanceSupplier<String>(() -> null);

    assertThrows(NullPointerException.class, shared::get);
  }


  @RepeatedTest(5)
  @DisplayName("Concurrent first access returns the same instance to all threads")
  void concurrentFirstAccess() throws Exception
  {
    final var shared = new SharedInstanceSupplier<>(Object::new);
    final var instances = getConcurrently(16, thread -> shared.get());

    for(var instance: instances)
      assertSame(shared.get(), instance);
  }
}
