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
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPart;
import de.sayayi.lib.message.part.parameter.ParamConfig;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.value.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.sayayi.lib.message.formatter.ParameterFormatter.NULL_TYPE;
import static de.sayayi.lib.message.part.MessagePart.Text.NULL;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.NAME_TYPE;
import static java.util.Optional.ofNullable;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class FormatterContextImpl implements FormatterContext
{
  private final @NotNull MessageAccessor messageAccessor;
  private final @NotNull Parameters parameters;
  private final Object value;
  private final String format;
  private final @NotNull ParamConfig map;
  private final @NotNull ParameterFormatter[] parameterFormatters;
  private int parameterFormatterIndex = 0;


  public FormatterContextImpl(@NotNull MessageAccessor messageAccessor,
                              @NotNull Parameters parameters, Object value, Class<?> type,
                              String format, @NotNull ParamConfig map)
  {
    this.messageAccessor = messageAccessor;
    this.parameters = parameters;
    this.value = value;
    this.format = format;
    this.map = map;

    if (type == null)
      type = value == null ? NULL_TYPE : value.getClass();

    parameterFormatters = messageAccessor.getFormatters(format, type);
  }


  @Override
  public @NotNull MessageAccessor getMessageSupport() {
    return messageAccessor;
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
  public @NotNull Set<String> getParameterNames() {
    return parameters.getParameterNames();
  }


  @Override
  public boolean hasConfigMapMessage(@NotNull ConfigKey.Type keyType) {
    return map.hasEntryWithKeyType(keyType);
  }


  @Override
  public @NotNull Optional<Message.WithSpaces> getConfigMapMessage(
      Object key, @NotNull Set<ConfigKey.Type> keyTypes, boolean includeDefault)
  {
    return ofNullable(
        map.getMessage(messageAccessor, key, parameters.getLocale(), keyTypes, includeDefault));
  }


  @Override
  public @NotNull Optional<ConfigValue> getConfigValue(@NotNull String name)
  {
    final ConfigValue configValue =
        map.find(messageAccessor, name, parameters.getLocale(), NAME_TYPE, null);

    return configValue != null
        ? Optional.of(configValue)
        : ofNullable(messageAccessor.getDefaultParameterConfig(name));
  }


  @Override
  public @NotNull Optional<String> getConfigValueString(@NotNull String name)
  {
    final ConfigValueString string = (ConfigValueString)
        map.find(messageAccessor, name, parameters.getLocale(), NAME_TYPE, ConfigValue.STRING_TYPE);

    if (string != null)
      return Optional.of(string.asObject());

    final ConfigValue configValue = messageAccessor.getDefaultParameterConfig(name);

    return configValue instanceof ConfigValueString
        ? Optional.of(((ConfigValueString)configValue).asObject())
        : Optional.empty();
  }


  @Override
  public @NotNull OptionalLong getConfigValueNumber(@NotNull String name)
  {
    final ConfigValueNumber number = (ConfigValueNumber)
        map.find(messageAccessor, name, parameters.getLocale(), NAME_TYPE, ConfigValue.NUMBER_TYPE);

    if (number != null)
      return OptionalLong.of(number.asObject());

    final ConfigValue configValue = messageAccessor.getDefaultParameterConfig(name);

    return configValue instanceof ConfigValueNumber
        ? OptionalLong.of(((ConfigValueNumber)configValue).asObject())
        : OptionalLong.empty();
  }


  @Override
  public @NotNull Optional<Boolean> getConfigValueBool(@NotNull String name)
  {
    final ConfigValueBool bool = (ConfigValueBool)
        map.find(messageAccessor, name, parameters.getLocale(), NAME_TYPE, ConfigValue.BOOL_TYPE);

    if (bool != null)
      return Optional.of(bool.asObject());

    final ConfigValue configValue = messageAccessor.getDefaultParameterConfig(name);

    return configValue instanceof ConfigValueBool
        ? Optional.of(((ConfigValueBool)configValue).asObject())
        : Optional.empty();
  }


  @Override
  public @NotNull Optional<Message.WithSpaces> getConfigValueMessage(@NotNull String name)
  {
    final Message.WithSpaces message =
        map.getMessage(messageAccessor, name, parameters.getLocale(), NAME_TYPE, false);
    if (message != null)
      return Optional.of(message);

    final ConfigValue configValue = messageAccessor.getDefaultParameterConfig(name);

    if (configValue instanceof ConfigValueMessage)
      return Optional.of(((ConfigValueMessage)configValue).asObject());
    else if (configValue instanceof ConfigValueString)
    {
      return Optional.of(
          ((ConfigValueString)configValue).asMessage(messageAccessor.getMessageFactory()));
    }

    return Optional.empty();
  }


  @Override
  public @NotNull Text delegateToNextFormatter()
  {
    if (parameterFormatterIndex == parameterFormatters.length)
      throw new NoSuchElementException();

    return parameterFormatters[parameterFormatterIndex++].format(this, value);
  }


  @Override
  public @NotNull Text format(Object value, Class<?> type, boolean propagateFormat)
  {
    return new FormatterContextImpl(messageAccessor, parameters, value, type,
        propagateFormat ? format : null, map).delegateToNextFormatter();
  }


  @Override
  public @NotNull Text format(Object value, Class<?> type, String format)
  {
    return new FormatterContextImpl(messageAccessor, parameters, value, type, format, map)
        .delegateToNextFormatter();
  }


  @Override
  public @NotNull Text format(Message.WithSpaces message)
  {
    return message == null
        ? NULL
        : new TextPart(message.format(messageAccessor, parameters), message.isSpaceBefore(),
            message.isSpaceAfter());
  }


  @Override
  public @NotNull OptionalLong size(Object value)
  {
    if (value != null)
      for(ParameterFormatter formatter: messageAccessor.getFormatters(value.getClass()))
        if (formatter instanceof SizeQueryable)
        {
          OptionalLong result = ((SizeQueryable)formatter).size(this, value);
          if (result.isPresent())
            return result;
        }

    return OptionalLong.empty();
  }
}
