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
package de.sayayi.lib.message.part.parameter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalLong;


/**
 * This interface provides access to the parameter configuration.
 *
 * @since 0.8.4
 */
public interface ParameterConfigAccessor
{
  /**
   * Gets a configuration value for named key {@code name}.
   * <p>
   * The value is taken from the parameter configuration map. If no such key is found the
   * message support is queried for a default configuration value.
   *
   * @param name  parameter configuration key, not {@code null}
   *
   * @return  optional instance representing the found value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Optional<ConfigValue> getConfigValue(@NotNull String name);


  /**
   * Gets a string configuration value for named key {@code name}.
   * <p>
   * The value is taken from the parameter configuration map. If no such key is found the
   * message support is queried for a default configuration value.
   * <p>
   * If a value is found for the given {@code name} but the type is not a string, the method
   * returns {@link Optional#empty()}.
   *
   * @param name  parameter configuration key, not {@code null}
   *
   * @return  optional string instance representing the found value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Optional<String> getConfigValueString(@NotNull String name);


  /**
   * Gets a number configuration value for named key {@code name}.
   * <p>
   * The value is taken from the parameter configuration map. If no such key is found the
   * message support is queried for a default configuration value.
   * <p>
   * If a value is found for the given {@code name} but the type is not a number, the method
   * returns {@link Optional#empty()}.
   *
   * @param name  parameter configuration key, not {@code null}
   *
   * @return  optional number instance representing the found value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull OptionalLong getConfigValueNumber(@NotNull String name);


  /**
   * Gets a boolean configuration value for named key {@code name}.
   * <p>
   * The value is taken from the parameter configuration map. If no such key is found the
   * message support is queried for a default configuration value.
   * <p>
   * If a value is found for the given {@code name} but the type is not boolean, the method
   * returns {@link Optional#empty()}.
   *
   * @param name  parameter configuration key, not {@code null}
   *
   * @return  optional boolean instance representing the found value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Optional<Boolean> getConfigValueBool(@NotNull String name);


  /**
   * Gets a message configuration value for named key {@code name}.
   * <p>
   * The value is taken from the parameter configuration map. If no such key is found the
   * message support is queried for a default configuration value.
   * <p>
   * If a value is found for the given {@code name} but the type is not a string nor a message,
   * the method returns {@link Optional#empty()}.
   *
   * @param name  parameter configuration key, not {@code null}
   *
   * @return  optional message instance representing the found value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Optional<Message.WithSpaces> getConfigValueMessage(@NotNull String name);
}
