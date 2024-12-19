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
package de.sayayi.lib.message.part;

import de.sayayi.lib.message.internal.part.NoSpaceTextPart;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.MessagePart.Text.EMPTY;
import static de.sayayi.lib.message.part.MessagePart.Text.NULL;
import static de.sayayi.lib.message.util.SpacesUtil.isTrimmedEmpty;


/**
 * Text part factory.
 *
 * @author Jeroen Gremmen
 * @since 0.5.0 (renamed in 0.8.0)
 */
public final class TextPartFactory
{
  private TextPartFactory() {}


  /**
   * Text part which represents a {@code null} text.
   *
   * @return  null text part, never {@code null}
   */
  @Contract(pure = true)
  public static @NotNull Text nullText() {
    return NULL;
  }


  /**
   * Text part which represents empty text.
   *
   * @return  empty text part, never {@code null}
   */
  @Contract(pure = true)
  public static @NotNull Text emptyText() {
    return EMPTY;
  }


  /**
   * Converts the given {@code text} in a text part without leading/trailing space.
   *
   * @param text  text, or {@code null}
   *
   * @return  text part representing the given {@code text} without leading/trailing space,
   *          never {@code null}
   */
  @Contract(pure = true)
  public static @NotNull Text noSpaceText(String text) {
    return text == null ? NULL : isTrimmedEmpty(text) ? EMPTY : new NoSpaceTextPart(text);
  }


  /**
   * Converts the given {@code text} in a text part preserving leading/trailing spaces.
   *
   * @param text  text, or {@code null}
   *
   * @return  text part representing the given {@code text} preserving leading/trailing space,
   *          never {@code null}
   */
  @Contract(pure = true)
  public static @NotNull Text spacedText(String text) {
    return text == null ? NULL : text.isEmpty() ? EMPTY : new TextPart(text);
  }


  /**
   * Changes the {@code text} as follows:
   * <ul>
   *   <li>
   *     if {@code addSpaceBefore} is {@code true} a leading space will be added.
   *     If the text alread had a leading space, no changes are made
   *   </li>
   *   <li>
   *     if {@code addSpaceAfter} is {@code true} a trailing space will be added.
   *     If the text alread had a trailing space, no changes are made
   *   </li>
   * </ul>
   *
   * @param text            text to modify, not {@code null}
   * @param addSpaceBefore  add a leading space if {@code true}
   * @param addSpaceAfter   add a trailing space if {code true}
   *
   * @return  the modified text or the original {@code text} if no changes were made,
   *          never {@code null}
   */
  @Contract(value = "_, false, false -> param1", pure = true)
  public static @NotNull Text addSpaces(@NotNull Text text, boolean addSpaceBefore, boolean addSpaceAfter)
  {
    var textSpaceBefore = text.isSpaceBefore();
    var textSpaceAfter = text.isSpaceAfter();

    addSpaceBefore |= textSpaceBefore;
    addSpaceAfter |= textSpaceAfter;

    return addSpaceBefore == textSpaceBefore && addSpaceAfter == textSpaceAfter
        ? text : new TextPart(text.getText(), addSpaceBefore, addSpaceAfter);
  }
}
