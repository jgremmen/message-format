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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.*;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.sayayi.lib.message.data.map.MapKey.NAME_TYPE;
import static de.sayayi.lib.message.formatter.ParameterFormatter.NULL_TYPE;
import static de.sayayi.lib.message.internal.part.MessagePart.Text.NULL;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class FormatterContextImpl implements FormatterContext
{
  @Getter private final @NotNull MessageContext messageContext;
  private final @NotNull Parameters parameters;
  private final Object value;
  private final String format;
  private final @NotNull DataMap map;
  private final @NotNull Deque<ParameterFormatter> parameterFormatters;


  public FormatterContextImpl(@NotNull MessageContext messageContext, @NotNull Parameters parameters,
                              Object value, Class<?> type, String format, @NotNull DataMap map)
  {
    this.messageContext = messageContext;
    this.parameters = parameters;
    this.value = value;
    this.format = format;
    this.map = map;

    if (type == null)
      type = value == null ? NULL_TYPE : value.getClass();

    parameterFormatters = new ArrayDeque<>(messageContext.getFormatters(format, type));
  }


  @Override
  public @NotNull Locale getLocale() {
    return parameters.getLocale();
  }


  @Override
  public Object getParameterValue(@NotNull String parameter) {
    return parameters.getParameterValue(parameter);
  }


  @Override
  public @NotNull SortedSet<String> getParameterNames() {
    return parameters.getParameterNames();
  }


  @Override
  public @NotNull Optional<MapValue> getMapValue(Object key, @NotNull Set<MapKey.Type> keyTypes,
                                                 Set<MapValue.Type> valueTypes) {
    return Optional.ofNullable(map.find(messageContext, key, parameters, keyTypes, valueTypes));
  }


  @Override
  public @NotNull Optional<Message.WithSpaces> getMapMessage(Object key, @NotNull Set<MapKey.Type> keyTypes,
                                                             boolean includeDefault) {
    return Optional.ofNullable(map.getMessage(messageContext, key, parameters, keyTypes, includeDefault));
  }


  @Override
  public @NotNull Optional<Data> getConfigValueData(@NotNull String name)
  {
    final MapValue mapValue = map.find(messageContext, name, parameters, NAME_TYPE, null);
    return mapValue != null ? Optional.of(mapValue) : Optional.ofNullable(messageContext.getDefaultData(name));
  }


  @Override
  public @NotNull Optional<String> getConfigValueString(@NotNull String name)
  {
    final MapValueString string =
        (MapValueString)map.find(messageContext, name, parameters, NAME_TYPE, MapValue.STRING_TYPE);

    if (string != null)
      return Optional.of(string.asObject());

    final MapValue mapValue = messageContext.getDefaultData(name);

    return mapValue instanceof MapValueString
        ? Optional.of(((MapValueString)mapValue).asObject())
        : Optional.empty();
  }


  @Override
  public @NotNull OptionalLong getConfigValueNumber(@NotNull String name)
  {
    final MapValueNumber number =
        (MapValueNumber)map.find(messageContext, name, parameters, NAME_TYPE, MapValue.NUMBER_TYPE);

    if (number != null)
      return OptionalLong.of(number.asObject());

    final MapValue mapValue = messageContext.getDefaultData(name);

    return mapValue instanceof MapValueNumber
        ? OptionalLong.of(((MapValueNumber)mapValue).asObject())
        : OptionalLong.empty();
  }


  @Override
  public @NotNull Optional<Boolean> getConfigValueBool(@NotNull String name)
  {
    final MapValueBool bool = (MapValueBool)map.find(messageContext, name, parameters, NAME_TYPE, MapValue.BOOL_TYPE);

    if (bool != null)
      return Optional.of(bool.asObject());

    final MapValue mapValue = messageContext.getDefaultData(name);

    return mapValue instanceof MapValueBool
        ? Optional.of(((MapValueBool)mapValue).asObject())
        : Optional.empty();
  }


  @Override
  public @NotNull Text delegateToNextFormatter() {
    return parameterFormatters.removeFirst().format(this, value);
  }


  @Override
  public @NotNull Text format(Object value, Class<?> type, boolean propagateFormat)
  {
    return new FormatterContextImpl(messageContext, parameters, value, type, propagateFormat ? format : null, map)
        .delegateToNextFormatter();
  }


  @Override
  public @NotNull Text format(Object value, Class<?> type, String format) {
    return new FormatterContextImpl(messageContext, parameters, value, type, format, map).delegateToNextFormatter();
  }


  @Override
  public @NotNull Text format(Message.WithSpaces message)
  {
    return message == null
        ? NULL
        : new TextPart(message.format(messageContext, parameters), message.isSpaceBefore(), message.isSpaceAfter());
  }
}
