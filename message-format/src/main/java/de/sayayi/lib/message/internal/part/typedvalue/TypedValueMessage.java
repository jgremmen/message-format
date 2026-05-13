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
package de.sayayi.lib.message.internal.part.typedvalue;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.internal.TextMessage;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TypedValue.MessageValue;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static de.sayayi.lib.message.util.MessageUtil.isName;
import static de.sayayi.lib.message.util.MessageUtil.serializeString;
import static java.util.Objects.requireNonNull;


/**
 * Internal implementation of {@link MessageValue} that wraps a {@link Message.WithSpaces} as a
 * typed configuration value. This record is used in parameter configurations and map entries
 * where the value is a message.
 *
 * @param messageValue  the wrapped message, not {@code null}
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public record TypedValueMessage(@NotNull Message.WithSpaces messageValue) implements MessageValue
{
  /**
   * Creates a new typed value message wrapping the given message.
   *
   * @param messageValue  the message to wrap, not {@code null}
   *
   * @throws NullPointerException  if {@code messageValue} is {@code null}
   */
  public TypedValueMessage(@NotNull Message.WithSpaces messageValue) {
    this.messageValue = requireNonNull(messageValue, "message must not be null");
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Message.WithSpaces asObject() {
    return messageValue();
  }


  /**
   * Serializes this message value into its format string representation using
   * {@link MessageUtil#serializeMessage(Context, Message, boolean)}.
   *
   * @param context  the serialization context, not {@code null}
   */
  @Override
  public void serialize(@NotNull Context context)
  {
    if (messageValue instanceof TextMessage textMessage)
    {
      final var string = ((Text)textMessage.getMessageParts()[0]).getTextWithSpaces();
      if (isName(string))
      {
        serializeString(context, string);
        return;
      }
    }

    context.textJoiner().add('\'');
    messageValue.serialize(context.withStringQuote('\''));
    context.textJoiner().add('\'');
  }


  /**
   * Returns the string representation of the wrapped message.
   *
   * @return  string representation, never {@code null}
   */
  @Override
  public @NotNull String toString() {
    return messageValue.toString();
  }


  /**
   * Writes this message value to the given pack output stream for binary serialization.
   *
   * @param packStream  data output pack target, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    PackSupport.pack(messageValue, packStream);
  }


  /**
   * Reads a {@code TypedValueMessage} from the given pack input stream.
   *
   * @param unpack      unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked message value, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull TypedValueMessage unpack(@NotNull PackSupport unpack, @NotNull PackInputStream packStream)
      throws IOException {
    return new TypedValueMessage(unpack.unpackMessageWithSpaces(packStream));
  }
}
