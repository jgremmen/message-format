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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.MapKey;
import de.sayayi.lib.message.data.map.MapValue;
import de.sayayi.lib.message.exception.MessageException;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Parameter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.addSpaces;


/**
 * Parameter message part with optional leading and/or trailing spaces.
 *
 * @author Jeroen Gremmen
 */
@Getter
@EqualsAndHashCode(doNotUseGetters = true)
public final class ParameterPart implements Parameter
{
  private static final long serialVersionUID = 500L;

  private final @NotNull String parameter;
  private final String format;
  private final @NotNull DataMap map;
  private final boolean spaceBefore;
  private final boolean spaceAfter;


  public ParameterPart(@NotNull String parameter, String format, boolean spaceBefore, boolean spaceAfter,
                       @NotNull Map<MapKey,MapValue> map)
  {
    this.parameter = parameter;
    this.format = "".equals(format) ? null : format;
    this.map = new DataMap(map);
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text getText(@NotNull MessageContext messageContext, @NotNull Parameters parameters)
  {
    final Object value = parameters.getParameterValue(parameter);
    final Class<?> type = value != null ? value.getClass() : String.class;
    final ParameterFormatter formatter = messageContext.getFormatter(format, type);

    try {
      return addSpaces(formatter.format(messageContext, value, format, parameters, map), spaceBefore, spaceAfter);
    } catch(Exception ex) {
      throw new MessageException("failed to format parameter " + parameter, ex);
    }
  }


  @Contract(pure = true)
  public @NotNull Set<String> getParameterNames()
  {
    final Set<String> parameterNames = new TreeSet<>();

    parameterNames.add(parameter);
    parameterNames.addAll(map.getParameterNames());

    return parameterNames;
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final StringBuilder s = new StringBuilder("Parameter(name=").append(parameter);

    if (format != null)
      s.append(", format=").append(format);
    if (!map.isEmpty())
      s.append(", map=").append(map);

    if (spaceBefore && spaceAfter)
      s.append(", space-around");
    else if (spaceBefore)
      s.append(", space-before");
    else if (spaceAfter)
      s.append(", space-after");

    return s.append(')').toString();
  }
}