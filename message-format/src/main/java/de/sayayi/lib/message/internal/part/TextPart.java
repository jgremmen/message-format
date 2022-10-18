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

import de.sayayi.lib.message.internal.SpacesUtil;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import java.util.Objects;

import static de.sayayi.lib.message.internal.SpacesUtil.isSpaceChar;
import static de.sayayi.lib.message.internal.SpacesUtil.trimSpaces;


/**
 * Text message part with optional leading and/or trailing spaces.
 *
 * @author Jeroen Gremmen
 */
@Getter
public final class TextPart implements Text
{
  private static final long serialVersionUID = 500L;

  private final String text;
  private final boolean spaceBefore;
  private final boolean spaceAfter;


  public TextPart(String text) {
    this(text, false, false);
  }


  public TextPart(String text, boolean spaceBefore, boolean spaceAfter)
  {
    final boolean empty = SpacesUtil.isEmpty(text);

    this.text = trimSpaces(text);
    this.spaceBefore = spaceBefore || (!empty && isSpaceChar(text.charAt(0)));
    this.spaceAfter = spaceAfter || (!empty && isSpaceChar(text.charAt(text.length() - 1)));
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

    return spaceBefore == that.isSpaceBefore() && spaceAfter == that.isSpaceAfter() &&
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
      s.append(", space-around");
    else if (spaceBefore)
      s.append(", space-before");
    else if (spaceAfter)
      s.append(", space-after");

    return s.append(')').toString();
  }
}