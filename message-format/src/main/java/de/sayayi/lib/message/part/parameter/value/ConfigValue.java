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
package de.sayayi.lib.message.part.parameter.value;

import de.sayayi.lib.message.part.parameter.ParameterConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * Interface representing a typed value in a parameter configuration map.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 *
 * @see ParameterConfig
 */
public interface ConfigValue
{
  /**
   * Return the config value type.
   *
   * @return  config value type, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Type getType();


  /**
   * Returns the underlying raw value object.
   *
   * @return  raw value object, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Object asObject();




  /**
   * Type constants for map values.
   */
  enum Type
  {
    /** String value type. */
    STRING,

    /** Number value type. */
    NUMBER,

    /** Boolean value type. */
    BOOL,

    /** Message value type. */
    MESSAGE
  }
}
