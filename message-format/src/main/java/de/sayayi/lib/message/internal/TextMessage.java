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
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.emptySet;


/**
 * This class represents a message consisting of text only. It also provides information about
 * leading and trailing spaces.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0
 */
public final class TextMessage implements Message.WithSpaces
{
  private static final long serialVersionUID = 800L;

  /** Trimmed message text, or {@code null} */
  private final String text;

  /** Does this message have a leading space? */
  private final boolean spaceBefore;

  /** Does this message have a trailing space? */
  private final boolean spaceAfter;


  /**
   * Construct a text message based on a {@code textPart}.
   *
   * @param textPart  text part, not {@code null}
   */
  public TextMessage(@NotNull Text textPart) {
    this(textPart.getText(), textPart.isSpaceBefore(), textPart.isSpaceAfter());
  }


  private TextMessage(String text, boolean spaceBefore, boolean spaceAfter)
  {
    this.text = text;
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  @Override
  public boolean isSpaceBefore() {
    return spaceBefore;
  }


  @Override
  public boolean isSpaceAfter() {
    return spaceAfter;
  }


  @Override
  public boolean isSpaceAround() {
    return spaceBefore && spaceAfter;
  }


  @Override
  public @NotNull String format(@NotNull MessageAccessor messageAccessor,
                                @NotNull Parameters parameters) {
    return text == null ? "" : text;
  }


  @Override
  public @NotNull MessagePart[] getMessageParts() {
    return new MessagePart[] { new TextPart(text, spaceBefore, spaceAfter) };
  }


  @Override
  public @NotNull Set<String> getTemplateNames() {
    return emptySet();
  }


  @Override
  public boolean isSame(@NotNull Message message)
  {
    if (message instanceof MessageDelegateWithCode)
      message = ((MessageDelegateWithCode)message).getMessage();

    return !(message instanceof LocaleAware) &&
        Arrays.equals(getMessageParts(), message.getMessageParts());
  }


  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    if (!(o instanceof TextMessage))
      return false;

    final TextMessage that = (TextMessage)o;

    return spaceBefore == that.spaceBefore &&
           spaceAfter == that.spaceAfter &&
           Objects.equals(text, that.text);
  }


  @Override
  public int hashCode() {
    return (text == null ? 0 : text.hashCode()) * 11 + (spaceBefore ? 8 : 0) + (spaceAfter ? 2 : 0);
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final StringBuilder s = new StringBuilder("TextMessage(text=").append(text);

    if (spaceBefore && spaceAfter)
      s.append(",space-around");
    else if (spaceBefore)
      s.append(",space-before");
    else if (spaceAfter)
      s.append(",space-after");

    return s.append(')').toString();
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeBoolean(spaceBefore);
    packStream.writeBoolean(spaceAfter);
    packStream.writeString(text);
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked text message, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull Message.WithSpaces unpack(@NotNull PackInputStream packStream)
      throws IOException
  {
    final boolean spaceBefore = packStream.readBoolean();
    final boolean spaceAfter = packStream.readBoolean();

    return new TextMessage(packStream.readString(), spaceBefore, spaceAfter);
  }
}
