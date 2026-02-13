/*
 * Copyright 2020 Jeroen Gremmen
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
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Locale;
import java.util.Set;


/**
 * This class represents the message parameter configuration map.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0
 */
public interface ParameterConfig
{
  /**
   * Tells whether the parameter configuration contains any values.
   *
   * @return  {@code true} if the parameter configuration contains at least 1 value,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  boolean isEmpty();


  /**
   * Tells whether the parameter configuration map contains a message entry with the given
   * {@code keyType}.
   *
   * @param keyType  entry key type to look for, not {@code null}
   *
   * @return  {@code true} if the map contains a message with the given key type,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  boolean hasMessageWithKeyType(@NotNull ConfigKey.Type keyType);


  /**
   * Returns a set of config names defined in the parameter configuration map.
   *
   * @return  unmodifiable set of config names, never {@code null}
   *
   * @since 0.20.0
   */
  @Contract(pure = true)
  @NotNull @Unmodifiable Set<String> getConfigNames();


  @Contract(pure = true)
  ConfigValue<?> getConfigValue(@NotNull String name);


  /**
   * Returns the default value from the parameter configuration map.

   * @return  default configuration value or {@code null} if no default value has been defined
   *
   * @since 0.20.0
   */
  @Contract(pure = true)
  ConfigValue<?> getDefaultValue();


  @Contract(pure = true)
  Message.WithSpaces getMessage(@NotNull MessageAccessor messageAccessor, Object key, @NotNull Locale locale,
                                @NotNull Set<ConfigKey.Type> keyTypes, boolean includeDefault);


  /**
   * Returns a set of template names referenced in all message values which are available in
   * the message parameter configuration.
   *
   * @return  unmodifiable set of all referenced template names, never {@code null}
   */
  @Contract(pure = true)
  @Unmodifiable
  @NotNull Set<String> getTemplateNames();
}
