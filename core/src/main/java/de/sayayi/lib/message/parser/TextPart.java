/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.Message.Parameters;
import org.jetbrains.annotations.Contract;


/**
 * @author Jeroen Gremmen
 */
final class TextPart extends MessagePart
{
  private static final long serialVersionUID = 201L;

  private final String text;


  TextPart(String text, boolean spaceBefore, boolean spaceAfter)
  {
    super(spaceBefore, spaceAfter);

    this.text = text;
  }


  @Override
  @Contract(pure = true)
  public String getText(Parameters parameters) {
    return text;
  }


  @Override
  @Contract(pure = true)
  public boolean isParameter() {
    return false;
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
