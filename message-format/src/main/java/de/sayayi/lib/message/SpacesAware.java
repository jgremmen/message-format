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
package de.sayayi.lib.message;

import org.jetbrains.annotations.Contract;


/**
 * This interface denotes that a message is aware of leading and trailing spaces.
 *
 * @author Jeroen Gremmen
 * @since 0.5.0
 */
public interface SpacesAware
{
  /**
   * Tells whether this message has a leading space.
   *
   * @return  {@code true} this message has a leading space, {@code false} otherwise
   */
  @Contract(pure = true)
  boolean isSpaceBefore();


  /**
   * Tells whether this message has a trailing space.
   *
   * @return  {@code true} this message has a trailing space, {@code false} otherwise
   */
  @Contract(pure = true)
  boolean isSpaceAfter();


  /**
   * Tells whether this message has a leading and trailing space.
   *
   * @return  {@code true} this message has a leading and trailing space, {@code false} otherwise
   *
   * @since 0.8.0
   */
  @Contract(pure = true)
  default boolean isSpaceAround() {
    return isSpaceBefore() && isSpaceAfter();
  }
}
