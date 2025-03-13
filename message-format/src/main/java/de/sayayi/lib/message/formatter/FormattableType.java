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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;


/**
 * A formattable type is an ordered type (class). The order is explicitly defined within a range
 * {@code 0..127}.
 * <p>
 * During formatting there may be multiple suitable formatters for a specific value. The order
 * determines in what order the formatters are presented. The best match (lowest order) will be
 * used to format the value. However, it can decide to delegate formatting to the next formatter.
 * <p>
 * All formatters bundled with the message format library (except for the Object and byte[]
 * formatter) have either a {@link #DEFAULT_ORDER} or {@link #DEFAULT_PRIMITIVE_OR_ARRAY_ORDER}.
 *
 * @see GenericFormatterService
 * @see FormatterContext#delegateToNextFormatter()
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class FormattableType implements Comparable<FormattableType>, Serializable
{
  /**
   * Default order value. If a formattable type has no explicit order, this default value will be
   * used instead.
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
   * Constructs a formattable type with a specific {@code order}.
   * <p>
   * Note: {@code Object} type must be fixed at order 127. If a lower order number is provided
   *       for this type an {@code IllegalArgumentException} is thrown.
   *
   * @param type   type, not {@code null}
   * @param order  order ({@code 0..127})
   */
  public FormattableType(@NotNull Class<?> type, int order)
  {
    if (type == Object.class && order != 127)
      throw new IllegalArgumentException("Object type order must be 127");
    else if (order < 0 || order >= 128)
      throw new IllegalArgumentException("order must be in range 0..127");

    this.type = requireNonNull(type, "type must not be null");
    this.order = order;
  }


  /**
   * Constructs a formattable type with default order.
   *
   * @param type  type, not {@code null}
   *
   * @see #DEFAULT_ORDER
   * @see #DEFAULT_PRIMITIVE_OR_ARRAY_ORDER
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
  public int getOrder() {
    return order;
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof FormattableType && type == ((FormattableType)o).type;
  }


  @Override
  public int hashCode() {
    return type.hashCode();
  }


  @Override
  public int compareTo(@NotNull FormattableType o)
  {
    int cmp = Integer.compare(order, o.order);
    if (cmp == 0)
    {
      // make comparison deterministic if order values are equal
      cmp = type.getName().compareTo(o.type.getName());
    }

    return cmp;
  }


  @Override
  public String toString() {
    return "FormattableType(type=" + type + ",order=" + order + ')';
  }
}
