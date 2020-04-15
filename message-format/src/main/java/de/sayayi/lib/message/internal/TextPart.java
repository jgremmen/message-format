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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.internal.MessagePart.Text;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import static java.lang.Character.isSpaceChar;


/**
 * @author Jeroen Gremmen
 */
@EqualsAndHashCode(doNotUseGetters = true)
public final class TextPart implements Text
{
  private static final long serialVersionUID = 500L;

  @Getter private final String text;
  @Getter private final boolean spaceBefore;
  @Getter private final boolean spaceAfter;


  public TextPart(String text) {
    this(text, false, false);
  }


  public TextPart(String text, boolean spaceBefore, boolean spaceAfter)
  {
    final boolean empty = text == null || text.isEmpty();

    this.text = empty ? null : text.trim();
    this.spaceBefore = spaceBefore || (!empty && isSpaceChar(text.charAt(0)));
    this.spaceAfter = spaceAfter || (!empty && isSpaceChar(text.charAt(text.length() - 1)));
  }


  @Override
  public boolean isEmpty() {
    return text == null || text.isEmpty();
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final StringBuilder s = new StringBuilder(getClass().getSimpleName()).append("(text=").append(text);

    if (isSpaceBefore() && isSpaceAfter())
      s.append(", space-around");
    else if (isSpaceBefore())
      s.append(", space-before");
    else if (isSpaceAfter())
      s.append(", space-after");

    return s.append(')').toString();
  }
}
