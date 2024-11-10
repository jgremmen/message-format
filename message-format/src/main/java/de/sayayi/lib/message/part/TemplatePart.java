/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.part;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.part.MessagePart.Template;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import de.sayayi.lib.message.util.SortedArrayMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static de.sayayi.lib.message.part.MessagePart.Text.EMPTY;
import static de.sayayi.lib.message.part.TextPartFactory.addSpaces;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;


/**
 * Template message part with optional leading and/or trailing spaces.
 *
 * @author Jeroen Gremmen
 *
 * @since 0.8.0
 */
public final class TemplatePart implements Template
{
  /** template name. */
  private final @NotNull String name;

  /** tells whether the template has a leading space. */
  private final boolean spaceBefore;

  /** tells whether the template has a trailing space. */
  private final boolean spaceAfter;

  /**
   * Default parameter map. If a parameter which is referenced in the template message is not
   * provided during formatting, a default value form this map is used, if available.
   * <p>
   * The map is optimized to require the least amount of space.
   */
  private final SortedArrayMap<String,ConfigValue> defaultParameterMap;

  /**
   * Parameter delegate map. If a parameter is referenced in the template message the parameter
   * name is delegated if an entry exists in this map.
   * <p>
   * The map is optimized to require the least amount of space.
   */
  private final SortedArrayMap<String,String> parameterDelegateMap;


  /**
   * Constructs a template part.
   *
   * @param name                template name, not empty or {@code null}
   * @param spaceBefore         {@code true} if the part has a leading space,
   *                            {@code false} if the part has no leading space
   * @param spaceAfter          {@code true} if the part has a trailing space,
   *                            {@code false} if the part has no trailing space
   * @param defaultParameters   default parameter map, not {@code null}
   * @param parameterDelegates  parameter delegate map, not {@code null}
   */
  public TemplatePart(@NotNull String name, boolean spaceBefore, boolean spaceAfter,
                      @NotNull Map<String,ConfigValue> defaultParameters,
                      @NotNull Map<String,String> parameterDelegates)
  {
    if ((this.name = requireNonNull(name, "name must not be null")).isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;

    defaultParameterMap = new SortedArrayMap<>(defaultParameters);
    parameterDelegateMap = new SortedArrayMap<>(parameterDelegates);
  }


  @Override
  public @NotNull String getName() {
    return name;
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
  public boolean isSpaceAround() {
    return spaceBefore && spaceAfter;
  }


  @Override
  public @NotNull Text getText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
  {
    final Message message = messageAccessor.getTemplateByName(name);

    return addSpaces(message != null
        ? noSpaceText(message.format(messageAccessor, new ParameterAdapter(parameters)))
        : EMPTY,
        spaceBefore, spaceAfter);
  }


  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof Template))
      return false;

    final Template that = (Template)o;

    return spaceBefore == that.isSpaceBefore() &&
           spaceAfter == that.isSpaceAfter() &&
           name.equals(that.getName());
  }


  @Override
  public int hashCode() {
    return name.hashCode() * 11 + (spaceBefore ? 8 : 0) + (spaceAfter ? 2 : 0);
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final StringBuilder s = new StringBuilder("Template(name=").append(name);

    if (spaceBefore && spaceAfter)
      s.append(",space-around");
    else if (spaceBefore)
      s.append(",space-before");
    else if (spaceAfter)
      s.append(",space-after");

    if (!defaultParameterMap.isEmpty())
    {
      s.append(defaultParameterMap
          .stream()
          .map(Entry::toString)
          .collect(joining(",", ",{", "}")));
    }

    return s.append(')').toString();
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @hidden
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeBoolean(spaceBefore);
    packStream.writeBoolean(spaceAfter);

    packStream.writeSmallVar(defaultParameterMap.size());
    packStream.writeSmallVar(parameterDelegateMap.size());

    for(var defaultParameter: defaultParameterMap)
    {
      packStream.writeString(defaultParameter.getKey());
      PackHelper.pack(defaultParameter.getValue(), packStream);
    }

    for(var parameterDelegate: parameterDelegateMap)
    {
      packStream.writeString(parameterDelegate.getKey());
      packStream.writeString(parameterDelegate.getValue());
    }

    packStream.writeString(name);
  }


  /**
   * @param unpack      pack helper instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked template part, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @hidden
   */
  public static @NotNull Template unpack(@NotNull PackHelper unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    switch(packStream.getVersion())
    {
      case 1:
        return unpack_v1(packStream);

      case 2:
        return unpack_v2(unpack, packStream);

      case 3:
      default:
        return unpack_v3(unpack, packStream);
    }
  }


  private static @NotNull Template unpack_v1(@NotNull PackInputStream packStream) throws IOException
  {
    // v1 = spaceBefore, spaceAfter, name

    final boolean spaceBefore = packStream.readBoolean();
    final boolean spaceAfter = packStream.readBoolean();

    return new TemplatePart(requireNonNull(packStream.readString()), spaceBefore, spaceAfter, emptyMap(), emptyMap());
  }


  private static @NotNull Template unpack_v2(@NotNull PackHelper unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    // v2 = spaceBefore, spaceAfter, def. param map (size + key/value-pairs), name

    final boolean spaceBefore = packStream.readBoolean();
    final boolean spaceAfter = packStream.readBoolean();
    final Map<String,ConfigValue> defaultParameterMap = new HashMap<>();

    for(int n = 0, size = packStream.readSmallVar(); n < size; n++)
    {
      defaultParameterMap.put(
          requireNonNull(packStream.readString()),
          unpack.unpackMapValue(packStream));
    }

    return new TemplatePart(requireNonNull(packStream.readString()),
        spaceBefore, spaceAfter, defaultParameterMap, emptyMap());
  }


  private static @NotNull Template unpack_v3(@NotNull PackHelper unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    // v3 = spaceBefore, spaceAfter, size default param map, size parameter delegate map,
    // param map key/value-pairs), param delegate map key/value-pairs, name

    final boolean spaceBefore = packStream.readBoolean();
    final boolean spaceAfter = packStream.readBoolean();

    final int defaultParameterMapSize = packStream.readSmallVar();
    final int parameterDelegateMapSize = packStream.readSmallVar();

    final Map<String,ConfigValue> defaultParameterMap = new HashMap<>();
    for(int n = 0; n < defaultParameterMapSize; n++)
    {
      defaultParameterMap.put(
          requireNonNull(packStream.readString()),
          unpack.unpackMapValue(packStream));
    }

    final Map<String,String> parameterDelegateMap = new HashMap<>();
    for(int n = 0; n < parameterDelegateMapSize; n++)
    {
      parameterDelegateMap.put(
          requireNonNull(packStream.readString()),
          requireNonNull(packStream.readString()));
    }

    return new TemplatePart(requireNonNull(packStream.readString()),
        spaceBefore, spaceAfter, defaultParameterMap, parameterDelegateMap);
  }




  private final class ParameterAdapter implements Parameters
  {
    private final Parameters parameters;


    private ParameterAdapter(@NotNull Parameters parameters) {
      this.parameters = parameters;
    }


    @Override
    public @NotNull Locale getLocale() {
      return parameters.getLocale();
    }


    @Override
    public @NotNull Set<String> getParameterNames()
    {
      final TreeSet<String> names = new TreeSet<>();

      defaultParameterMap.forEach(defaultParameter -> names.add(defaultParameter.getKey()));

      return unmodifiableSet(names);
    }


    @Override
    public Object getParameterValue(@NotNull String parameter)
    {
      final String delegatedParameter = parameterDelegateMap.findValue(parameter);
      if (delegatedParameter != null)
        parameter = delegatedParameter;

      Object value = parameters.getParameterValue(parameter);
      if (value == null)
      {
        final ConfigValue templateConfigValue = defaultParameterMap.findValue(parameter);
        if (templateConfigValue != null)
          value = templateConfigValue.asObject();
      }

      return value;
    }


    @Override
    public String toString()
    {
      return "Parameters(locale='" + parameters.getLocale() + "'," + getParameterNames()
          .stream()
          .map(name -> name + '=' + getParameterValue(name))
          .collect(joining(",", "{", "})"));
    }
  }
}
