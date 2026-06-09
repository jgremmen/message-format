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

import de.sayayi.lib.message.FormatStringSerializer.Context;
import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextJoiner;
import de.sayayi.lib.message.template.Template;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * A {@link Template} implementation that wraps a {@link Message} and delegates formatting to it.
 * <p>
 * This is the standard template type used when templates are parsed from message format strings
 * or imported from pack files.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@SuppressWarnings("ClassCanBeRecord")
public final class MessageTemplate implements Template
{
  private final Message message;


  /**
   * Creates a new message template wrapping the given message.
   *
   * @param message  the message to wrap, not {@code null}
   */
  public MessageTemplate(@NotNull Message message) {
    this.message = message;
  }


  /**
   * Returns the wrapped message.
   *
   * @return  message, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Message getMessage() {
    return message;
  }


  /** {@inheritDoc} */
  @Contract(pure = true)
  public boolean isSame(@NotNull Template template)
  {
    return
        template instanceof MessageTemplate messageTemplate &&
        MessageFactory.isSame(message, messageTemplate.getMessage());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException {
    return message.formatAsText(messageAccessor, parameters);
  }


  /**
   * Returns a string representation of this template, useful for debugging.
   *
   * @return  string representation, never {@code null}
   */
  @Override
  public String toString()
  {
    final var textJoiner = new TextJoiner();

    message.serialize(new Context(UTF_8.newEncoder(), textJoiner, '"'));

    return "Template(\"" + textJoiner.asNoSpaceText().getText() + "\")";
  }
}
