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

import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessageFilter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * Duplicate message exception. This exception is thrown by the default message handler if a message with the same code
 * is published twice.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 *
 * @see ConfigurableMessageSupport#setMessageFilter(MessageFilter)
 */
public final class DuplicateMessageException extends MessageException
{
  /** Duplicate message code. */
  private final String code;


  /**
   * Constructs a new duplicate message exception for the given message {@code code}.
   *
   * @param code     duplicate message code, not {@code null}
   * @param message  the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
   *                 method.
   */
  public DuplicateMessageException(@NotNull String code, String message) {
    this(code, message, null);
  }


  /**
   * Constructs a new duplicate message exception for the given message {@code code} and cause.
   *
   * @param code     duplicate message code, not {@code null}
   * @param message  the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
   * @param cause    the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null}
   *                 value is permitted and indicates that the cause is nonexistent or unknown.)
   */
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
