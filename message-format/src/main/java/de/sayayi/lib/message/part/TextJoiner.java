/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.part;

import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.TextPartFactory.*;
import static de.sayayi.lib.message.util.MessageUtil.isSpaceChar;


/**
 * Text and string object joiner that accumulates {@link Text} parts, strings and characters into a single text,
 * collapsing adjacent spaces into a single separator space. Trailing space state is tracked and carried over between
 * consecutive {@code add} calls so that a space is only emitted when actual non-space content follows.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class TextJoiner
{
  private final StringBuilder joined = new StringBuilder();
  private boolean insertSpaceBefore;


  /**
   * Returns the joined text parts as a {@link Text} that preserves a trailing space. If non-space content was
   * followed by a space (or a space-only text), the resulting text will have its
   * {@linkplain Text#isSpaceAfter() trailing space} flag set.
   *
   * @return  joined text optionally with a trailing space, never {@code null}
   *
   * @see #asNoSpaceText()
   */
  @Contract(pure = true)
  public @NotNull Text asSpacedText() {
    return addSpaces(spacedText(joined.toString()), false, insertSpaceBefore);
  }


  /**
   * Returns the joined text parts as a {@link Text} with no leading or trailing space. Any pending trailing space is
   * discarded.
   *
   * @return  joined text without leading/trailing spaces, never {@code null}
   *
   * @see #asSpacedText()
   */
  @Contract(pure = true)
  public @NotNull Text asNoSpaceText() {
    return noSpaceText(joined.toString());
  }


  /**
   * Adds a {@link Text} part to this joiner, respecting its leading and trailing space flags. If the text has a
   * {@linkplain Text#isSpaceBefore() leading space} or a pending space was recorded from a previous call, a space
   * separator is inserted before the text content. If the text content is {@code null} or empty, only the space state
   * is accumulated without appending any characters.
   *
   * @param text  text to add, not {@code null}
   *
   * @return  this text joiner, never {@code null}
   */
  @Contract(value = "_ -> this", mutates = "this")
  public @NotNull TextJoiner add(@NotNull Text text)
  {
    insertSpaceBefore |= text.isSpaceBefore();

    var s = text.getText();
    if (s != null && !s.isEmpty())
    {
      if (insertSpaceBefore)
        joined.append(' ');
      joined.append(s);

      insertSpaceBefore = text.isSpaceAfter();
    }
    else
      insertSpaceBefore |= text.isSpaceAfter();

    return this;
  }


  /**
   * Adds a single character to this joiner. If the character is a
   * {@linkplain de.sayayi.lib.message.util.MessageUtil#isSpaceChar(char) space character}, it is not appended
   * directly but instead recorded as a pending space that will be emitted as a single separator space before the next
   * non-space content. Otherwise, the character is appended immediately, preceded by a separator space if one was
   * pending.
   *
   * @param c  character to add
   *
   * @return  this text joiner, never {@code null}
   *
   * @since 0.21.0
   */
  @Contract(value = "_ -> this", mutates = "this")
  public @NotNull TextJoiner add(char c)
  {
    if (isSpaceChar(c))
      insertSpaceBefore = true;
    else
    {
      if (insertSpaceBefore)
        joined.append(' ');

      joined.append(c);
      insertSpaceBefore = false;
    }

    return this;
  }


  /**
   * Adds a {@link Text} part to this joiner with its leading and trailing spaces stripped.
   *
   * @param text  text to add, not {@code null}
   *
   * @return  this text joiner, never {@code null}
   */
  @Contract(value = "_ -> this", mutates = "this")
  public @NotNull TextJoiner addNoSpace(@NotNull Text text) {
    return add(noSpaceText(text.getText()));
  }


  /**
   * Adds a string to this joiner with its leading and trailing spaces stripped.
   *
   * @param text  string to add, or {@code null}
   *
   * @return  this text joiner, never {@code null}
   */
  @Contract(value = "_ -> this", mutates = "this")
  public @NotNull TextJoiner addNoSpace(String text) {
    return add(noSpaceText(text));
  }


  /**
   * Adds a string to this joiner, preserving its leading and trailing spaces.
   *
   * @param text  string to add, or {@code null}
   *
   * @return  this text joiner, never {@code null}
   */
  @Contract(value = "_ -> this", mutates = "this")
  public @NotNull TextJoiner addWithSpace(String text) {
    return add(spacedText(text));
  }


  /**
   * Returns the string representation of this joiner by delegating to
   * {@link #asSpacedText() asSpacedText()}.{@link Object#toString() toString()}.
   *
   * @return  the joined text as a string, never {@code null}
   */
  @Override
  public @NotNull String toString() {
    return asSpacedText().toString();
  }
}
