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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static java.util.Optional.ofNullable;


/**
 * Base implementation of {@link ConfigAccessor} that resolves configuration values from a {@link Config} instance,
 * falling back to default configuration values provided by the {@link MessageAccessor}.
 *
 * @author Jeroen Gremmen
 * @since 0.8.4 (extracted from ParameterFormatterContext)
 */
public class BaseConfigAccessor implements ConfigAccessor
{
  /** The message accessor used to resolve default configuration values. */
  protected final @NotNull MessageAccessor messageAccessor;

  /** The configuration to look up values from. */
  protected final @NotNull Config config;


  /**
   * Creates a new base config accessor with the given message accessor and configuration.
   *
   * @param messageAccessor  the message accessor for resolving defaults, not {@code null}
   * @param config           the configuration to look up values from, not {@code null}
   */
  public BaseConfigAccessor(@NotNull MessageAccessor messageAccessor, @NotNull Config config)
  {
    this.messageAccessor = messageAccessor;
    this.config = config;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Config getConfig() {
    return config;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Optional<TypedValue<?>> getConfigValue(@NotNull String name)
  {
    final var configValue = config.getConfigValue(name);

    return configValue != null
        ? Optional.of(configValue)
        : ofNullable(messageAccessor.getDefaultConfig(name));
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Optional<String> getConfigValueString(@NotNull String name)
  {
    return config.getConfigValue(name) instanceof StringValue cvs
        ? Optional.of(cvs.stringValue())
        : messageAccessor.getDefaultConfig(name) instanceof StringValue cvs
            ? Optional.of(cvs.stringValue())
            : Optional.empty();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull OptionalLong getConfigValueLong(@NotNull String name)
  {
    return config.getConfigValue(name) instanceof NumberValue cvn
        ? OptionalLong.of(cvn.longValue())
        : messageAccessor.getDefaultConfig(name) instanceof NumberValue cvn
            ? OptionalLong.of(cvn.longValue())
            : OptionalLong.empty();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull OptionalInt getConfigValueInt(@NotNull String name)
  {
    return config.getConfigValue(name) instanceof NumberValue cvn && isIntegerRange(cvn)
        ? OptionalInt.of(cvn.intValue())
        : messageAccessor.getDefaultConfig(name) instanceof NumberValue cvn && isIntegerRange(cvn)
            ? OptionalInt.of(cvn.intValue())
            : OptionalInt.empty();
  }


  /**
   * Checks whether the given number value fits within the range of an integer.
   *
   * @param numberValue  the number value to check, not {@code null}
   *
   * @return  {@code true} if the value fits in an integer, {@code false} otherwise
   */
  @Contract(pure = true)
  private boolean isIntegerRange(@NotNull NumberValue numberValue)
  {
    final var n = numberValue.longValue();

    return n >= Integer.MIN_VALUE && n <= Integer.MAX_VALUE;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Optional<Boolean> getConfigValueBool(@NotNull String name)
  {
    return config.getConfigValue(name) instanceof BoolValue cvb
        ? Optional.of(cvb.booleanValue())
        : messageAccessor.getDefaultConfig(name) instanceof BoolValue cvb
            ? Optional.of(cvb.booleanValue())
            : Optional.empty();
  }


  /**
   * {@inheritDoc}
   */
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


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull <T extends Enum<T>> Optional<T> getConfigValueEnum(@NotNull String name, @NotNull Class<T> enumType)
  {
    String value;

    Optional<T> enumValue =
        config.getConfigValue(name) instanceof StringValue stringValue &&
        !(value = stringValue.stringValue()).isEmpty()
            ? findEnumValue(value, enumType)
            : Optional.empty();

    if (enumValue.isEmpty())
    {
      enumValue =
          messageAccessor.getDefaultConfig(name) instanceof StringValue stringValue &&
          !(value = stringValue.stringValue()).isEmpty()
              ? findEnumValue(value, enumType)
              : Optional.empty();
    }

    return enumValue;
  }


  /**
   * Finds an enum constant matching the given string value. The match is case-insensitive and also supports
   * hyphenated names (e.g. "my-value" matches {@code MY_VALUE}).
   *
   * @param value     the string value to match against enum constant names, not {@code null}
   * @param enumType  the enum class to search, not {@code null}
   * @param <T>       the enum type
   *
   * @return  an optional containing the matching enum constant, or empty if no match is found
   */
  @Contract(pure = true)
  private <T extends Enum<T>> Optional<T> findEnumValue(@NotNull String value, @NotNull Class<T> enumType)
  {
    for(T enumValue: enumType.getEnumConstants())
    {
      final var enumName = enumValue.name();

      if (enumName.equalsIgnoreCase(value) ||
          enumName.replace('_', '-').equalsIgnoreCase(value))
        return Optional.of(enumValue);
    }

    return Optional.empty();
  }
}
