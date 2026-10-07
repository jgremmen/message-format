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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageBuilder;
import de.sayayi.lib.message.MessageBuilder.MapEqualityBuilder;
import de.sayayi.lib.message.MessageBuilder.MapValueBuilder;
import de.sayayi.lib.message.MessageBuilder.ParameterBuilder;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MapKey.CompareType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

import static de.sayayi.lib.message.part.MapKey.CompareType.EQ;
import static de.sayayi.lib.message.part.MapKey.CompareType.NE;


/**
 * Default implementation of {@link MapEqualityBuilder}.
 * <p>
 * Builds a map entry that matches a value using equality or inequality.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@ApiStatus.Internal
public non-sealed class MapEqualityBuilderImpl implements MapEqualityBuilder
{
  /** Parameter builder that receives the completed map entry. */
  private final @NotNull ParameterBuilderImpl parameterBuilder;

  /** Factory that turns the selected comparison type into a concrete map key. */
  private final @NotNull Function<CompareType,MapKey> keyFactory;

  /** Comparison type currently selected for the entry being built. */
  protected @NotNull CompareType compareType;


  /**
   * Construct a new equality map entry builder.
   *
   * @param parameterBuilder  the enclosing parameter builder, not {@code null}
   * @param keyFactory        factory that creates a {@link MapKey} for the given comparison type, not {@code null}
   */
  MapEqualityBuilderImpl(@NotNull ParameterBuilderImpl parameterBuilder,
                         @NotNull Function<CompareType,MapKey> keyFactory)
  {
    this.parameterBuilder = parameterBuilder;
    this.keyFactory = keyFactory;
    this.compareType = EQ;
  }


  /**
   * Selects equality comparison for the map entry.
   *
   * @return  builder for supplying the entry value, never {@code null}
   */
  @Override
  public @NotNull MapValueBuilder eq()
  {
    compareType = EQ;

    return this;
  }


  /**
   * Selects inequality comparison for the map entry.
   *
   * @return  builder for supplying the entry value, never {@code null}
   */
  @Override
  public @NotNull MapValueBuilder ne()
  {
    compareType = NE;

    return this;
  }


  /**
   * Completes the map entry with a pre-built message value.
   *
   * @param message  message value for the entry, not {@code null}
   *
   * @return  parameter builder that owns the map, never {@code null}
   */
  @Override
  public @NotNull ParameterBuilder message(@NotNull Message.WithSpaces message) {
    return parameterBuilder.addMapEntry(keyFactory.apply(compareType), message);
  }


  /**
   * Completes the map entry with a value parsed from a message-format string.
   *
   * @param message  message-format string for the entry value, not {@code null}
   *
   * @return  parameter builder that owns the map, never {@code null}
   */
  @Override
  public @NotNull ParameterBuilder message(@NotNull String message) {
    return parameterBuilder.addMapEntry(keyFactory.apply(compareType), message);
  }


  /**
   * Completes the map entry by configuring a nested message builder.
   *
   * @param messageConfigurer  callback that configures the nested message, not {@code null}
   *
   * @return  parameter builder that owns the map, never {@code null}
   */
  @Override
  public @NotNull ParameterBuilder message(@NotNull Consumer<MessageBuilder> messageConfigurer) {
    return parameterBuilder.addMapEntry(keyFactory.apply(compareType), messageConfigurer);
  }
}
