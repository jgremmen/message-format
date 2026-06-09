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
package de.sayayi.lib.message.internal.part.template;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.TypedValue;
import de.sayayi.lib.message.util.SortedStringMap;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static de.sayayi.lib.message.part.MessagePart.Text.EMPTY;
import static de.sayayi.lib.message.part.MessagePart.Text.SPACE;
import static de.sayayi.lib.message.part.TextPartFactory.setSpaces;
import static de.sayayi.lib.message.util.MessageUtil.validateName;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;


/**
 * Template message part with optional leading and/or trailing spaces. A template part references a named template
 * message registered in the {@link MessageAccessor}. During formatting, the referenced template is resolved and
 * formatted using the current parameters. Parameter names can be delegated to other parameter names and missing
 * parameter values can fall back to configured defaults.
 *
 * @author Jeroen Gremmen
 *
 * @since 0.8.0
 */
public final class TemplatePart implements MessagePart.Template
{
  /** template name. */
  private final @NotNull String name;

  /** tells whether the template has a leading space. */
  private final boolean spaceBefore;

  /** tells whether the template has a trailing space. */
  private final boolean spaceAfter;

  /**
   * Default parameter map. If a parameter which is referenced in the template message is not
   * provided during formatting, a default value from this map is used, if available.
   * <p>
   * The map is optimized to require the least amount of space.
   */
  private final SortedStringMap<TypedValue<?>> defaultParameterMap;

  /**
   * Parameter delegate map. If a parameter is referenced in the template message the parameter
   * name is delegated if an entry exists in this map.
   * <p>
   * The map is optimized to require the least amount of space.
   */
  private final SortedStringMap<String> parameterDelegateMap;


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
                      @NotNull java.util.Map<String,TypedValue<?>> defaultParameters,
                      @NotNull java.util.Map<String,String> parameterDelegates)
  {
    this.name = validateName(name, "template name");
    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;

    defaultParameterMap = new SortedStringMap<>(defaultParameters, true);
    parameterDelegateMap = new SortedStringMap<>(parameterDelegates, true);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull String getName() {
    return name;
  }


  /** {@inheritDoc} */
  @Override
  public boolean isSpaceBefore() {
    return spaceBefore;
  }


  /** {@inheritDoc} */
  @Override
  public boolean isSpaceAfter() {
    return spaceAfter;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Text getText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
  {
    final var template = messageAccessor.getTemplateByName(name);

    return setSpaces(template != null
        ? template.formatAsText(messageAccessor, new ParameterAdapter(parameters))
        : EMPTY,
        spaceBefore, spaceAfter);
  }


  /** {@inheritDoc} */
  @Override
  public void serialize(@NotNull Context context)
  {
    final var textJoiner = context.textJoiner();

    if (spaceBefore)
      textJoiner.add(SPACE);

    textJoiner.addNoSpace("%[");
    textJoiner.addNoSpace(name);

    final var contextWithoutQuotes = context.withoutStringQuote();

    parameterDelegateMap.forEach((key, value) ->
        textJoiner.add(',').addNoSpace(key).addNoSpace("->").addNoSpace(value));

    defaultParameterMap.forEach((key, value) -> {
      textJoiner.add(',').addNoSpace(key).add('=');
      value.serialize(contextWithoutQuotes);
    });

    textJoiner.add(']');

    if (spaceAfter)
      textJoiner.add(SPACE);
  }


  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o)
  {
    return o instanceof Template that &&
        spaceBefore == that.isSpaceBefore() &&
        spaceAfter == that.isSpaceAfter() &&
        name.equals(that.getName());
  }


  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return name.hashCode() * 11 + (spaceBefore ? 8 : 0) + (spaceAfter ? 2 : 0);
  }


  /** {@inheritDoc} */
  @Override
  @Contract(pure = true)
  public String toString()
  {
    final var s = new StringBuilder("Template(name=").append(name);

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
   * Serializes this template part to the given pack output stream.
   *
   * @param packStream  data output pack target, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeBoolean(spaceBefore);
    packStream.writeBoolean(spaceAfter);

    packStream.writeSmallVar(defaultParameterMap.size());
    packStream.writeSmallVar(parameterDelegateMap.size());

    for(var defaultParameter: defaultParameterMap.entrySet())
    {
      packStream.writeString(defaultParameter.getKey());
      PackSupport.pack(defaultParameter.getValue(), packStream);
    }

    for(var parameterDelegate: parameterDelegateMap.entrySet())
    {
      packStream.writeString(parameterDelegate.getKey());
      packStream.writeString(parameterDelegate.getValue());
    }

    packStream.writeString(name);
  }


  /**
   * Deserializes a template part from the given pack input stream.
   *
   * @param unpack      pack helper instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked template part, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   */
  public static @NotNull Template unpack(@NotNull PackSupport unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    final var spaceBefore = packStream.readBoolean();
    final var spaceAfter = packStream.readBoolean();

    final var defaultParameterMapSize = packStream.readSmallVar();
    final var parameterDelegateMapSize = packStream.readSmallVar();

    final var defaultParameterMap = new HashMap<String,TypedValue<?>>();
    for(var n = 0; n < defaultParameterMapSize; n++)
    {
      defaultParameterMap.put(
          requireNonNull(packStream.readString()),
          unpack.unpackTypedValue(packStream));
    }

    final var parameterDelegateMap = new HashMap<String,String>();
    for(var n = 0; n < parameterDelegateMapSize; n++)
    {
      parameterDelegateMap.put(
          requireNonNull(packStream.readString()),
          requireNonNull(packStream.readString()));
    }

    return new TemplatePart(requireNonNull(packStream.readString()),
        spaceBefore, spaceAfter, defaultParameterMap, parameterDelegateMap);
  }




  /**
   * A {@link Parameters} adapter that resolves parameter name delegation and provides default values for missing
   * parameters as configured in the enclosing {@link TemplatePart}.
   */
  private final class ParameterAdapter implements Parameters
  {
    private final Parameters parameters;


    /**
     * Creates a new parameter adapter wrapping the given parameters.
     *
     * @param parameters  the original parameters to adapt, not {@code null}
     */
    private ParameterAdapter(@NotNull Parameters parameters) {
      this.parameters = parameters;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Locale getLocale() {
      return parameters.getLocale();
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Set<String> getParameterNames()
    {
      final var names = new TreeSet<String>();
      final var parameterMap = parameters.asParameterMap();

      names.addAll(defaultParameterMap.keySet());
      names.addAll(parameterMap.keySet());

      parameterDelegateMap.forEach((key, value) -> {
        if (parameterMap.containsKey(value))
          names.add(key);
      });

      return unmodifiableSet(names);
    }


    /** {@inheritDoc} */
    @Override
    public Object getParameterValue(@NotNull String parameter)
    {
      var delegatedParameter = parameterDelegateMap.get(parameter);
      if (delegatedParameter != null)
        parameter = delegatedParameter;

      var value = parameters.getParameterValue(parameter);
      if (value == null)
      {
        var templateConfigValue = defaultParameterMap.get(parameter);
        if (templateConfigValue != null)
          value = templateConfigValue.asObject();
      }

      return value;
    }


    /**
     * {@inheritDoc}
     *
     * @return  unmodifiable map view of the adapted parameter names and values
     */
    @Override
    public @NotNull java.util.Map<String,Object> asParameterMap()
    {
      final var map = new TreeMap<String,Object>();

      for(var parameterName: getParameterNames())
        map.put(parameterName, getParameterValue(parameterName));

      return unmodifiableMap(map);
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "Parameters(locale=" + parameters.getLocale() + ',' + asParameterMap() + ')';
    }
  }
}
