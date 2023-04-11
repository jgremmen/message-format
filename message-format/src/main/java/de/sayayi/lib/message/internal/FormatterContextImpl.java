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
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageSupportAccessor;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.parameter.ParamConfig;
import de.sayayi.lib.message.parameter.key.ConfigKey;
import de.sayayi.lib.message.parameter.value.ConfigValue;
import de.sayayi.lib.message.parameter.value.ConfigValueBool;
import de.sayayi.lib.message.parameter.value.ConfigValueNumber;
import de.sayayi.lib.message.parameter.value.ConfigValueString;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.sayayi.lib.message.formatter.ParameterFormatter.NULL_TYPE;
import static de.sayayi.lib.message.internal.part.MessagePart.Text.NULL;
import static de.sayayi.lib.message.parameter.key.ConfigKey.NAME_TYPE;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class FormatterContextImpl implements FormatterContext
{
  private final @NotNull MessageSupportAccessor messageSupport;
  private final @NotNull Parameters parameters;
  private final Object value;
  private final String format;
  private final @NotNull ParamConfig map;
  private final @NotNull Deque<ParameterFormatter> parameterFormatters;


  public FormatterContextImpl(@NotNull MessageSupportAccessor messageSupport,
                              @NotNull Parameters parameters, Object value, Class<?> type,
                              String format, @NotNull ParamConfig map)
  {
    this.messageSupport = messageSupport;
    this.parameters = parameters;
    this.value = value;
    this.format = format;
    this.map = map;

    if (type == null)
      type = value == null ? NULL_TYPE : value.getClass();

    parameterFormatters = new ArrayDeque<>(messageSupport.getFormatters(format, type));
  }


  @Override
  public @NotNull MessageSupportAccessor getMessageSupport() {
    return messageSupport;
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
  public @NotNull Optional<ConfigValue> getMapValue(Object key,
                                                    @NotNull Set<ConfigKey.Type> keyTypes,
                                                    Set<ConfigValue.Type> valueTypes) {
    return Optional.ofNullable(map.find(messageSupport, key, parameters, keyTypes, valueTypes));
  }


  @Override
  public @NotNull Optional<Message.WithSpaces> getMapMessage(Object key,
                                                             @NotNull Set<ConfigKey.Type> keyTypes,
                                                             boolean includeDefault) {
    return Optional.ofNullable(map.getMessage(messageSupport, key, parameters, keyTypes, includeDefault));
  }


  @Override
  public @NotNull Optional<ConfigValue> getConfigValue(@NotNull String name)
  {
    final ConfigValue configValue =
        map.find(messageSupport, name, parameters, NAME_TYPE, null);

    return configValue != null
        ? Optional.of(configValue)
        : Optional.ofNullable(messageSupport.getDefaultParameterConfig(name));
  }


  @Override
  public @NotNull Optional<String> getConfigValueString(@NotNull String name)
  {
    final ConfigValueString string = (ConfigValueString)
        map.find(messageSupport, name, parameters, NAME_TYPE, ConfigValue.STRING_TYPE);

    if (string != null)
      return Optional.of(string.asObject());

    final ConfigValue configValue = messageSupport.getDefaultParameterConfig(name);

    return configValue instanceof ConfigValueString
        ? Optional.of(((ConfigValueString)configValue).asObject())
        : Optional.empty();
  }


  @Override
  public @NotNull OptionalLong getConfigValueNumber(@NotNull String name)
  {
    final ConfigValueNumber number = (ConfigValueNumber)
        map.find(messageSupport, name, parameters, NAME_TYPE, ConfigValue.NUMBER_TYPE);

    if (number != null)
      return OptionalLong.of(number.asObject());

    final ConfigValue configValue = messageSupport.getDefaultParameterConfig(name);

    return configValue instanceof ConfigValueNumber
        ? OptionalLong.of(((ConfigValueNumber)configValue).asObject())
        : OptionalLong.empty();
  }


  @Override
  public @NotNull Optional<Boolean> getConfigValueBool(@NotNull String name)
  {
    final ConfigValueBool bool = (ConfigValueBool)
        map.find(messageSupport, name, parameters, NAME_TYPE, ConfigValue.BOOL_TYPE);

    if (bool != null)
      return Optional.of(bool.asObject());

    final ConfigValue configValue = messageSupport.getDefaultParameterConfig(name);

    return configValue instanceof ConfigValueBool
        ? Optional.of(((ConfigValueBool)configValue).asObject())
        : Optional.empty();
  }


  @Override
  public @NotNull Text delegateToNextFormatter() {
    return parameterFormatters.removeFirst().format(this, value);
  }


  @Override
  public @NotNull Text format(Object value, Class<?> type, boolean propagateFormat)
  {
    return new FormatterContextImpl(messageSupport, parameters, value, type,
        propagateFormat ? format : null, map).delegateToNextFormatter();
  }


  @Override
  public @NotNull Text format(Object value, Class<?> type, String format)
  {
    return new FormatterContextImpl(messageSupport, parameters, value, type, format, map)
        .delegateToNextFormatter();
  }


  @Override
  public @NotNull Text format(Message.WithSpaces message)
  {
    return message == null
        ? NULL
        : new TextPart(message.format(messageSupport, parameters), message.isSpaceBefore(),
            message.isSpaceAfter());
  }
}
