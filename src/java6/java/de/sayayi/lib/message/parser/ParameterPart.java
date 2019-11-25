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
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.exception.MessageException;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import lombok.Getter;
import org.jetbrains.annotations.Contract;


/**
 * @author Jeroen Gremmen
 */
final class ParameterPart extends MessagePart
{
  private static final long serialVersionUID = 201L;

  @Getter private final String parameter;
  @Getter private final String format;
  @Getter private final ParameterData data;


  ParameterPart(String parameter, String format, boolean spaceBefore, boolean spaceAfter, ParameterData data)
  {
    super(spaceBefore, spaceAfter);

    this.parameter = parameter;
    this.format = "".equals(format) ? null : format;
    this.data = data;
  }


  @Override
  @Contract(pure = true)
  public String getText(Parameters parameters)
  {
    final Object value = parameters.getParameterValue(parameter);
    final Class<?> type = (value != null) ? value.getClass() : String.class;

    final ParameterFormatter formatter = parameters.getFormatter(format, type);
    if (formatter == null)
      throw new IllegalStateException("no matching formatter found for data " + parameter);

    try {
      return formatter.format(value, format, parameters, data);
    } catch(Exception ex) {
      throw new MessageException("failed to format parameter " + parameter, ex);
    }
  }


  @Override
  @Contract(pure = true)
  public boolean isParameter() {
    return true;
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
