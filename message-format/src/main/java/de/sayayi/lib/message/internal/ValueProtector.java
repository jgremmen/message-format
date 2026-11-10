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

import de.sayayi.lib.message.util.SupplierDelegate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.function.*;


/**
 * Utility class which protects parameter values from being evaluated more than once.
 * <p>
 * Supplier parameter values ({@link Supplier}, {@link BooleanSupplier}, {@link IntSupplier}, {@link LongSupplier} and
 * {@link DoubleSupplier}) are evaluated by their formatter each time the value is required, such as for every map key
 * comparison and for the actual formatting. An expensive or non-idempotent supplier (e.g. a counter, a clock or a
 * random value) would therefore be invoked several times while formatting a single message and the selected map entry
 * could disagree with the formatted value.
 * <p>
 * This class wraps such suppliers in a caching delegate, based on {@link SupplierDelegate}, which invokes the original
 * supplier at most once and returns the cached value on subsequent calls. All other values are returned unchanged.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@ApiStatus.Internal
final class ValueProtector
{
  /** This class is not meant to be instantiated. */
  private ValueProtector() {
  }


  /**
   * Protects the given parameter {@code value} from being evaluated more than once.
   * <p>
   * If {@code value} is a {@link Supplier}, {@link BooleanSupplier}, {@link IntSupplier}, {@link LongSupplier} or
   * {@link DoubleSupplier}, a caching delegate of the same functional interface type is returned. The delegate invokes
   * {@code value} on its first evaluation only and returns the cached result from then on. If the supplier throws an
   * exception, no result is cached and the supplier is invoked again on the next evaluation.
   * <p>
   * Values which are already protected are returned as is, so protecting a value twice does not stack delegates. All
   * other values, including {@code null}, are returned unchanged.
   * <p>
   * The returned delegate implements the matching supplier interface only. If {@code value} implements more than one
   * supplier interface, the first matching interface in the order listed above is used. Any other interface or type
   * implemented by {@code value} is not visible on the returned delegate. As a consequence, a parameter formatter
   * registered for the specific type of {@code value} (e.g. {@code MySupplier implements LongSupplier}) never becomes a
   * formatter candidate for the returned delegate.
   *
   * @param value  parameter value to protect, or {@code null}
   *
   * @return  caching supplier delegate if {@code value} is a supplier, otherwise {@code value} itself
   */
  @Contract(pure = true)
  static Object protect(Object value)
  {
    if (value == null)
      return null;

    return switch(value) {
      case Supplier<?> supplier when !(value instanceof SupplierDelegate<?>) ->
          SupplierDelegate.of(supplier);

      case BooleanSupplier booleanSupplier when !(value instanceof BooleanSupplierDelegate) ->
          new BooleanSupplierDelegate(booleanSupplier);

      case IntSupplier intSupplier when !(value instanceof IntSupplierDelegate) ->
          new IntSupplierDelegate(intSupplier);

      case LongSupplier longSupplier when !(value instanceof LongSupplierDelegate) ->
          new LongSupplierDelegate(longSupplier);

      case DoubleSupplier doubleSupplier when !(value instanceof DoubleSupplierDelegate) ->
          new DoubleSupplierDelegate(doubleSupplier);

      default -> value;
    };
  }




  /**
   * Boolean supplier which invokes the delegate boolean supplier only once and caches the supplied value.
   * <p>
   * This class is thread-safe.
   *
   * @see SupplierDelegate
   */
  @SuppressWarnings("ClassCanBeRecord")
  private static class BooleanSupplierDelegate implements BooleanSupplier
  {
    /** Caching delegate for the boolean supplier. */
    private final Supplier<Boolean> delegate;


    /**
     * Creates a new caching delegate for the given boolean supplier.
     *
     * @param booleanSupplier  boolean supplier to delegate to, not {@code null}
     */
    private BooleanSupplierDelegate(BooleanSupplier booleanSupplier) {
      delegate = SupplierDelegate.of(booleanSupplier::getAsBoolean);
    }


    /**
     * Returns the cached value, invoking the delegate boolean supplier on the first call.
     *
     * @return  supplied boolean value
     */
    @Override
    public boolean getAsBoolean() {
      return delegate.get();
    }
  }




  /**
   * Int supplier which invokes the delegate int supplier only once and caches the supplied value.
   * <p>
   * This class is thread-safe.
   *
   * @see SupplierDelegate
   */
  @SuppressWarnings("ClassCanBeRecord")
  private static class IntSupplierDelegate implements IntSupplier
  {
    /** Caching delegate for the int supplier. */
    private final Supplier<Integer> delegate;


    /**
     * Creates a new caching delegate for the given int supplier.
     *
     * @param intSupplier  int supplier to delegate to, not {@code null}
     */
    private IntSupplierDelegate(IntSupplier intSupplier) {
      delegate = SupplierDelegate.of(intSupplier::getAsInt);
    }


    /**
     * Returns the cached value, invoking the delegate int supplier on the first call.
     *
     * @return  supplied int value
     */
    @Override
    public int getAsInt() {
      return delegate.get();
    }
  }




  /**
   * Long supplier which invokes the delegate long supplier only once and caches the supplied value.
   * <p>
   * This class is thread-safe.
   *
   * @see SupplierDelegate
   */
  @SuppressWarnings("ClassCanBeRecord")
  private static class LongSupplierDelegate implements LongSupplier
  {
    /** Caching delegate for the long supplier. */
    private final Supplier<Long> delegate;


    /**
     * Creates a new caching delegate for the given long supplier.
     *
     * @param longSupplier  long supplier to delegate to, not {@code null}
     */
    private LongSupplierDelegate(LongSupplier longSupplier) {
      delegate = SupplierDelegate.of(longSupplier::getAsLong);
    }


    /**
     * Returns the cached value, invoking the delegate long supplier on the first call.
     *
     * @return  supplied long value
     */
    @Override
    public long getAsLong() {
      return delegate.get();
    }
  }




  /**
   * Double supplier which invokes the delegate double supplier only once and caches the supplied value.
   * <p>
   * This class is thread-safe.
   *
   * @see SupplierDelegate
   */
  @SuppressWarnings("ClassCanBeRecord")
  private static class DoubleSupplierDelegate implements DoubleSupplier
  {
    /** Caching delegate for the double supplier. */
    private final Supplier<Double> delegate;


    /**
     * Creates a new caching delegate for the given double supplier.
     *
     * @param doubleSupplier  double supplier to delegate to, not {@code null}
     */
    private DoubleSupplierDelegate(DoubleSupplier doubleSupplier) {
      delegate = SupplierDelegate.of(doubleSupplier::getAsDouble);
    }


    /**
     * Returns the cached value, invoking the delegate double supplier on the first call.
     *
     * @return  supplied double value
     */
    @Override
    public double getAsDouble() {
      return delegate.get();
    }
  }
}
