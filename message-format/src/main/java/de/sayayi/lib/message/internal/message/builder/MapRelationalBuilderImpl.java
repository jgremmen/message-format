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
package de.sayayi.lib.message.internal.message.builder;

import de.sayayi.lib.message.MessageBuilder.MapRelationalBuilder;
import de.sayayi.lib.message.MessageBuilder.MapValueBuilder;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MapKey.CompareType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static de.sayayi.lib.message.part.MapKey.CompareType.*;


/**
 * Default implementation of {@link MapRelationalBuilder}.
 * <p>
 * Builds a map entry that matches a numeric or string value using a relational comparison.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@ApiStatus.Internal
public final class MapRelationalBuilderImpl extends MapEqualityBuilderImpl implements MapRelationalBuilder
{
  /**
   * Construct a new relational map entry builder.
   *
   * @param parameterBuilder  the enclosing parameter builder, not {@code null}
   * @param keyFactory        factory that creates a {@link MapKey} for the given comparison type, not {@code null}
   */
  MapRelationalBuilderImpl(@NotNull ParameterBuilderImpl parameterBuilder,
                           @NotNull Function<CompareType,MapKey> keyFactory) {
    super(parameterBuilder, keyFactory);
  }


  /**
   * Selects a less-than comparison for the map entry.
   *
   * @return  builder for supplying the entry value, never {@code null}
   */
  @Override
  public @NotNull MapValueBuilder lt()
  {
    compareType = LT;

    return this;
  }


  /**
   * Selects a less-than-or-equal comparison for the map entry.
   *
   * @return  builder for supplying the entry value, never {@code null}
   */
  @Override
  public @NotNull MapValueBuilder lte()
  {
    compareType = LTE;

    return this;
  }


  /**
   * Selects a greater-than comparison for the map entry.
   *
   * @return  builder for supplying the entry value, never {@code null}
   */
  @Override
  public @NotNull MapValueBuilder gt()
  {
    compareType = GT;

    return this;
  }


  /**
   * Selects a greater-than-or-equal comparison for the map entry.
   *
   * @return  builder for supplying the entry value, never {@code null}
   */
  @Override
  public @NotNull MapValueBuilder gte()
  {
    compareType = GTE;

    return this;
  }
}
