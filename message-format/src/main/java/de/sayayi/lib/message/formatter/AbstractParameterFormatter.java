/*
 * Copyright 2019 Jeroen Gremmen
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
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPartFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.Type.NUMBER;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.Type.STRING;
import static java.util.Optional.empty;


/**
 * @param <T>  parameter type
 *
 * @author Jeroen Gremmen
 * @since 0.3.1
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractParameterFormatter<T> implements ParameterFormatter
{
  @Override
  public final @NotNull Text format(@NotNull FormatterContext context, Object value)
  {
    // handle empty, !empty, null and !null first
    final Message.WithSpaces msg = context
        .getConfigMapMessage(value, EMPTY_NULL_TYPE)
        .orElse(null);

    if (msg != null)
      return context.format(msg);

    if (value == null)
      return nullText();

    //noinspection unchecked
    final Text text = formatValue(context, (T)value);

    // handle empty, !empty, null and !null for result
    return context
        .getConfigMapMessage(text.getText(), EMPTY_NULL_TYPE)
        .map(context::format)
        .orElse(text);
  }


  /**
   * Format {@code value} using {@code context}.
   * <p>
   * This method differs from {@link #format(FormatterContext, Object)} in that it has already
   * handled {@code empty} and {@code null} cases matched in the parameter configuration.
   * <p>
   * In the same way it will handle {@code empty} and {@code null} cases for the text returned
   * by this method.
   *
   * @param context  formatter context, not {@code null}
   * @param value    value to be formatted, not {@code null}
   *
   * @return  formatted text, never {@code null}
   */
  protected abstract @NotNull Text formatValue(@NotNull FormatterContext context,
                                               @NotNull T value);


  @Contract(pure = true)
  @SuppressWarnings("SameParameterValue")
  protected @NotNull Optional<Text> formatUsingMappedString(@NotNull FormatterContext context,
                                                            @NotNull String s,
                                                            boolean includeDefault)
  {
    if (context.hasConfigMapMessage(STRING))
    {
      return Optional.of(context
          .getConfigMapMessage(s, STRING_TYPE, includeDefault)
          .map(context::format)
          .orElseGet(TextPartFactory::emptyText));
    }

    return empty();
  }


  @Contract(pure = true)
  @SuppressWarnings("SameParameterValue")
  protected @NotNull Optional<Text> formatUsingMappedNumber(@NotNull FormatterContext context,
                                                            long n, boolean includeDefault)
  {
    if (context.hasConfigMapMessage(NUMBER))
    {
      return Optional.of(context
          .getConfigMapMessage(n, NUMBER_TYPE, includeDefault)
          .map(context::format)
          .orElseGet(TextPartFactory::emptyText));
    }

    return empty();
  }
}
