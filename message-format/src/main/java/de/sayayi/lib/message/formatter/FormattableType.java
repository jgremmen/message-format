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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;


/**
 * Describes a type that a parameter formatter can handle and the priority of that type during formatter selection.
 * The order is explicitly defined within a range {@code 0..127}, where lower values have higher priority.
 * <p>
 * When multiple formatters can handle a value, the formatter for the highest priority matching type is tried first.
 * It may delegate formatting to the next matching formatter.
 * <p>
 * All formatters bundled with the message format library (except for the Object and byte[] formatter) have either a
 * {@link #DEFAULT_ORDER} or {@link #DEFAULT_PRIMITIVE_OR_ARRAY_ORDER}.
 *
 * @see GenericFormatterService
 * @see ParameterFormatterContext#delegateToNextFormatter()
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class FormattableType implements Comparable<FormattableType>, Serializable
{
  /**
   * Default formattable type for {@link Object}, which can match values of any type.
   */
  public static final FormattableType DEFAULT = new FormattableType(Object.class);


  /**
   * Default order value. If a formattable type has no explicit order, this default value will be used instead.
   *
   * @see #getOrder()
   */
  public static final int DEFAULT_ORDER = 80;

  /**
   * Default order value for primitive and array types.
   *
   * @see #getOrder()
   */
  public static final int DEFAULT_PRIMITIVE_OR_ARRAY_ORDER = 100;


  /** Formattable class. */
  private final @NotNull Class<?> type;

  /** Formattable type order. */
  private final int order;


  /**
   * Creates a formattable type with the specified priority order.
   * <p>
   * The {@link Object} type must use order {@code 127}, so it is considered only after more specific types.
   *
   * @param type   type handled by the formatter, not {@code null}
   * @param order  priority order in the range {@code 0..127}
   *
   * @throws IllegalArgumentException  if the order is outside the allowed range or if {@link Object} is given an order
   *                                   other than {@code 127}
   * @throws NullPointerException      if {@code type} is {@code null}
   */
  @SuppressWarnings("ConstantValue")
  public FormattableType(@NotNull Class<?> type, @Range(from = 0, to = 127) int order)
  {
    if (type == Object.class && order != 127)
      throw new IllegalArgumentException("Object type order must be 127");
    else if (order < 0 || order >= 128)
      throw new IllegalArgumentException("order must be in range 0..127");

    this.type = requireNonNull(type, "type must not be null");
    this.order = order;
  }


  /**
   * Creates a formattable type with the default priority for its type.
   *
   * @param type  type handled by the formatter, not {@code null}
   *
   * @see #DEFAULT_ORDER
   * @see #DEFAULT_PRIMITIVE_OR_ARRAY_ORDER
   *
   * @throws NullPointerException  if {@code type} is {@code null}
   */
  public FormattableType(@NotNull Class<?> type)
  {
    this.type = requireNonNull(type, "type must not be null");

    order = type == Object.class
        ? 127
        : type.isPrimitive() || type.isArray()
            ? DEFAULT_PRIMITIVE_OR_ARRAY_ORDER
            : DEFAULT_ORDER;
  }


  /**
   * Returns the formattable type.
   *
   * @return  type, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Class<?> getType() {
    return type;
  }


  /**
   * Returns the order for this formattable type.
   *
   * @return  order in range {@code 0..127}
   */
  @Contract(pure = true)
  public @Range(from = 0, to = 127) int getOrder() {
    return order;
  }


  /**
   * Indicates whether this formattable type is equal to another object.
   * Two formattable types are equal when they describe the same class and priority order.
   *
   * @param o  object to compare with
   *
   * @return  {@code true} if the objects describe the same type and order
   */
  @Override
  public boolean equals(Object o) {
    return this == o || (o instanceof FormattableType that && type == that.type && order == that.order);
  }


  @Override
  public int hashCode() {
    return type.hashCode() * 31 + order;
  }


  /**
   * Compares this formattable type with another by priority order, then by type name when the orders are equal.
   *
   * @param o  formattable type to compare with, not {@code null}
   *
   * @return  a negative value, zero, or a positive value when this type sorts before, equally to, or after the given
   *          type
   */
  @Override
  public int compareTo(@NotNull FormattableType o)
  {
    var cmp = Integer.compare(order, o.order);
    if (cmp == 0)
    {
      // make comparison deterministic if order values are equal
      cmp = type.getName().compareTo(o.type.getName());
    }

    return cmp;
  }


  /**
   * Returns a readable representation of this formattable type.
   *
   * @return  a string containing the type and its priority order
   */
  @Override
  public String toString() {
    return "FormattableType(type=" + type + ",order=" + order + ')';
  }
}
