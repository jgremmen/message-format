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
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.internal.FormatterContextImpl;
import de.sayayi.lib.message.internal.part.MessagePart.Parameter;
import de.sayayi.lib.message.pack.Pack;
import de.sayayi.lib.message.pack.Unpack;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.addSpaces;


/**
 * Parameter message part with optional leading and/or trailing spaces.
 *
 * @author Jeroen Gremmen
 */
@Getter
public final class ParameterPart implements Parameter
{
  public static final byte PACK_ID = 2;

  private static final long serialVersionUID = 800L;

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
   * @param dataOutput  data output pack target
   *
   * @throws IOException  if an I/O error occurs.
   *
   * @since 0.8.0
   */
  public void pack(@NotNull DataOutput dataOutput) throws IOException
  {
    dataOutput.writeByte(PACK_ID);
    dataOutput.writeByte((format != null ? 4 : 0) + (spaceBefore ? 2 : 0) + (spaceAfter ? 1 : 0));
    dataOutput.writeUTF(parameter);
    if (format != null)
      dataOutput.writeUTF(format);

    final Map<MapKey,MapValue> map = this.map.asObject();
    final int size = map.size();

    dataOutput.writeByte(size);

    for(Entry<MapKey,MapValue> mapEntry: map.entrySet())
    {
      Pack.pack(mapEntry.getKey(), dataOutput);
      Pack.pack(mapEntry.getValue(), dataOutput);
    }
  }


  /**
   * @param unpack     unpacker instance, not {@code null}
   * @param dataInput  source data input, not {@code null}
   *
   * @return  unpacked parameter part, never {@code null}
   *
   * @throws IOException  if an I/O error occurs.
   *
   * @since 0.8.0
   */
  public static @NotNull ParameterPart unpack(@NotNull Unpack unpack, @NotNull DataInput dataInput) throws IOException
  {
    final byte flags = dataInput.readByte();
    final String parameter = dataInput.readUTF();
    final String format = (flags & 4) == 0 ? null : dataInput.readUTF();
    final int size = dataInput.readUnsignedByte();
    final Map<MapKey,MapValue> map = new HashMap<>();

    for(int n = 0; n < size; n++)
    {
      final MapKey key = unpack.loadMapKey(dataInput);
      final MapValue value = unpack.loadMapValue(dataInput);

      map.put(key, value);
    }

    return new ParameterPart(parameter, format, (flags & 2) != 0, (flags & 1) != 0, map);
  }
}
