/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.internal.part;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.internal.part.MessagePart.Text.EMPTY;
import static de.sayayi.lib.message.internal.part.MessagePart.Text.NULL;
import static lombok.AccessLevel.PRIVATE;


/**
 * Message part factory.
 *
 * @author Jeroen Gremmen
 */
@SuppressWarnings("java:S1118")
@NoArgsConstructor(access = PRIVATE)
public final class MessagePartFactory
{
  @Contract(pure = true)
  public static @NotNull Text nullText() {
    return NULL;
  }


  @Contract(pure = true)
  public static @NotNull Text emptyText() {
    return EMPTY;
  }


  @Contract(pure = true)
  @SuppressWarnings("java:S3358")
  public static @NotNull Text noSpaceText(String text) {
    return text == null ? NULL : text.trim().isEmpty() ? EMPTY : new NoSpaceTextPart(text);
  }


  @Contract(pure = true)
  @SuppressWarnings("java:S3358")
  public static @NotNull Text spacedText(String text) {
    return text == null ? NULL : text.isEmpty() ? EMPTY : new TextPart(text);
  }


  public static @NotNull Text addSpaces(@NotNull Text text, boolean spaceBefore, boolean spaceAfter)
  {
    final boolean textSpaceBefore = text.isSpaceBefore();
    final boolean textSpaceAfter = text.isSpaceAfter();

    return (textSpaceBefore || spaceBefore) == textSpaceBefore && (textSpaceAfter || spaceAfter) == textSpaceAfter
        ? text
        : new TextPart(text.getText(), textSpaceBefore || spaceBefore, textSpaceAfter || spaceAfter);
  }


  @Contract(pure = true)
  public static @NotNull Text messageToText(@NotNull MessageContext messageContext, Message.WithSpaces message,
                                            @NotNull Parameters parameters)
  {
    return message == null
        ? NULL
        : new TextPart(message.format(messageContext, parameters), message.isSpaceBefore(), message.isSpaceAfter());
  }
}
