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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.internal.pack.PackInputStream;
import de.sayayi.lib.message.internal.pack.PackOutputStream;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;


/**
 * This class represents a message consisting of text only. It also provides information about
 * leading and trailing spaces.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0
 */
public final class TextMessage implements Message.WithSpaces
{
  /** Text part, not {@code null} */
  private final Text textPart;


  /**
   * Construct a text message based on a {@code textPart}.
   *
   * @param textPart  text part, not {@code null}
   */
  public TextMessage(@NotNull Text textPart) {
    this.textPart = textPart;
  }


  @Override
  public boolean isSpaceBefore() {
    return textPart.isSpaceBefore();
  }


  @Override
  public boolean isSpaceAfter() {
    return textPart.isSpaceAfter();
  }


  @Override
  public boolean isSpaceAround() {
    return textPart.isSpaceAround();
  }


  @Override
  public @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException {
    return textPart;
  }


  @Override
  public @NotNull MessagePart[] getMessageParts() {
    return new MessagePart[] { textPart };
  }


  @Override
  public @NotNull Set<String> getTemplateNames() {
    return Set.of();
  }


  @Override
  public boolean isSame(@NotNull Message message)
  {
    if (message instanceof MessageDelegateWithCode)
      message = ((MessageDelegateWithCode)message).getMessage();

    return !(message instanceof LocaleAware) && Arrays.equals(getMessageParts(), message.getMessageParts());
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof TextMessage && textPart.equals(((TextMessage)o).textPart);
  }


  @Override
  public int hashCode() {
    return textPart.hashCode();
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    var s = new StringBuilder("TextMessage(text=").append(textPart.getText());

    if (textPart.isSpaceBefore() && textPart.isSpaceAfter())
      s.append(",space-around");
    else if (textPart.isSpaceBefore())
      s.append(",space-before");
    else if (textPart.isSpaceAfter())
      s.append(",space-after");

    return s.append(')').toString();
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
    packStream.writeBoolean(textPart.isSpaceBefore());
    packStream.writeBoolean(textPart.isSpaceAfter());
    packStream.writeString(textPart.getText());
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked text message, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  public static @NotNull Message.WithSpaces unpack(@NotNull PackInputStream packStream) throws IOException
  {
    var spaceBefore = packStream.readBoolean();
    var spaceAfter = packStream.readBoolean();

    return new TextMessage(new TextPart(packStream.readString(), spaceBefore, spaceAfter));
  }
}
