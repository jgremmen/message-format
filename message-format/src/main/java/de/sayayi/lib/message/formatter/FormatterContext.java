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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;


/**
 *
 * @see ParameterFormatter#format(FormatterContext, Object)
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public interface FormatterContext extends Parameters
{
  /**
   * Returns the message accessor used to create this formatter context.
   *
   * @return  message accessor, never {@code null}
   */
  @Contract(pure = true)
  @NotNull MessageAccessor getMessageSupport();


  /**
   * Tells whether the parameter configuration map contains an entry with the given {@code keyType}.
   *
   * @param keyType  entry key type to look for, not {@code null}
   *
   * @return  {@code true} if the map contains an entry with the given key type,
   *          {@code false} otherwise
   *
   * @see de.sayayi.lib.message.part.parameter.ParamConfig#hasEntryWithKeyType(ConfigKey.Type)
   *      ParamConfig#hasEntryWithKeyType(ConfigKey.Type)
   */
  @Contract(pure = true)
  boolean hasConfigMapMessage(@NotNull ConfigKey.Type keyType);


  @Contract(pure = true)
  default @NotNull Optional<Message.WithSpaces> getConfigMapMessage(
      Object key, @NotNull Set<ConfigKey.Type> keyTypes) {
    return getConfigMapMessage(key, keyTypes, false);
  }


  @Contract(pure = true)
  @NotNull Optional<Message.WithSpaces> getConfigMapMessage(
      Object key, @NotNull Set<ConfigKey.Type> keyTypes, boolean includeDefault);


  /**
   * Gets a configuration value for named key {@code name}.
   * <p>
   * The value is taken from the parameter configuration map. If no such key is found the
   * message support is queried for a default configuration value.
   *
   * @param name  parameter configuration key, not {@code null}
   *
   * @return  optional instance representing the found value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Optional<ConfigValue> getConfigValue(@NotNull String name);


  /**
   * Gets a string configuration value for named key {@code name}.
   * <p>
   * The value is taken from the parameter configuration map. If no such key is found the
   * message support is queried for a default configuration value.
   * <p>
   * If a value is found for the given {@code name} but the type is not a string, the method
   * returns {@link Optional#empty()}.
   *
   * @param name  parameter configuration key, not {@code null}
   *
   * @return  optional string instance representing the found value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Optional<String> getConfigValueString(@NotNull String name);


  /**
   * Gets a number configuration value for named key {@code name}.
   * <p>
   * The value is taken from the parameter configuration map. If no such key is found the
   * message support is queried for a default configuration value.
   * <p>
   * If a value is found for the given {@code name} but the type is not a number, the method
   * returns {@link Optional#empty()}.
   *
   * @param name  parameter configuration key, not {@code null}
   *
   * @return  optional number instance representing the found value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull OptionalLong getConfigValueNumber(@NotNull String name);


  /**
   * Gets a boolean configuration value for named key {@code name}.
   * <p>
   * The value is taken from the parameter configuration map. If no such key is found the
   * message support is queried for a default configuration value.
   * <p>
   * If a value is found for the given {@code name} but the type is not boolean, the method
   * returns {@link Optional#empty()}.
   *
   * @param name  parameter configuration key, not {@code null}
   *
   * @return  optional boolean instance representing the found value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Optional<Boolean> getConfigValueBool(@NotNull String name);


  /**
   * Gets a message configuration value for named key {@code name}.
   * <p>
   * The value is taken from the parameter configuration map. If no such key is found the
   * message support is queried for a default configuration value.
   * <p>
   * If a value is found for the given {@code name} but the type is not a string nor a message,
   * the method returns {@link Optional#empty()}.
   *
   * @param name  parameter configuration key, not {@code null}
   *
   * @return  optional message instance representing the found value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Optional<Message.WithSpaces> getConfigValueMessage(@NotNull String name);


  /**
   * Delegate formatting to the next best parameter formatter.
   * <p>
   * Based on the object type, format and registered formatters, a list of prioritized formatters
   * is calculated and the top formatter is invoked. If that formatter delegates formatting, the
   * next formatter from the list will be invoked with the same formatter context.
   * <p>
   * The last formatter from the list, which is usually the formatter associated with type
   * {@code Object}, must not delegate to the next formatter or a {@code NoSuchElementException}
   * is thrown. By default the {@code Object} formatter is a string formatter, which will never
   * delegate. However, if this formatter is redefined, it must never delegate formatting.
   * <p>
   * A named-only formatter never has a next formatter it can delegate to.
   *
   * @return  formatted text, never {@code null}
   */
  @NotNull Text delegateToNextFormatter();


  /**
   * Format the given {@code value} using its type.
   *
   * @param value  value to format
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  default @NotNull Text format(Object value) {
    return format(value, null, false);
  }


  /**
   * Format the given {@code value} using its type and the current format designator
   * (if available and if {@code propagateFormat = true}).
   *
   * @param value            value to format
   * @param propagateFormat  propagate parameter format designator if {@code true}, ignore format
   *                         designator if this parameter is {@code false}
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  default @NotNull Text format(Object value, boolean propagateFormat) {
    return format(value, null, propagateFormat);
  }


  /**
   * Format the given {@code value} using {@code type}.
   *
   * @param value  value to format
   * @param type   value type or {@code null}
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  default @NotNull Text format(Object value, @NotNull Class<?> type) {
    return format(value, type, false);
  }


  /**
   * Format the given {@code value} using {@code type} and the current format designator
   * (if available and if {@code propagateFormat = true}).
   *
   * @param value            value to format
   * @param type             value type or {@code null}
   * @param propagateFormat  propagate parameter format designator if {@code true}, ignore format
   *                         designator if this parameter is {@code false}
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text format(Object value, Class<?> type, boolean propagateFormat);


  /**
   * Format the given {@code value} using {@code type} and {@code format}.
   *
   * @param value   value to format
   * @param type    value type or {@code null}
   * @param format  formatter name
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text format(Object value, Class<?> type, String format);


  /**
   * Format the given {@code message}.
   *
   * @param message  message, or {@code null}
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text format(Message.WithSpaces message);


  /**
   * Determines the size of {@code value}.
   *
   * @param value  value to calculate size of or {@code null}
   *
   * @return  {@link OptionalLong#empty()} if {@code value} is {@code null} or if there's no
   *          suitable formatter capable of calculating the size. If the size has been calculated,
   *          {@link OptionalLong#getAsLong()} will return the value
   */
  @Contract(pure = true)
  @NotNull OptionalLong size(Object value);
}
