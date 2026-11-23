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

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;


/**
 * Lazily created shared instance, which is published without holding a lock while it is being created.
 * <p>
 * Shared instances, such as the shared formatter service, instantiate service providers while they are being created.
 * A provider's constructor or static initializer is code outside this library. If the shared instance were created
 * while holding a lock, a thread initializing a provider class, whose static initializer accesses the shared
 * instance, would wait for that lock, while the thread holding the lock waits for the class initialization to complete.
 * Without a lock, each thread creates the instance on its own and the instance published first is returned to all
 * threads. The instances created by the other threads are discarded.
 * <p>
 * Accessing the shared instance while it is being created on the same thread, e.g. from the constructor of a service
 * provider, fails with an {@link IllegalStateException} instead of recursing.
 * <p>
 * This class is thread-safe.
 *
 * @param <T>  instance type
 *
 * @author Jeroen Gremmen
 * @since 0.26.0
 */
@Internal
public final class SharedInstance<T>
{
  /** Factory creating the instance, not {@code null}. */
  private final @NotNull Supplier<T> factory;

  /** Published instance, or {@code null} if no instance has been published yet. */
  private final @NotNull AtomicReference<T> instance = new AtomicReference<>();

  /** Set while the current thread creates an instance, to detect recursive access. */
  private final @NotNull ThreadLocal<Boolean> creating = new ThreadLocal<>();


  /**
   * Creates a shared instance, which is created by {@code factory} on first access.
   *
   * @param factory  factory creating the instance, not {@code null}. It must not return {@code null}
   */
  public SharedInstance(@NotNull Supplier<T> factory) {
    this.factory = requireNonNull(factory, "factory must not be null");
  }


  /**
   * Returns the shared instance, creating it if no instance has been published yet.
   *
   * @return  shared instance, never {@code null}
   *
   * @throws IllegalStateException  if called while the current thread creates the instance
   */
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
   * @throws IllegalStateException  if called while the current thread creates the instance
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
    return "SharedInstance(created=" + (instance.get() != null) + ')';
  }
}
