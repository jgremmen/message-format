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
import de.sayayi.lib.message.part.MessagePart.Config;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.Set;


/**
 * Provides a parameter formatter with message and parameter context, and operations for formatting values, measuring
 * their size, and resolving their classifiers.
 *
 * @see ParameterFormatter#format(ParameterFormatterContext, Object)
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public interface ParameterFormatterContext extends Parameters, ConfigAccessor, MapAccessor
{
  /**
   * Returns the message accessor used to create this formatter context.
   *
   * @return  message accessor, never {@code null}
   */
  @Contract(pure = true)
  @NotNull MessageAccessor getMessageAccessor();


  /**
   * Delegates formatting to the next applicable formatter in the type-based formatter chain. A formatter selected
   * explicitly by name has no next formatter unless it is also available through type-based resolution.
   *
   * @return  formatted text, never {@code null}
   *
   * @throws NoSuchElementException  if no next formatter is available
   *
   * @see NamedParameterFormatter
   */
  @NotNull Text delegateToNextFormatter();


  /**
   * Formats {@code value} using its runtime type and the current parameter context.
   *
   * @param value  value to format, or {@code null}
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text format(Object value);


  /**
   * Formats {@code value} using the specified {@code type} and the current parameter context.
   *
   * @param value  value to format, or {@code null}
   * @param type   type to use for formatter selection, not {@code null}
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text format(Object value, @NotNull Class<?> type);


  /**
   * Formats {@code value} using the specified type, formatter name, and parameter configuration.
   * <p>
   * If {@code type} is {@code null}, the type is determined from {@code value}. If {@code config} is {@code null},
   * the current parameter configuration is used.
   *
   * @param value   value to format, or {@code null}
   * @param type    type to use for formatter selection, or {@code null} to use the value's type
   * @param format  formatter name, or {@code null} to use type-based selection
   * @param config  parameter configuration, or {@code null} to use the current configuration
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text format(Object value, Class<?> type, String format, Config config);


  /**
   * Formats a nested {@code message} using the current message and parameter context.
   *
   * @param message  message to format, or {@code null}
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text format(Message.WithSpaces message);


  /**
   * Returns the size of {@code value} when a suitable formatter can determine it.
   *
   * @param value  value to measure, or {@code null}
   *
   * @return  the size when available, or {@link OptionalLong#empty()} when it cannot be determined
   */
  @Contract(pure = true)
  @NotNull OptionalLong size(Object value);


  /**
   * Resolves the classifiers that describe {@code value}.
   *
   * @param value   value to classify, or {@code null}
   * @param config  parameter configuration to use, or {@code null} to use the current configuration
   *
   * @return  classifier names, never {@code null}
   *
   * @since 0.21.0
   */
  @Contract(pure = true)
  @NotNull Set<String> getClassifiers(Object value, Config config);
}
