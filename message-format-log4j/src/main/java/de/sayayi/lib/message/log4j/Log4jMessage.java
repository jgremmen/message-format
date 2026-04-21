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
package de.sayayi.lib.message.log4j;

import org.apache.logging.log4j.message.Message;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;


/**
 * Log4j {@link Message} implementation that delegates message formatting to a supplier.
 * This allows for lazy evaluation of the formatted message string, deferring the formatting work until the message
 * is actually needed.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
final class Log4jMessage implements Message
{
  private final Supplier<String> messageSupplier;
  private final Throwable throwable;


  /**
   * Creates a new Log4j message with the given message supplier and optional throwable.
   *
   * @param messageSupplier  supplier providing the formatted message string, not {@code null}
   * @param throwable        throwable associated with this log message, or {@code null}
   */
  Log4jMessage(@NotNull Supplier<String> messageSupplier, Throwable throwable)
  {
    this.messageSupplier = messageSupplier;
    this.throwable = throwable;
  }


  /**
   * {@inheritDoc}
   * <p>
   * The formatted message is obtained by invoking the message supplier.
   */
  @Override
  public String getFormattedMessage() {
    return messageSupplier.get();
  }


  /**
   * {@inheritDoc}
   * <p>
   * Always returns {@code null} as parameters are handled by the message supplier.
   */
  @Override
  public Object[] getParameters() {
    return null;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Throwable getThrowable() {
    return throwable;
  }
}
