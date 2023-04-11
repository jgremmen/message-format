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
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.parameter.key.ConfigKey.Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

import static de.sayayi.lib.message.parameter.key.ConfigKey.EMPTY_NULL_TYPE;
import static de.sayayi.lib.message.parameter.key.ConfigKey.Type.*;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractParameterFormatter implements ParameterFormatter
{
  /** A set containing all data map key types, except for {@link Type#NAME}. */
  protected static final Set<Type> NO_NAME_KEY_TYPES =
      EnumSet.of(NULL, EMPTY, BOOL, NUMBER, STRING);


  @Override
  public @NotNull Text format(@NotNull FormatterContext formatterContext, Object value)
  {
    // handle empty, !empty, null and !null first
    Message.WithSpaces msg =
        formatterContext.getMapMessage(value, EMPTY_NULL_TYPE).orElse(null);
    if (msg != null)
      return formatterContext.format(msg);

    final Text text = formatValue(formatterContext, value);

    // handle empty, !empty, null and !null for result
    msg = formatterContext.getMapMessage(text.getText(), EMPTY_NULL_TYPE).orElse(null);

    return msg == null ? text : formatterContext.format(msg);
  }


  /**
   * Format {@code value} using {@code formatterContext}.
   * <p>
   * This method differs from {@link #format(FormatterContext, Object)} in that it has already
   * handled {@code empty} and {@code null} cases matched in the parameter configuration.
   * <p>
   * In the same way it will handle {@code empty} and {@code null} cases for the text returned
   * by this method.
   *
   * @param formatterContext  formatter context, not {@code null}
   * @param value             value to be formatted
   *
   * @return  formatted text, never {@code null}
   */
  protected abstract @NotNull Text formatValue(@NotNull FormatterContext formatterContext,
                                               Object value);


  @Contract(pure = true)
  protected @NotNull String trimNotNull(Text text)
  {
    if (text == null)
      return "";

    String s = text.getText();

    return s == null ? "" : s;
  }
}
