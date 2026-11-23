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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;


/**
 * Supplier of a lazily created instance, which is shared by all threads and created without holding a lock.
 * <p>
 * The instance is created by a factory on the first call to {@link #get()}. Unlike a supplier creating the instance
 * while holding a lock, such as {@link SupplierDelegate}, no thread waits for another thread creating the instance.
 * This prevents deadlocks if the factory runs code which waits for other threads. An example is a factory which
 * initializes a class, while another thread is initializing that class and calls this supplier from its static
 * initializer. With a lock, that thread would wait for the lock, while the thread holding the lock waits for the class
 * initialization to complete.
 * <p>
 * If several threads call {@link #get()} before an instance has been published, each of them creates an instance. The
 * instance published first is returned to all threads and the instances created by the other threads are discarded.
 * The factory must therefore tolerate being invoked more than once, and a discarded instance must not require any
 * cleanup. If the instance is expensive to create and its factory does not call code outside of your control, a
 * {@link SupplierDelegate} is the better choice.
 * <p>
 * If the factory throws an exception, no instance is published and the exception is relayed to the caller. The next
 * call to {@link #get()} invokes the factory again.
 * <p>
 * A call to {@link #get()} from the factory on the same thread, directly or indirectly, fails with an
 * {@link IllegalStateException} instead of recursing.
 * <p>
 * This class is thread-safe.
 *
 * @param <T>  instance type
 *
 * @author Jeroen Gremmen
 * @since 0.26.0
 */
public final class SharedInstanceSupplier<T> implements Supplier<@NotNull T>
{
  /** Factory creating the instance, not {@code null}. */
  private final @NotNull Supplier<T> factory;

  /** Published instance, or {@code null} if no instance has been published yet. */
  private final @NotNull AtomicReference<T> instance = new AtomicReference<>();

  /** Set while the current thread creates an instance, to detect recursive access. */
  private final @NotNull ThreadLocal<Boolean> creating = new ThreadLocal<>();


  /**
   * Creates a supplier for the instance created by {@code factory} on the first call to {@link #get()}.
   *
   * @param factory  factory creating the instance, not {@code null}. It must not return {@code null}
   */
  public SharedInstanceSupplier(@NotNull Supplier<T> factory) {
    this.factory = requireNonNull(factory, "factory must not be null");
  }


  /**
   * Returns the shared instance, creating it with the factory if no instance has been published yet.
   *
   * @return  shared instance, never {@code null}
   *
   * @throws IllegalStateException  if called by the factory on the same thread, directly or indirectly
   * @throws NullPointerException   if the factory returns {@code null}
   */
  @Override
  public @NotNull T get()
  {
    final var value = instance.get();
    return value != null ? value : create();
  }


  /**
   * Creates an instance and publishes it, unless another thread has published an instance in the meantime.
   *
   * @return  published instance, never {@code null}
   *
   * @throws IllegalStateException  if called by the factory on the same thread, directly or indirectly
   * @throws NullPointerException   if the factory returns {@code null}
   */
  private @NotNull T create()
  {
    if (creating.get() != null)
      throw new IllegalStateException("recursive shared instance initialization");

    creating.set(TRUE);
    try {
      final var created = requireNonNull(factory.get(), "factory must not return null");
      final var published = instance.compareAndExchange(null, created);

      return published != null ? published : created;
    } finally {
      creating.remove();
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "SharedInstanceSupplier(created=" + (instance.get() != null) + ')';
  }
}
