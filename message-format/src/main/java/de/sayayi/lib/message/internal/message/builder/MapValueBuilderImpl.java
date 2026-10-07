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
import de.sayayi.lib.message.MessageBuilder.MapValueBuilder;
import de.sayayi.lib.message.MessageBuilder.ParameterBuilder;
import de.sayayi.lib.message.part.MapKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;


/**
 * Default implementation of {@link MapValueBuilder}.
 * <p>
 * Builds the message value for a parameter's mapped entry, either for a specific key or for the default case.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@ApiStatus.Internal
public final class MapValueBuilderImpl implements MapValueBuilder
{
  /** Parameter builder that receives the completed map entry. */
  private final @NotNull ParameterBuilderImpl parameterBuilder;

  /** Key associated with the map entry, or {@code null} for the default entry. */
  private final MapKey key;


  /**
   * Construct a new map value builder.
   *
   * @param parameterBuilder  the enclosing parameter builder, not {@code null}
   * @param key               the map key, or {@code null} for the default entry
   */
  MapValueBuilderImpl(@NotNull ParameterBuilderImpl parameterBuilder, MapKey key)
  {
    this.parameterBuilder = parameterBuilder;
    this.key = key;
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
    return parameterBuilder.addMapEntry(key, message);
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
    return parameterBuilder.addMapEntry(key, message);
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
    return parameterBuilder.addMapEntry(key, messageConfigurer);
  }
}
