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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.TextPartFactory.*;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@SuppressWarnings("UnstableApiUsage")
public final class TextJoiner
{
  private final StringBuilder joined = new StringBuilder();
  private boolean insertSpaceBefore;


  /**
   * Returns the joined text parts as a text preserving leading/trailing spaces.
   *
   * @return  joined text optionally with leading/trailing spaces, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Text asSpacedText() {
    return addSpaces(spacedText(joined.toString()), false, insertSpaceBefore);
  }


  /**
   * Returns the joined text parts as a text with no leading/trailing space.
   *
   * @return  joined text without leading/trailing spaces, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Text asNoSpaceText() {
    return noSpaceText(joined.toString());
  }


  @Contract(value = "_ -> this", mutates = "this")
  public TextJoiner add(@NotNull Text text)
  {
    insertSpaceBefore |= joined.length() > 0 && text.isSpaceBefore();

    String s = text.getText();
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


  @Contract(mutates = "this")
  public TextJoiner addNoSpace(@NotNull Text text) {
    return add(noSpaceText(text.getText()));
  }


  @Contract(mutates = "this")
  public TextJoiner addNoSpace(String text) {
    return add(noSpaceText(text));
  }


  @Contract(mutates = "this")
  public TextJoiner addWithSpace(String s) {
    return add(spacedText(s));
  }


  @Override
  public String toString() {
    return asSpacedText().toString();
  }
}
