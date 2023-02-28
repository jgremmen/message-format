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
package de.sayayi.lib.message.internal.part;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.exception.MessageException;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.internal.FormatterContextImpl;
import de.sayayi.lib.message.internal.part.MessagePart.Parameter;
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.parameter.ParamConfig;
import de.sayayi.lib.message.parameter.key.ConfigKey;
import de.sayayi.lib.message.parameter.value.ConfigValue;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.addSpaces;
import static java.util.Objects.requireNonNull;


/**
 * Parameter message part with optional leading and/or trailing spaces.
 *
 * @author Jeroen Gremmen
 */
@Getter
public final class ParameterPart implements Parameter
{
  private static final long serialVersionUID = 800L;

  private final @NotNull String parameter;
  private final String format;
  private final @NotNull ParamConfig map;
  private final boolean spaceBefore;
  private final boolean spaceAfter;


  public ParameterPart(@NotNull String parameter, String format, boolean spaceBefore, boolean spaceAfter,
                       @NotNull Map<ConfigKey, ConfigValue> map)
  {
    this.parameter = parameter;
    this.format = "".equals(format) ? null : format;
    this.map = new ParamConfig(map);
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text getText(@NotNull MessageContext messageContext, @NotNull Parameters parameters)
  {
    final FormatterContext formatterContext = new FormatterContextImpl(messageContext, parameters,
        parameters.getParameterValue(parameter), null, format, map);

    try {
      return addSpaces(formatterContext.delegateToNextFormatter(), spaceBefore, spaceAfter);
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
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    if (!(o instanceof ParameterPart))
      return false;

    final ParameterPart that = (ParameterPart)o;

    return
        parameter.equals(that.parameter) &&
        Objects.equals(format, that.format) &&
        spaceBefore == that.spaceBefore &&
        spaceAfter == that.spaceAfter &&
        map.equals(that.map);
  }


  @Override
  public int hashCode() {
    return parameter.hashCode() * 11 + (spaceBefore ? 8 : 0) + (spaceAfter ? 2 : 0);
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


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    final Map<ConfigKey, ConfigValue> map = this.map.getMap();

    packStream.writeBoolean(spaceBefore);
    packStream.writeBoolean(spaceAfter);
    packStream.writeSmallVar(map.size());
    packStream.writeString(format);
    packStream.writeString(parameter);

    for(Entry<ConfigKey, ConfigValue> mapEntry: map.entrySet())
    {
      PackHelper.pack(mapEntry.getKey(), packStream);
      PackHelper.pack(mapEntry.getValue(), packStream);
    }
  }


  /**
   * @param unpack     unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked parameter part, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull ParameterPart unpack(@NotNull PackHelper unpack,
                                              @NotNull PackInputStream packStream)
      throws IOException
  {
    final boolean spaceBefore = packStream.readBoolean();
    final boolean spaceAfter = packStream.readBoolean();
    final int size = packStream.readSmallVar();
    final String format = packStream.readString();
    final String parameter = requireNonNull(packStream.readString());
    final Map<ConfigKey, ConfigValue> map = new HashMap<>();

    for(int n = 0; n < size; n++)
    {
      final ConfigKey key = unpack.unpackMapKey(packStream);
      final ConfigValue value = unpack.unpackMapValue(packStream);

      map.put(key, value);
    }

    return new ParameterPart(parameter, format, spaceBefore, spaceAfter, map);
  }
}
