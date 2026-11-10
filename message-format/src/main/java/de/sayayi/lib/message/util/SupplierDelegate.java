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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;


/**
 * This supplier implementation delegates to another supplier and caches the supplied value, thus
 * invoking the delegated supplier only once.
 * <p>
 * This class is thread-safe. The class of the delegate supplier is available through {@link #getDelegateClass()}, even
 * after the value has been obtained.
 *
 * <h2>Example</h2>
 * <pre>
 *   Supplier&lt;ExpensiveObject&gt; lazyObject =
 *       SupplierDelegate.of(() -&gt; new ExpensiveObject());
 *
 *   // the delegate supplier is invoked only on the first call
 *   ExpensiveObject obj1 = lazyObject.get();
 *
 *   // subsequent calls return the cached value
 *   ExpensiveObject obj2 = lazyObject.get();
 *
 *   assert obj1 == obj2;
 * </pre>
 *
 * @param <T>  supplier value type
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class SupplierDelegate<T> implements Supplier<T>
{
  /** Class of the delegate supplier, retained after the delegate supplier has been released. */
  private final Class<? extends Supplier<T>> delegateClass;

  /** Lock guarding the first invocation of the delegate supplier, or {@code null} once the value has been obtained. */
  private volatile ReentrantLock lock = new ReentrantLock();

  /** Delegate supplier, or {@code null} once the value has been obtained. */
  private volatile Supplier<T> supplier;

  /** Cached value obtained from the delegate supplier. */
  private volatile T value;


  /**
   * Creates a new supplier delegate for the given supplier.
   *
   * @param supplier  supplier to delegate to, not {@code null}
   */
  @SuppressWarnings("unchecked")
  private SupplierDelegate(@NotNull Supplier<T> supplier)
  {
    this.supplier = requireNonNull(supplier, "supplier must not be null");
    this.delegateClass = (Class<? extends Supplier<T>>)supplier.getClass();
  }


  /**
   * Returns the cached value, invoking the delegate supplier on the first call. Subsequent calls will return the cached
   * value without invoking the delegate again.
   * <p>
   * This method is thread-safe: concurrent first calls invoke the delegate supplier only once. If the delegate supplier
   * throws an exception, the exception is relayed to the caller and the delegate supplier is invoked again on the next
   * call.
   * <p>
   * If the delegate supplier calls this method on the same supplier delegate from the same thread, an
   * {@link IllegalStateException} is thrown instead of invoking the delegate supplier recursively.
   *
   * @return  the supplied value, which may be {@code null} depending on the delegate
   *
   * @throws IllegalStateException  if called recursively by the delegate supplier
   */
  @Override
  public T get()
  {
    if (supplier != null)
    {
      var _lock = lock;
      if (_lock != null)
      {
        _lock.lock();
        try {
          if (supplier != null)
          {
            // detect recursion
            if (_lock.getHoldCount() > 1)
              throw new IllegalStateException("recursive SupplierDelegate initialization");

            value = supplier.get();
            supplier = null;
            lock = null;
          }
        } finally {
          _lock.unlock();
        }
      }
    }

    return value;
  }


  /**
   * Returns the class of the supplier this instance delegates to.
   * <p>
   * The class remains available after the value has been obtained and the delegate supplier has been released. It
   * allows callers to identify the origin of the supplied value without invoking the delegate supplier.
   *
   * @return  delegate supplier class, never {@code null} and never {@code SupplierDelegate.class}
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  public @NotNull Class<? extends Supplier<T>> getDelegateClass() {
    return delegateClass;
  }


  /**
   * Returns a caching supplier delegate for the given {@code supplier}.
   * <p>
   * If {@code supplier} already is a {@code SupplierDelegate}, it is returned as is instead of being wrapped again.
   * This way, the delegate class reported by {@link #getDelegateClass()} is never a {@code SupplierDelegate}.
   *
   * @param supplier  supplier to delegate to, not {@code null}
   * @param <T>       supplier value type
   *
   * @return  supplier delegate instance, never {@code null}
   *
   * @throws NullPointerException  if {@code supplier} is {@code null}
   */
  @Contract(value = "_ -> !null", pure = true)
  public static <T> @NotNull Supplier<T> of(@NotNull Supplier<T> supplier) {
    return supplier instanceof SupplierDelegate<T> delegate ? delegate : new SupplierDelegate<>(supplier);
  }
}
