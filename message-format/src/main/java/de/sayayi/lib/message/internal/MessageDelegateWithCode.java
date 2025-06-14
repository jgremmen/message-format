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
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public final class MessageDelegateWithCode extends AbstractMessageWithCode
{
  /** Delegated message. */
  private final @NotNull Message message;


  public MessageDelegateWithCode(@NotNull String code, @NotNull Message message)
  {
    super(code);

    this.message = requireNonNull(message, "message must not be null");
  }


  /**
   * Returns the underlying message requests are delegated to.
   *
   * @return  underlying message, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Message getMessage() {
    return message;
  }


  @Override
  public @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException
  {
    try {
      return message.formatAsText(messageAccessor, parameters);
    } catch(MessageFormatException ex) {
      throw ex.withCode(code);
    }
  }


  @Override
  public @NotNull MessagePart[] getMessageParts() {
    return message.getMessageParts();
  }


  @Override
  @Unmodifiable
  public @NotNull Set<String> getTemplateNames() {
    return message.getTemplateNames();
  }


  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof MessageDelegateWithCode))
      return false;

    var that = (MessageDelegateWithCode)o;

    return code.equals(that.code) && message.equals(that.message);
  }


  @Override
  public int hashCode() {
    return (59 + code.hashCode()) * 59 + message.hashCode();
  }


  @Override
  public String toString() {
    return "MessageDelegateWithCode(code=" + code + ",message=" + message + ')';
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeString(getCode());
    PackSupport.pack(message, packStream);
  }


  /**
   * @param unpack     unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked message delegate with code, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  public static @NotNull Message.WithCode unpack(@NotNull PackSupport unpack, @NotNull PackInputStream packStream)
      throws IOException {
    return new MessageDelegateWithCode(requireNonNull(packStream.readString()), unpack.unpackMessage(packStream));
  }
}
