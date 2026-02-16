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
package de.sayayi.lib.message.internal.part.parameter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.internal.part.config.BaseConfigAccessor;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.NULL_TYPE;
import static de.sayayi.lib.message.part.MessagePart.Text.NULL;
import static de.sayayi.lib.message.part.TextPartFactory.addSpaces;
import static java.util.Optional.ofNullable;


/**
 * Parameter formatter context implementation.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
final class ParameterFormatterContextImpl extends BaseConfigAccessor
    implements ParameterFormatterContext
{
  private final @NotNull MessagePart.Map map;
  private final @NotNull Parameters parameters;
  private final Object value;
  private final String format;
  private final @NotNull ParameterFormatter[] parameterFormatters;
  private int parameterFormatterIndex = 0;


  ParameterFormatterContextImpl(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters,
                                Object value, Class<?> type, String format, @NotNull MessagePart.Config config,
                                @NotNull MessagePart.Map map)
  {
    super(messageAccessor, config);

    this.map = map;
    this.parameters = parameters;
    this.value = value;
    this.format = format;

    if (type == null)
      type = value == null ? NULL_TYPE : value.getClass();

    parameterFormatters = messageAccessor.getFormatters(format, type, config);
  }


  @Override
  public @NotNull MessagePart.Map getMap() {
    return map;
  }


  @Override
  public @NotNull MessageAccessor getMessageAccessor() {
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
  public boolean hasMapMessage(@NotNull MapKey.Type keyType) {
    return map.hasMessageWithKeyType(keyType);
  }


  @Override
  public @NotNull Optional<Message.WithSpaces> getMapMessage(
      Object key, @NotNull Set<MapKey.Type> keyTypes, boolean includeDefault) {
    return ofNullable(map.getMessage(messageAccessor, key, getLocale(), keyTypes, includeDefault, config));
  }


  @Override
  public @NotNull Text delegateToNextFormatter()
  {
    if (parameterFormatterIndex == parameterFormatters.length)
      throw new NoSuchElementException();

    return parameterFormatters[parameterFormatterIndex++].format(this, value);
  }


  @Override
  public @NotNull Text format(Object value)
  {
    // propagate current format and parameter config to the next formatter
    return new ParameterFormatterContextImpl(messageAccessor, parameters, value, null, format, config, map)
        .delegateToNextFormatter();
  }


  @Override
  public @NotNull Text format(Object value, @NotNull Class<?> type)
  {
    // propagate current format and parameter config to the next formatter
    return new ParameterFormatterContextImpl(messageAccessor, parameters, value, type, format, config, map)
        .delegateToNextFormatter();
  }


  @Override
  public @NotNull Text format(Object value, Class<?> type, String format, MessagePart.Config config)
  {
    return new ParameterFormatterContextImpl(messageAccessor, parameters, value, type, format,
        config == null ? this.config : config, map).delegateToNextFormatter();
  }


  @Override
  public @NotNull Text format(Message.WithSpaces message)
  {
    return message == null
        ? NULL
        : addSpaces(
            message.formatAsText(messageAccessor, parameters),
            message.isSpaceBefore(),
            message.isSpaceAfter());
  }


  @Override
  public @NotNull OptionalLong size(Object value)
  {
    if (value != null)
    {
      OptionalLong result;

      for(var formatter: messageAccessor.getFormatters(value.getClass(), config))
        if (formatter instanceof SizeQueryable &&
            (result = ((SizeQueryable)formatter).size(this, value)).isPresent())
          return result;
    }

    return OptionalLong.empty();
  }
}
