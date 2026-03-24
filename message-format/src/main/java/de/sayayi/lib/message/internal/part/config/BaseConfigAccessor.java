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
package de.sayayi.lib.message.internal.part.config;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.part.ConfigAccessor;
import de.sayayi.lib.message.part.MessagePart.Config;
import de.sayayi.lib.message.part.TypedValue;
import de.sayayi.lib.message.part.TypedValue.BoolValue;
import de.sayayi.lib.message.part.TypedValue.MessageValue;
import de.sayayi.lib.message.part.TypedValue.NumberValue;
import de.sayayi.lib.message.part.TypedValue.StringValue;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalLong;

import static java.util.Optional.ofNullable;


/**
 * @since 0.8.4  (extracted from ParameterFormatterContext)
 */
public class BaseConfigAccessor implements ConfigAccessor
{
  protected final @NotNull MessageAccessor messageAccessor;
  protected final @NotNull Config config;


  public BaseConfigAccessor(@NotNull MessageAccessor messageAccessor, @NotNull Config config)
  {
    this.messageAccessor = messageAccessor;
    this.config = config;
  }


  @Override
  public @NotNull Config getConfig() {
    return config;
  }


  @Override
  public @NotNull Optional<TypedValue<?>> getConfigValue(@NotNull String name)
  {
    final var configValue = config.getConfigValue(name);

    return configValue != null
        ? Optional.of(configValue)
        : ofNullable(messageAccessor.getDefaultConfig(name));
  }


  @Override
  public @NotNull Optional<String> getConfigValueString(@NotNull String name)
  {
    return config.getConfigValue(name) instanceof StringValue cvs
        ? Optional.of(cvs.stringValue())
        : messageAccessor.getDefaultConfig(name) instanceof StringValue cvs
            ? Optional.of(cvs.stringValue())
            : Optional.empty();
  }


  @Override
  public @NotNull OptionalLong getConfigValueNumber(@NotNull String name)
  {
    return config.getConfigValue(name) instanceof NumberValue cvn
        ? OptionalLong.of(cvn.longValue())
        : messageAccessor.getDefaultConfig(name) instanceof NumberValue cvn
            ? OptionalLong.of(cvn.longValue())
            : OptionalLong.empty();
  }


  @Override
  public @NotNull Optional<Boolean> getConfigValueBool(@NotNull String name)
  {
    return config.getConfigValue(name) instanceof BoolValue cvb
        ? Optional.of(cvb.booleanValue())
        : messageAccessor.getDefaultConfig(name) instanceof BoolValue cvb
            ? Optional.of(cvb.booleanValue())
            : Optional.empty();
  }


  @Override
  public @NotNull Optional<Message.WithSpaces> getConfigValueMessage(@NotNull String name)
  {
    var configValue = config.getConfigValue(name);
    if (configValue == null)
      configValue = messageAccessor.getDefaultConfig(name);

    if (configValue instanceof MessageValue messageValue)
      return Optional.of(messageValue.messageValue());
    else if (configValue instanceof StringValue stringValue)
      return Optional.of(stringValue.asMessage(messageAccessor.getMessageFactory()));

    return Optional.empty();
  }
}
