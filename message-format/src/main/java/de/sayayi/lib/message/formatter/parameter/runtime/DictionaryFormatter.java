/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Dictionary;
import java.util.Set;

import static de.sayayi.lib.message.formatter.FormattableType.DEFAULT_ORDER;
import static de.sayayi.lib.message.part.MapKey.EMPTY_NULL_TYPE;


/**
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public final class DictionaryFormatter implements ParameterFormatter
{
  @Override
  public @NotNull Text format(@NotNull ParameterFormatterContext context, Object _dictionary)
  {
    // delegate to next formatter if no config string entry with name "key" is present
    final var key = context.getConfigValue("key");
    if (key.isEmpty())
      return context.delegateToNextFormatter();

    final var dictionary = (Dictionary<?,?>)_dictionary;
    final var value = switch(key.get()) {
      case TypedValueBool bool -> dictionary.get(bool.asObject());
      case TypedValueNumber number -> getValueByNumber(dictionary, number.longValue());
      case TypedValueString string -> getValueByString(dictionary, string.stringValue());
      default -> null;
    };

    if (value == null)
    {
      return context
          .getMapMessage(null, EMPTY_NULL_TYPE)
          .map(context::format)
          .orElse(Text.NULL);
    }

    return context.format(value);
  }


  @Contract(pure = true)
  private Object getValueByString(@NotNull Dictionary<?,?> dictionary, @NotNull String string)
  {
    Object value;

    if ((value = dictionary.get(string)) == null && string.length() == 1)
      value = dictionary.get(string.charAt(0));

    return value;
  }


  @Contract(pure = true)
  private Object getValueByNumber(@NotNull Dictionary<?,?> dictionary, long number)
  {
    Object value;

    if ((value = dictionary.get(number)) == null)
    {
      if ((value = dictionary.get(BigInteger.valueOf(number))) == null)
        value = dictionary.get(BigDecimal.valueOf(number));

      if (value == null && number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE)
        value = dictionary.get((int)number);

      if (value == null && number >= Short.MIN_VALUE && number <= Short.MAX_VALUE)
        value = dictionary.get((short)number);

      if (value == null && number >= Byte.MIN_VALUE && number <= Byte.MAX_VALUE)
        value = dictionary.get((byte)number);
    }

    return value;
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    // insert before MapFormatter!
    return Set.of(new FormattableType(Dictionary.class, DEFAULT_ORDER - 10));
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("key");
  }
}
