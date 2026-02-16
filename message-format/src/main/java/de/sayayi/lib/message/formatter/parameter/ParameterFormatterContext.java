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
package de.sayayi.lib.message.formatter.parameter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.part.ConfigAccessor;
import de.sayayi.lib.message.part.MapAccessor;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;


/**
 * The formatter context provides a parameter formatter with all context information it requires
 * to format a parameter part.
 *
 * @see ParameterFormatter#format(ParameterFormatterContext, Object)
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public interface ParameterFormatterContext extends Parameters, ConfigAccessor, MapAccessor
{
  /**
   * Returns the message accessor used to create this formatter context.
   *
   * @return  message accessor, never {@code null}
   *
   * @since 0.8.0 (renamed in 0.9.2)
   */
  @Contract(pure = true)
  @NotNull MessageAccessor getMessageAccessor();


  /**
   * Delegate formatting to the next best parameter formatter.
   * <p>
   * Based on the object type, format and registered formatters, a list of prioritized formatters
   * is calculated and the top formatter is invoked. If that formatter delegates formatting, the
   * next formatter from the list will be invoked with the same formatter context.
   * <p>
   * The last formatter from the list, which is usually the formatter associated with type
   * {@code Object}, must not delegate to the next formatter or a {@code NoSuchElementException}
   * is thrown. By default, the {@code Object} formatter is a string formatter, which will never
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
  @NotNull Text format(Object value);


  /**
   * Format the given {@code value} using {@code type}.
   *
   * @param value  value to format
   * @param type   value type
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text format(Object value, @NotNull Class<?> type);


  /**
   * Format the given {@code value} using {@code type}, {@code format} and {@code config}.
   * <p>
   * If {@code type} is {@code null}, it will be determined by analyzing {@code value}.
   * If {@code config} is null, the current parameter configuration map is used.
   *
   * @param value   value to format
   * @param type    value type
   * @param format  formatter name
   * @param config  parameter config instance
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text format(Object value, Class<?> type, String format, MessagePart.Config config);


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
