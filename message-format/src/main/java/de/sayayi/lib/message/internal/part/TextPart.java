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
package de.sayayi.lib.message.internal.part;

import de.sayayi.lib.message.internal.SpacesUtil;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

import static de.sayayi.lib.message.internal.SpacesUtil.isSpaceChar;
import static de.sayayi.lib.message.internal.SpacesUtil.trimSpaces;


/**
 * Text message part with optional leading and/or trailing spaces.
 *
 * @author Jeroen Gremmen
 */
public final class TextPart implements Text
{
  private static final long serialVersionUID = 800L;

  private final String text;
  private final boolean spaceBefore;
  private final boolean spaceAfter;


  /**
   * Constructs a text part from the given {@code text}.
   *
   * @param text  text or {@code null}
   */
  public TextPart(String text) {
    this(text, false, false);
  }


  public TextPart(String text, boolean addSpaceBefore, boolean addSpaceAfter)
  {
    if (SpacesUtil.isEmpty(text))
    {
      this.text = text;
      this.spaceBefore = addSpaceBefore;
      this.spaceAfter = addSpaceAfter;
    }
    else
    {
      this.text = trimSpaces(text);
      this.spaceBefore = addSpaceBefore || isSpaceChar(text.charAt(0));
      this.spaceAfter = addSpaceAfter || isSpaceChar(text.charAt(text.length() - 1));
    }
  }


  @Override
  public String getText() {
    return text;
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
  public @NotNull String getTextWithSpaces()
  {
    if (isEmpty())
      return spaceBefore || spaceAfter ? " " : "";

    return spaceBefore ? !spaceAfter ? ' ' + text : ' ' + text + ' ' : spaceAfter ? text + ' ' : text;
  }


  @Override
  public boolean isEmpty() {
    return SpacesUtil.isEmpty(text);
  }


  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    if (!(o instanceof Text))
      return false;

    final Text that = (Text)o;

    return spaceBefore == that.isSpaceBefore() &&
           spaceAfter == that.isSpaceAfter() &&
           Objects.equals(text, that.getText());
  }


  @Override
  public int hashCode() {
    return (isEmpty() ? 0 : text.hashCode()) * 11 + (spaceBefore ? 8 : 0) + (spaceAfter ? 2 : 0);
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final StringBuilder s = new StringBuilder("Text(text=").append(text);

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
   * @return  unpacked text part, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull Text unpack(@NotNull PackInputStream packStream) throws IOException
  {
    final boolean spaceBefore = packStream.readBoolean();
    final boolean spaceAfter = packStream.readBoolean();
    final String text = packStream.readString();

    return text == null && !spaceBefore && !spaceAfter
        ? Text.NULL
        : new TextPart(text, spaceBefore, spaceAfter);
  }
}
