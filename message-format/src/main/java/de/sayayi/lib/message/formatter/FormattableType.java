/*
 * Copyright 2023 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@ToString
public final class FormattableType implements Comparable<FormattableType>, Serializable
{
  private static final long serialVersionUID = 800L;

  public static final byte DEFAULT_ORDER = 100;


  @EqualsAndHashCode.Include
  private final @NotNull Class<?> type;

  private final byte order;


  public FormattableType(@NotNull Class<?> type, @Range(from = 0, to = 127) int order)
  {
    this.type = requireNonNull(type);
    this.order = (byte)order;

    //noinspection ConstantValue
    if (order < 0 || order > 127)
      throw new IllegalArgumentException("order must be in range 0..127");
  }


  public FormattableType(@NotNull Class<?> type) {
    this(type, DEFAULT_ORDER);
  }


  @Contract(pure = true)
  public @NotNull Class<?> getType() {
    return type;
  }


  @Contract(pure = true)
  public @Range(from = 0, to = 127) int getOrder() {
    return order;
  }


  @Override
  public int compareTo(@NotNull FormattableType o)
  {
    int cmp = Byte.compare(order, o.order);
    if (cmp == 0)
      if ((cmp = type.getSimpleName().compareTo(o.type.getSimpleName())) == 0)
        cmp = type.getName().compareTo(o.type.getName());

    return cmp;
  }
}