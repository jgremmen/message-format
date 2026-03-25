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
package de.sayayi.lib.message.internal.part.text;

import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.util.MessageUtil;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

import static de.sayayi.lib.message.util.MessageUtil.isSpaceChar;
import static de.sayayi.lib.message.util.MessageUtil.trimAndNormalizeSpaces;


/**
 * Text message part with optional leading and/or trailing spaces.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public final class TextPart implements MessagePart.Text
{
  /** trimmed text string or {@code null}. */
  private final String text;

  /** tells whether the text has a leading space. */
  private final boolean spaceBefore;

  /** tells whether the parameter has a trailing space. */
  private final boolean spaceAfter;


  /**
   * Constructs a text part from the given {@code text}.
   *
   * @param text  text or {@code null}
   */
  public TextPart(String text) {
    this(text, false, false);
  }


  /**
   * Constructs a text part with optional leading/trailing space.
   *
   * @param text            text or {@code null}
   * @param addSpaceBefore  {@code true} if the part has a leading space,
   *                        {@code false} if the part has no leading space
   * @param addSpaceAfter   {@code true} if the part has a trailing space,
   *                        {@code false} if the part has no trailing space
   */
  public TextPart(String text, boolean addSpaceBefore, boolean addSpaceAfter)
  {
    if (MessageUtil.isEmpty(text))
    {
      this.text = text;
      this.spaceBefore = addSpaceBefore;
      this.spaceAfter = addSpaceAfter;
    }
    else
    {
      this.text = trimAndNormalizeSpaces(text);
      this.spaceBefore = addSpaceBefore || isSpaceChar(text.charAt(0));
      this.spaceAfter = addSpaceAfter || isSpaceChar(text.charAt(text.length() - 1));
    }
  }


  @Override
  public String getText() {
    return text;
  }


  @Override
  public @NotNull String getTextNotNull() {
    return text == null ? "" : text;
  }


  @Override
  public @NotNull String getTextWithSpaces()
  {
    if (!spaceBefore && !spaceAfter)
      return text == null ? "" : text;
    else if (text == null)
      return " ";

    final var chars = text.toCharArray();
    final var charsWithSpaces = new char[chars.length + 2];
    int n = 0;

    if (spaceBefore)
      charsWithSpaces[n++] = ' ';

    for(char ch: chars)
      charsWithSpaces[n++] = ch;

    if (spaceAfter)
      charsWithSpaces[n++] = ' ';

    return new String(charsWithSpaces, 0, n);
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
  public boolean isEmpty() {
    return MessageUtil.isEmpty(text);
  }


  @Override
  public boolean equals(Object o)
  {
    return o instanceof Text that &&
        spaceBefore == that.isSpaceBefore() &&
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
    final var s = new StringBuilder("Text(");

    if (text == null)
      s.append("null");
    else
      s.append('\'').append(text.replace("'", "\\'")).append('\'');

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
    final var spaceBefore = packStream.readBoolean();
    final var spaceAfter = packStream.readBoolean();
    final var text = packStream.readString();

    return text == null && !spaceBefore && !spaceAfter
        ? Text.NULL
        : new TextPart(text, spaceBefore, spaceAfter);
  }
}
