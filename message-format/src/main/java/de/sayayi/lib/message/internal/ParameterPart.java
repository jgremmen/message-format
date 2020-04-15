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

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.exception.MessageException;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * @author Jeroen Gremmen
 */
public final class ParameterPart implements MessagePart.Parameter
{
  private static final long serialVersionUID = 500L;

  @Getter private final String parameter;
  @Getter private final String format;
  @Getter private final Data data;
  @Getter private final boolean spaceBefore;
  @Getter private final boolean spaceAfter;


  public ParameterPart(String parameter, String format, boolean spaceBefore, boolean spaceAfter, Data data)
  {
    this.parameter = parameter;
    this.format = "".equals(format) ? null : format;
    this.data = data;
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  @NotNull
  @Override
  @Contract(pure = true)
  public MessagePart.Text getText(@NotNull Parameters parameters)
  {
    final Object value = parameters.getParameterValue(parameter);
    final Class<?> type = (value != null) ? value.getClass() : String.class;
    final ParameterFormatter formatter = parameters.getFormatter(format, type);

    try {
      Text text = formatter.format(value, format, parameters, data);

      if (text == null)
        return TextPart.NULL;
      else if (spaceBefore == text.isSpaceBefore() && spaceAfter == text.isSpaceAfter())
        return text;

      return new TextPart(text.getText(),
          text.isSpaceBefore() || spaceBefore, text.isSpaceAfter() || spaceAfter);
    } catch(Exception ex) {
      throw new MessageException("failed to format parameter " + parameter, ex);
    }
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final StringBuilder s = new StringBuilder(getClass().getSimpleName()).append("(data=").append(parameter);

    if (format != null)
      s.append(", format=").append(format);
    if (data != null)
      s.append(", data=").append(data);

    if (isSpaceBefore() && isSpaceAfter())
      s.append(", space-around");
    else if (isSpaceBefore())
      s.append(", space-before");
    else if (isSpaceAfter())
      s.append(", space-after");

    return s.append(')').toString();
  }
}
