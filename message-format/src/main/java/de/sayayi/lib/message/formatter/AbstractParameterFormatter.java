/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.data.map.MapKey.Type;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.EMPTY_NULL_TYPE;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractParameterFormatter implements ParameterFormatter
{
  /**
   * A set containing all data map key types, except for {@link Type#NAME}.
   */
  protected static final Set<Type> NO_NAME_KEY_TYPES =
      EnumSet.of(Type.NULL, Type.EMPTY, Type.BOOL, Type.NUMBER, Type.STRING);


  @Override
  public @NotNull Text format(@NotNull FormatterContext formatterContext, Object value)
  {
    // handle empty, !empty, null and !null first
    final Message.WithSpaces msg = formatterContext.getMapMessage(value, EMPTY_NULL_TYPE).orElse(null);
    return msg != null ? formatterContext.format(msg) : formatValue(formatterContext, value);
  }


  protected abstract @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value);


  @Contract(pure = true)
  protected @NotNull Optional<Text> translate(@NotNull FormatterContext formatterContext, String value)
  {
    final Message.WithSpaces msg = formatterContext.getMapMessage(value,
        EnumSet.of(Type.STRING, Type.EMPTY)).orElse(null);
    return msg != null ? Optional.of(formatterContext.format(msg)) : Optional.empty();

  }


  @Contract(pure = true)
  protected @NotNull String trimNotNull(Text text)
  {
    if (text == null)
      return "";

    String s = text.getText();

    return s == null ? "" : s;
  }
}
