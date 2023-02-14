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
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.SortedSet;

import static java.util.Collections.emptySortedSet;
import static lombok.AccessLevel.PRIVATE;


/**
 * @author Jeroen Gremmen
 */
@ToString(doNotUseGetters = true)
@EqualsAndHashCode(doNotUseGetters = true)
@RequiredArgsConstructor(access = PRIVATE)
public final class TextMessage implements Message.WithSpaces
{
  public static final int PACK_ID = 6;

  private static final long serialVersionUID = 800L;

  private final String text;

  @Getter private final boolean spaceBefore;
  @Getter private final boolean spaceAfter;


  public TextMessage(@NotNull Text textPart) {
    this(textPart.getText(), textPart.isSpaceBefore(), textPart.isSpaceAfter());
  }


  @Override
  public @NotNull String format(@NotNull MessageContext messageContext, @NotNull Parameters parameters) {
    return text == null ? "" : text;
  }


  @Override
  @Contract(value = "-> false", pure = true)
  public boolean hasParameters() {
    return false;
  }


  @Override
  public @NotNull SortedSet<String> getParameterNames() {
    return emptySortedSet();
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
    packStream.write(PACK_ID, 3);
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
  public static @NotNull Message.WithSpaces unpack(@NotNull PackInputStream packStream) throws IOException
  {
    final boolean spaceBefore = packStream.readBoolean();
    final boolean spaceAfter = packStream.readBoolean();

    return new TextMessage(packStream.readString(), spaceBefore, spaceAfter);
  }
}
