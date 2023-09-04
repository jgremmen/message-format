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
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.part.parameter.value.*;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalLong;

import static java.util.Optional.ofNullable;


/**
 * @since 0.8.4  (extracted from ParameterFormatterContext)
 */
abstract class AbstractParameterConfigAccessor implements ParameterConfigAccessor
{
  protected final @NotNull MessageAccessor messageAccessor;
  protected final @NotNull ParameterConfig parameterConfig;


  protected AbstractParameterConfigAccessor(@NotNull MessageAccessor messageAccessor,
                                            @NotNull ParameterConfig parameterConfig)
  {
    this.messageAccessor = messageAccessor;
    this.parameterConfig = parameterConfig;
  }


  @Override
  public @NotNull Optional<ConfigValue> getConfigValue(@NotNull String name)
  {
    final ConfigValue configValue = parameterConfig.getConfigValue(name);

    return configValue != null
        ? Optional.of(configValue)
        : ofNullable(messageAccessor.getDefaultParameterConfig(name));
  }


  @Override
  public @NotNull Optional<String> getConfigValueString(@NotNull String name)
  {
    ConfigValue configValue = parameterConfig.getConfigValue(name);

    if (configValue instanceof ConfigValueString)
      return Optional.of(((ConfigValueString)configValue).stringValue());

    configValue = messageAccessor.getDefaultParameterConfig(name);

    return configValue instanceof ConfigValueString
        ? Optional.of(((ConfigValueString)configValue).asObject())
        : Optional.empty();
  }


  @Override
  public @NotNull OptionalLong getConfigValueNumber(@NotNull String name)
  {
    ConfigValue configValue = parameterConfig.getConfigValue(name);

    if (configValue instanceof ConfigValueNumber)
      return OptionalLong.of(((ConfigValueNumber)configValue).longValue());

    configValue = messageAccessor.getDefaultParameterConfig(name);

    return configValue instanceof ConfigValueNumber
        ? OptionalLong.of(((ConfigValueNumber)configValue).asObject())
        : OptionalLong.empty();
  }


  @Override
  public @NotNull Optional<Boolean> getConfigValueBool(@NotNull String name)
  {
    ConfigValue configValue = parameterConfig.getConfigValue(name);

    if (configValue instanceof ConfigValueBool)
      return Optional.of(((ConfigValueBool)configValue).booleanValue());

    configValue = messageAccessor.getDefaultParameterConfig(name);

    return configValue instanceof ConfigValueBool
        ? Optional.of(((ConfigValueBool)configValue).asObject())
        : Optional.empty();
  }


  @Override
  public @NotNull Optional<Message.WithSpaces> getConfigValueMessage(@NotNull String name)
  {
    ConfigValue configValue = parameterConfig.getConfigValue(name);
    if (configValue == null)
      configValue = messageAccessor.getDefaultParameterConfig(name);

    if (configValue instanceof ConfigValueMessage)
      return Optional.of(((ConfigValueMessage)configValue).asObject());
    else if (configValue instanceof ConfigValueString)
    {
      return Optional.of(
          ((ConfigValueString)configValue).asMessage(messageAccessor.getMessageFactory()));
    }

    return Optional.empty();
  }
}
