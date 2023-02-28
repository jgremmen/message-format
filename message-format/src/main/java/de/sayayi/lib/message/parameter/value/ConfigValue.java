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
package de.sayayi.lib.message.parameter.value;

import de.sayayi.lib.message.parameter.ParamConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;


/**
 * Interface representing a typed value in a data map.
 *
 * @author Jeroen Gremmen
 *
 * @see ParamConfig
 */
public interface ConfigValue extends Serializable
{
  /** Config value types {@code string} and {@code message}. */
  Set<Type> STRING_MESSAGE_TYPE = unmodifiableSet(EnumSet.of(Type.STRING, Type.MESSAGE));

  /** Config value type {@code string}. */
  Set<Type> STRING_TYPE = unmodifiableSet(EnumSet.of(Type.STRING));

  /** Config value type {@code bool}. */
  Set<Type> BOOL_TYPE = unmodifiableSet(EnumSet.of(Type.BOOL));

  /** Config value type {@code number}. */
  Set<Type> NUMBER_TYPE = unmodifiableSet(EnumSet.of(Type.NUMBER));


  /**
   * Return the config value type.
   *
   * @return  config value type, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Type getType();


  /**
   * Returns the underlying raw data object.
   *
   * @return  raw data object, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Serializable asObject();




  /**
   * Type constants for map values.
   */
  enum Type {
    STRING, NUMBER, BOOL, MESSAGE
  }
}
