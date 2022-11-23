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

import de.sayayi.lib.message.internal.part.MessagePart.Text;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.internal.SpacesUtil.trimSpaces;


/**
 * Text message part without leading and trailing spaces.
 *
 * @author Jeroen Gremmen
 */
public final class NoSpaceTextPart implements Text
{
  private static final long serialVersionUID = 800L;

  @Getter private final @NotNull String text;


  public NoSpaceTextPart(@NotNull String text) {
    this.text = trimSpaces(text);
  }


  @Override
  public boolean isEmpty() {
    return text.isEmpty();
  }


  @Override
  public boolean isSpaceBefore() {
    return false;
  }


  @Override
  public boolean isSpaceAfter() {
    return false;
  }


  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    if (!(o instanceof Text))
      return false;

    final Text that = (Text)o;

    return !that.isSpaceBefore() && !that.isSpaceAfter() && text.equals(that.getText());
  }


  @Override
  public int hashCode() {
    return isEmpty() ? 0 : text.hashCode();
  }


  @Override
  @Contract(pure = true)
  public String toString() {
    return "Text(text=" + text + ')';
  }
}