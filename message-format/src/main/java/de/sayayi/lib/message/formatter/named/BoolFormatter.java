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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.EMPTY_NULL_TYPE;
import static de.sayayi.lib.message.data.map.MapKey.Type.BOOL;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Arrays.asList;


/**
 * @author Jeroen Gremmen
 */
public final class BoolFormatter extends AbstractParameterFormatter implements NamedParameterFormatter
{
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "bool";
  }


  @Override
  public boolean canFormat(@NotNull Class<?> type)
  {
    return
        type == Boolean.class || type == boolean.class ||
        Number.class.isAssignableFrom(type) ||
        type == long.class ||
        type == int.class ||
        type == short.class ||
        type == byte.class ||
        type == String.class ||
        type == NULL_TYPE;
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text format(@NotNull FormatterContext formatterContext, Object value)
  {
    Message.WithSpaces msg = formatterContext.getMapMessage(value, EMPTY_NULL_TYPE).orElse(null);
    if (msg != null)
      return formatterContext.format(msg);

    if (value == null)
      return nullText();

    boolean bool;

    if (value instanceof Boolean)
      bool = (Boolean)value;
    else if (value instanceof BigInteger)
      bool = ((java.math.BigInteger)value).signum() != 0;
    else if (value instanceof BigDecimal)
      bool = ((java.math.BigDecimal)value).signum() != 0;
    else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
      bool = ((Number)value).longValue() != 0;
    else if (value instanceof Number)
      bool = Math.signum(((Number)value).doubleValue()) != 0;
    else
      bool = Boolean.parseBoolean(String.valueOf(value));

    // allow custom messages for true/false value?
    if ((msg = formatterContext.getMapMessage(bool, EnumSet.of(BOOL)).orElse(null)) != null)
      return formatterContext.format(msg);

    return noSpaceText(Boolean.toString(bool));
  }


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value) {
    throw new IllegalStateException();
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return new HashSet<>(asList(
        new FormattableType(Boolean.class),
        new FormattableType(boolean.class, 125)));
  }
}
