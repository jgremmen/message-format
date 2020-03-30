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
package de.sayayi.lib.message.impl;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.parser.MessagePart;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class MultipartMessage implements Message
{
  private static final long serialVersionUID = 201L;

  private final MessagePart[] parts;


  public MultipartMessage(@NotNull List<MessagePart> parts) {
    this.parts = parts.toArray(new MessagePart[0]);
  }


  @Override
  @Contract(pure = true)
  public String format(@NotNull Parameters parameters)
  {
    final StringBuilder message = new StringBuilder();
    boolean spaceBefore = false;

    for(final MessagePart part: parts)
    {
      final String text = part.getText(parameters);

      if (!isEmpty(text))
      {
        if ((spaceBefore || part.isSpaceBefore()) && message.length() > 0)
          message.append(' ');

        message.append(text);
        spaceBefore = part.isSpaceAfter();
      }
    }

    return message.toString();
  }


  @Override
  @Contract(pure = true)
  public boolean hasParameters() {
    return parts.length > 0 && (parts.length > 1 || parts[0].isParameter());
  }


  @Override
  public boolean isSpaceBefore() {
    return parts.length > 0 && parts[0].isSpaceBefore();
  }


  @Override
  public boolean isSpaceAfter() {
    return parts.length > 0 && parts[parts.length - 1].isSpaceAfter();
  }


  private static boolean isEmpty(String s) {
    return s == null || s.isEmpty();
  }
}