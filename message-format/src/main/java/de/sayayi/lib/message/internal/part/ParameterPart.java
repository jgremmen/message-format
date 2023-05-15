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

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.addSpaces;
import static java.util.Objects.requireNonNull;


/**
 * Parameter message part with optional leading and/or trailing spaces.
 *
 * @author Jeroen Gremmen
 */
public final class ParameterPart implements Parameter
{
  private static final long serialVersionUID = 800L;

  private final @NotNull String name;
  private final String format;
  private final @NotNull ParamConfig paramConfig;
  private final boolean spaceBefore;
  private final boolean spaceAfter;


  public ParameterPart(@NotNull String name, String format, boolean spaceBefore, boolean spaceAfter,
                       @NotNull Map<ConfigKey,ConfigValue> map)
  {
    if ((this.name = requireNonNull(name, "name must not be null")).isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    this.format = "".equals(format) ? null : format;
    this.paramConfig = new ParamConfig(requireNonNull(map, "map must not be null"));
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return name;
  }


  @Override
  @Contract(pure = true)
  public String getFormat() {
    return format;
  }


  @Override
  @Contract(pure = true)
  public @NotNull ParamConfig getParamConfig() {
    return paramConfig;
  }


  @Override
  public boolean isSpaceBefore() {
    return spaceBefore;
  }


  @Override
  public boolean isSpaceAfter() {
    return spaceAfter;
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text getText(@NotNull MessageAccessor messageAccessor,
                               @NotNull Parameters parameters)
  {
    final FormatterContext context = new FormatterContextImpl(messageAccessor, parameters,
        parameters.getParameterValue(name), null, format, paramConfig);

    try {
      return addSpaces(context.delegateToNextFormatter(), spaceBefore, spaceAfter);
    } catch(Exception ex) {
      throw new MessageException("failed to format parameter " + name, ex);
    }
  }


  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    if (!(o instanceof Parameter))
      return false;

    final Parameter that = (Parameter)o;

    return
        name.equals(that.getName()) &&
        Objects.equals(format, that.getFormat()) &&
        spaceBefore == that.isSpaceBefore() &&
        spaceAfter == that.isSpaceAfter() &&
        paramConfig.equals(that.getParamConfig());
  }


  @Override
  public int hashCode() {
    return name.hashCode() * 11 + (spaceBefore ? 8 : 0) + (spaceAfter ? 2 : 0);
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final StringBuilder s = new StringBuilder("Parameter(name=").append(name);

    if (format != null)
      s.append(",format=").append(format);
    if (!paramConfig.isEmpty())
      s.append(",map=").append(paramConfig);

    if (spaceBefore && spaceAfter)
      s.append(",space-around");
    else if (spaceBefore)
      s.append(",space-before");
    else if (spaceAfter)
      s.append(",space-after");

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
    final Map<ConfigKey,ConfigValue> map = paramConfig.getMap();

    packStream.writeBoolean(spaceBefore);
    packStream.writeBoolean(spaceAfter);
    packStream.writeSmallVar(map.size());
    packStream.writeString(format);
    packStream.writeString(name);

    for(final Entry<ConfigKey,ConfigValue> mapEntry: map.entrySet())
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
    final String name = requireNonNull(packStream.readString());
    final Map<ConfigKey,ConfigValue> map = new HashMap<>();

    for(int n = 0; n < size; n++)
      map.put(unpack.unpackMapKey(packStream), unpack.unpackMapValue(packStream));

    return new ParameterPart(name, format, spaceBefore, spaceAfter, map);
  }
}
