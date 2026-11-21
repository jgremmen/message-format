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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;


/**
 * Supplier parsing a message text without locking on first use and caching the parsed message.
 * <p>
 * Concurrent first calls may parse the message text more than once. This is harmless, as each call yields an
 * equivalent message.
 *
 * @author Jeroen Gremmen
 * @since 0.25.1
 */
@Internal
final class ParsedMessageSupplier implements Supplier<Message>
{
  /** Factory used for parsing the message text. */
  private final @NotNull MessageFactory messageFactory;

  /** Message text to parse. */
  private final @Language("MessageFormat") String text;

  /** Parsed message, or {@code null} if the message text has not been parsed yet. */
  private volatile Message message;


  /**
   * Creates a supplier for the given message text.
   *
   * @param messageFactory  factory used for parsing the message text, not {@code null}
   * @param text            message text to parse
   */
  ParsedMessageSupplier(@NotNull MessageFactory messageFactory, @Language("MessageFormat") String text)
  {
    this.messageFactory = messageFactory;
    this.text = text;
  }


  /**
   * Returns the parsed message, parsing the message text on the first call.
   *
   * @return  parsed message, never {@code null}
   */
  @Override
  public @NotNull Message get()
  {
    var parsedMessage = message;
    if (parsedMessage == null)
      message = parsedMessage = messageFactory.parseMessage(text);

    return parsedMessage;
  }


  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "ParsedMessageSupplier(text=" + text + ",parsed=" + (message != null) + ')';
  }
}
