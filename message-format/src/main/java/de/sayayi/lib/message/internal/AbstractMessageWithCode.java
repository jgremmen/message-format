/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
abstract class AbstractMessageWithCode implements Message.WithCode
{
  /**
   * Unique message code, never {@code null} nor empty.
   */
  protected final @NotNull String code;


  AbstractMessageWithCode(@NotNull String code)
  {
    if (requireNonNull(code, "message code must not be null").isEmpty())
      throw new IllegalArgumentException("message code must not be empty");

    this.code = code;
  }


  @Override
  public @NotNull String getCode() {
    return code;
  }


  @Override
  public int hashCode() {
    return code.hashCode();
  }


  @Override
  public boolean equals(Object o)
  {
    return this == o ||
        (o instanceof AbstractMessageWithCode && code.equals(((AbstractMessageWithCode)o).code));
  }
}
