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
package de.sayayi.lib.message.formatter.parameter.named;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

import static de.sayayi.lib.message.part.MapKey.Type.*;


/**
 * Named parameter formatter that selects a message from a map based on the parameter value.
 * <p>
 * This formatter is selected by using the name {@code choice} in a message parameter, e.g.
 * {@code %{myParam,format:choice}}.
 * <p>
 * It matches the parameter value against the map keys defined in the parameter configuration. All key types are
 * supported: {@code null}, {@code empty}, {@code bool}, {@code number} and {@code string}. Each match is ranked by
 * accuracy (e.g. exact, equivalent, lenient) and the message associated with the best matching key is formatted and
 * returned. If no key matches, the default map entry is used. If no default is present either, an empty text is
 * returned.
 * <p>
 * Unlike other formatters, the choice formatter does not convert or format the value itself. Instead, it purely acts
 * as a selector that picks one of several mapped messages based on the value.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ChoiceFormatter implements NamedParameterFormatter
{
  private static final Set<MapKey.Type> KEY_TYPES = EnumSet.of(NULL, EMPTY, BOOL, NUMBER, STRING);


  /**
   * {@inheritDoc}
   *
   * @return  {@code "choice"}, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "choice";
  }


  /**
   * {@inheritDoc}
   * <p>
   * Matches the given {@code value} against the map keys in the parameter configuration and formats the associated
   * message. If no key matches, the default map entry is used. If no default is present, an empty text is returned.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value)
  {
    return context.format(context
        .getMapMessage(value, KEY_TYPES, true)
        .orElse(Message.WithSpaces.EMPTY));
  }
}
