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
package de.sayayi.lib.message.exception;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;


/**
 * Duplicate message exception. This exception is thrown by the default message handler if a message
 * with the same code is published twice.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 *
 * @see de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport#setMessageHandler(Predicate)
 *      ConfigurableMessageSupport#setMessageHandler(Predicate)
 */
public final class DuplicateMessageException extends MessageException
{
  /** Duplicate message code. */
  private final String code;


  public DuplicateMessageException(@NotNull String code, String message) {
    this(code, message, null);
  }


  public DuplicateMessageException(@NotNull String code, String message, Throwable cause)
  {
    super(message, cause);

    this.code = code;
  }


  /**
   * Returns the message code which has been identified as being a duplicate.
   *
   * @return  duplicate message code, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull String getCode() {
    return code;
  }
}
