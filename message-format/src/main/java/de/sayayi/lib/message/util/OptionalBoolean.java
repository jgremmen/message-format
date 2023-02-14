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

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static lombok.AccessLevel.PRIVATE;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@RequiredArgsConstructor(access = PRIVATE)
public final class OptionalBoolean
{
  private static final OptionalBoolean EMPTY = new OptionalBoolean(null);
  private static final OptionalBoolean TRUE = new OptionalBoolean(true);
  private static final OptionalBoolean FALSE = new OptionalBoolean(false);


  private final Boolean value;


  /**
   * Returns an empty {@code OptionalBoolean} instance. No value is present for this OptionalBoolean.
   *
   * @return an empty {@code OptionalBoolean}.
   */
  @Contract(pure = true)
  public static @NotNull OptionalBoolean empty() {
    return EMPTY;
  }


  /**
   * Return an {@code OptionalBoolean} with the specified value present.
   *
   * @param value the value to be present
   * @return an {@code OptionalBoolean} with the value present
   */
  @Contract(pure = true)
  public static @NotNull OptionalBoolean of(boolean value) {
    return value ? TRUE : FALSE;
  }


  /**
   * If a value is present in this {@code OptionalBoolean}, returns the value,
   * otherwise throws {@code NoSuchElementException}.
   *
   * @return the value held by this {@code OptionalBoolean}
   * @throws NoSuchElementException if there is no value present
   *
   * @see OptionalBoolean#isPresent()
   */
  @Contract(pure = true)
  public boolean getAsBoolean()
  {
    if (value == null)
      throw new NoSuchElementException("No value present");

    return value;
  }


  /**
   * Return {@code true} if there is a value present, otherwise {@code false}.
   *
   * @return {@code true} if there is a value present, otherwise {@code false}
   */
  @Contract(pure = true)
  public boolean isPresent() {
    return value != null;
  }


  /**
   * Have the specified consumer accept the value if a value is present,
   * otherwise do nothing.
   *
   * @param consumer block to be executed if a value is present
   * @throws NullPointerException if value is present and {@code consumer} is
   * null
   */
  public void ifPresent(@NotNull BooleanConsumer consumer)
  {
    if (value != null)
      consumer.accept(value);
  }


  /**
   * Return the value if present, otherwise return {@code other}.
   *
   * @param other the value to be returned if there is no value present
   * @return the value, if present, otherwise {@code other}
   */
  @Contract(pure = true)
  public boolean orElse(boolean other) {
    return value != null ? value : other;
  }


  /**
   * Return the value if present, otherwise invoke {@code other} and return
   * the result of that invocation.
   *
   * @param other a {@code BooleanSupplier} whose result is returned if no value is present
   *
   * @return the value if present otherwise the result of {@code other.getAsBoolean()}
   *
   * @throws NullPointerException if value is not present and {@code other} is null
   */
  public boolean orElseGet(@NotNull BooleanSupplier other) {
    return value != null ? value : other.getAsBoolean();
  }


  /**
   * Return the contained value, if present, otherwise throw an exception
   * to be created by the provided supplier.
   *
   * @param <X> Type of the exception to be thrown
   * @param exceptionSupplier The supplier which will return the exception to be thrown
   *
   * @return the present value
   *
   * @throws X if there is no value present
   * @throws NullPointerException if no value is present and {@code exceptionSupplier} is null
   */
  public <X extends Throwable> boolean orElseThrow(@NotNull Supplier<X> exceptionSupplier) throws X
  {
    if (value != null)
      return value;

    throw exceptionSupplier.get();
  }


  /**
   * Indicates whether some other object is "equal to" this OptionalLong. The
   * other object is considered equal if:
   * <ul>
   * <li>it is also an {@code OptionalBoolean} and;
   * <li>both instances have no value present or;
   * <li>the present values are "equal to" each other via {@code ==}.
   * </ul>
   *
   * @param obj an object to be tested for equality
   * @return {code true} if the other object is "equal to" this object otherwise {@code false}
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof OptionalBoolean && Objects.equals(value, ((OptionalBoolean)obj).value);
  }


  /**
   * Returns the hash code value of the present value, if any, or 0 (zero) if
   * no value is present.
   *
   * @return hash code value of the present value or 0 if no value is present
   */
  @Override
  public int hashCode() {
    return value != null ? Boolean.hashCode(value) : 0;
  }


  /**
   * {@inheritDoc}
   *
   * Returns a non-empty string representation of this object suitable for
   * debugging. The exact presentation format is unspecified and may vary
   * between implementations and versions.
   *
   * @return the string representation of this instance
   */
  @Override
  public String toString() {
    return value != null ? String.format("OptionalBoolean[%s]", value) : "OptionalBoolean.empty";
  }
}
