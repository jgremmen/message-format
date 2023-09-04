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
package de.sayayi.lib.message.part.parameter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPart;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.sayayi.lib.message.formatter.ParameterFormatter.NULL_TYPE;
import static de.sayayi.lib.message.part.MessagePart.Text.NULL;
import static java.util.Optional.ofNullable;


/**
 * Parameter formatter context implementation.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ParameterFormatterContext
    extends AbstractParameterConfigAccessor
    implements FormatterContext
{
  private final @NotNull Parameters parameters;
  private final Object value;
  private final String format;
  private final @NotNull ParameterFormatter[] parameterFormatters;
  private int parameterFormatterIndex = 0;


  public ParameterFormatterContext(@NotNull MessageAccessor messageAccessor,
                                   @NotNull Parameters parameters, Object value, Class<?> type,
                                   String format, @NotNull ParameterConfig parameterConfig)
  {
    super(messageAccessor, parameterConfig);

    this.parameters = parameters;
    this.value = value;
    this.format = format;

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
    return parameterConfig.hasMessageWithKeyType(keyType);
  }


  @Override
  public @NotNull Optional<Message.WithSpaces> getConfigMapMessage(
      Object key, @NotNull Set<ConfigKey.Type> keyTypes, boolean includeDefault)
  {
    final Set<ConfigKey.Type> types = EnumSet.copyOf(keyTypes);
    types.remove(ConfigKey.Type.NAME);

    return ofNullable(parameterConfig.getMessage(messageAccessor, key, getLocale(), types, includeDefault));
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
    return new ParameterFormatterContext(messageAccessor, parameters, value, type,
        propagateFormat ? format : null, parameterConfig).delegateToNextFormatter();
  }


  @Override
  public @NotNull Text format(Object value, Class<?> type, String format)
  {
    return new ParameterFormatterContext(messageAccessor, parameters, value, type, format, parameterConfig)
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
