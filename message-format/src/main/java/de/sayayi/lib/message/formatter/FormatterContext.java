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
import de.sayayi.lib.message.MessageSupport.MessageSupportAccessor;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.parameter.key.ConfigKey;
import de.sayayi.lib.message.parameter.value.ConfigValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@SuppressWarnings("unused")
public interface FormatterContext extends Parameters
{
  /**
   * Returns the message context used to create this formatter context.
   *
   * @return  message context, never {@code null}
   */
  @Contract(pure = true)
  @NotNull MessageSupportAccessor getMessageSupport();


  @Contract(pure = true)
  @NotNull Optional<ConfigValue> getMapValue(Object key, @NotNull Set<ConfigKey.Type> keyTypes,
                                             Set<ConfigValue.Type> valueTypes);


  @Contract(pure = true)
  default @NotNull Optional<Message.WithSpaces> getMapMessage(Object key, @NotNull Set<ConfigKey.Type> keyTypes) {
    return getMapMessage(key, keyTypes, false);
  }


  @Contract(pure = true)
  @NotNull Optional<Message.WithSpaces> getMapMessage(Object key, @NotNull Set<ConfigKey.Type> keyTypes,
                                                      boolean includeDefault);


  @Contract(pure = true)
  default @NotNull Message.WithSpaces getMapMessageOrEmpty(Object key, @NotNull Set<ConfigKey.Type> keyTypes,
                                                           boolean includeDefault) {
    return getMapMessage(key, keyTypes, includeDefault).orElse(EmptyMessage.INSTANCE);
  }


  @Contract(pure = true)
  @NotNull Optional<ConfigValue> getConfigValue(@NotNull String name);


  @Contract(pure = true)
  @NotNull Optional<String> getConfigValueString(@NotNull String name);


  @Contract(pure = true)
  @NotNull OptionalLong getConfigValueNumber(@NotNull String name);


  @Contract(pure = true)
  @NotNull Optional<Boolean> getConfigValueBool(@NotNull String name);


  /**
   * Delegate formatting to next best parameter formatter.
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
   * A named formatter never has a next formatter it can delegate to.
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
   * @param value            value to format
   * @param type             value type or {@code null}
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
}
