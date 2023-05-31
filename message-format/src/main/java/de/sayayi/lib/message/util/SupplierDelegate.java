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

import java.util.function.Supplier;


/**
 * This supplier implementation delegates to another supplier and caches the supplied value, thus
 * invoking the delegated supplier only once.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 *
 * @param <T>  supplier value type
 */
public final class SupplierDelegate<T> implements Supplier<T>
{
  private final Object $LOCK = new Object[0];
  private Supplier<T> supplier;
  private T value;


  private SupplierDelegate(@NotNull Supplier<T> supplier) {
    this.supplier = supplier;
  }


  @Override
  public T get()
  {
    synchronized($LOCK) {
      if (supplier != null)
      {
        value = supplier.get();
        supplier = null;
      }

      return value;
    }
  }


  /**
   * Creates a supplier delegate with the given {@code supplier}.
   *
   * @param supplier  supplier to delegate to, not {@code null}
   *
   * @return  supplier delegate instance, never {@code null}
   *
   * @param <T>  supplier value type
   */
  @Contract(value = "_ -> new", pure = true)
  public static <T> @NotNull Supplier<T> of(@NotNull Supplier<T> supplier) {
    return new SupplierDelegate<>(supplier);
  }
}
