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
package de.sayayi.lib.message.util;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@FunctionalInterface
public interface BooleanConsumer
{
  /**
   * Performs this operation on the given argument.
   *
   * @param value the input argument
   */
  void accept(boolean value);


  /**
   * Returns a composed {@code BooleanConsumer} that performs, in sequence, this
   * operation followed by the {@code after} operation. If performing either
   * operation throws an exception, it is relayed to the caller of the
   * composed operation. If performing this operation throws an exception,
   * the {@code after} operation will not be performed.
   *
   * @param   after the operation to perform after this operation
   *
   * @return  a composed {@code BooleanConsumer} that performs in sequence this
   *          operation followed by the {@code after} operation
   *
   * @throws NullPointerException if {@code after} is null
   */
  default @NotNull BooleanConsumer andThen(@NotNull BooleanConsumer after)
  {
    requireNonNull(after);

    return (boolean t) -> {
      accept(t);
      after.accept(t);
    };
  }
}
