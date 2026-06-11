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
package de.sayayi.lib.message.util;


import de.sayayi.lib.message.Message.Parameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;


/**
 * Utility class that provides type-safe conversions for
 * {@linkplain Parameters#getParameterValue(String) parameter values}. Each method retrieves a named parameter value
 * from a {@link Parameters} instance and attempts to convert it to the requested type, returning an {@link Optional}
 * (or {@link OptionalInt}/{@link OptionalLong}) that is empty when the value is {@code null} or cannot be converted.
 * <p>
 * This class backs the default convenience methods on {@link Parameters} such as
 * {@link Parameters#getParameterValueAsBoolean(String)},
 * {@link Parameters#getParameterValueAsInt(String)},
 * {@link Parameters#getParameterValueAsLong(String)},
 * {@link Parameters#getParameterValueAsEnum(String, Class)} and
 * {@link Parameters#getParameterValueAsString(String)}.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@SuppressWarnings("IfCanBeSwitch")
public final class ParameterValueHelper
{
  private ParameterValueHelper() {}


  /**
   * Retrieves the named parameter value and converts it to a {@code Boolean}.
   * <p>
   * The conversion supports {@link Boolean} values directly as well as the strings {@code "true"} and {@code "false"}
   * (case-insensitive). Values wrapped in an {@link Optional} are unwrapped before conversion.
   *
   * @param parameters  parameters instance to retrieve the value from, not {@code null}
   * @param name        parameter name, not {@code null}
   *
   * @return  an {@link Optional} containing the boolean value, or an empty optional if the value is {@code null}
   *          or not convertible to a boolean
   */
  @Contract(pure = true)
  public static @NotNull Optional<Boolean> getBoolean(@NotNull Parameters parameters, @NotNull String name)
  {
    var value = parameters.getParameterValue(name);
    if (value != null)
    {
      if (value instanceof Optional<?> optional)
        value = optional.orElse(null);

      if (Boolean.TRUE == value)
        return Optional.of(true);

      if (Boolean.FALSE == value)
        return Optional.of(false);

      if (value instanceof String string)
      {
        if ("true".equalsIgnoreCase(string))
          return Optional.of(true);

        if ("false".equalsIgnoreCase(string))
          return Optional.of(false);
      }
    }

    return Optional.empty();
  }


  /**
   * Retrieves the named parameter value and converts it to an {@code int}.
   * <p>
   * The conversion supports {@link Number} subclasses, {@link BigInteger}, {@link BigDecimal}, numeric strings and
   * values wrapped in {@link Optional}, {@link OptionalInt} or {@link OptionalLong}. If the value overflows the
   * {@code int} range or cannot be parsed, an empty result is returned.
   *
   * @param parameters  parameters instance to retrieve the value from, not {@code null}
   * @param name        parameter name, not {@code null}
   *
   * @return  an {@link OptionalInt} containing the int value, or an empty optional if the value is {@code null}
   *          or not convertible to an int
   */
  @Contract(pure = true)
  public static @NotNull OptionalInt getInt(@NotNull Parameters parameters, @NotNull String name)
  {
    var value = getLong(parameters, name);
    if (value.isPresent())
    {
      final var longValue = value.getAsLong();
      if (longValue == (int)longValue)
        return OptionalInt.of((int)longValue);
    }

    return OptionalInt.empty();
  }


  /**
   * Retrieves the named parameter value and converts it to a {@code long}.
   * <p>
   * The conversion supports {@link Number} subclasses, {@link BigInteger}, {@link BigDecimal}, numeric strings and
   * values wrapped in {@link Optional}, {@link OptionalInt} or {@link OptionalLong}. If the value overflows the
   * {@code long} range or cannot be parsed, an empty result is returned.
   *
   * @param parameters  parameters instance to retrieve the value from, not {@code null}
   * @param name        parameter name, not {@code null}
   *
   * @return  an {@link OptionalLong} containing the long value, or an empty optional if the value is {@code null}
   *          or not convertible to a long
   */
  @Contract(pure = true)
  public static @NotNull OptionalLong getLong(@NotNull Parameters parameters, @NotNull String name)
  {
    var value = parameters.getParameterValue(name);
    if (value != null)
    {
      try {
        if (value instanceof OptionalLong optionalLong)
          return optionalLong;

        if (value instanceof OptionalInt optionalInt)
          return optionalInt.isPresent() ? OptionalLong.of(optionalInt.getAsInt()) : OptionalLong.empty();

        if (value instanceof Optional<?> optional)
          value = optional.orElse(null);

        if (value instanceof Double dbl)
          value = BigDecimal.valueOf(dbl);
        else if (value instanceof Float flt)
          value = BigDecimal.valueOf(flt);

        if (value instanceof BigDecimal bigDecimal)
          value = bigDecimal.toBigIntegerExact();

        if (value instanceof BigInteger bigInteger)
          return OptionalLong.of(bigInteger.longValueExact());

        if (value instanceof Number number)
          return OptionalLong.of(number.longValue());

        if (value instanceof String string)
          return OptionalLong.of(Long.parseLong(string));
      } catch(ArithmeticException | NumberFormatException ignored) {
      }
    }

    return OptionalLong.empty();
  }


  /**
   * Retrieves the named parameter value and converts it to an enum constant of the specified type.
   * <p>
   * If the value is already an instance of the given enum type it is returned directly. String values are matched
   * against enum constant names in a case-insensitive manner; additionally, underscores in enum names are treated as
   * interchangeable with hyphens. Values wrapped in an {@link Optional} are unwrapped before conversion.
   *
   * @param parameters  parameters instance to retrieve the value from, not {@code null}
   * @param name        parameter name, not {@code null}
   * @param enumType    the enum class to convert to, not {@code null}
   * @param <T>         the enum type
   *
   * @return  an {@link Optional} containing the matching enum constant, or an empty optional if the value is
   *          {@code null} or does not match any constant
   */
  @Contract(pure = true)
  @SuppressWarnings("unchecked")
  public static <T extends Enum<T>> @NotNull Optional<T> getEnum(@NotNull Parameters parameters,
                                                                 @NotNull String name,
                                                                 @NotNull Class<T> enumType)
  {
    var value = parameters.getParameterValue(name);
    if (value != null)
    {
      if (enumType.isInstance(value))
        return Optional.of((T)value);

      if (value instanceof Optional<?> optional)
        value = optional.orElse(null);

      if (value instanceof String string)
        for(T enumValue: enumType.getEnumConstants())
        {
          final var enumName = enumValue.name();

          if (enumName.equalsIgnoreCase(string) ||
              enumName.replace('_', '-').equalsIgnoreCase(string))
            return Optional.of(enumValue);
        }
    }

    return Optional.empty();
  }


  /**
   * Retrieves the named parameter value and converts it to a {@code String}.
   * <p>
   * The conversion supports any {@link CharSequence} value, which is converted to a string via
   * {@link CharSequence#toString()}. Values wrapped in an {@link Optional} are unwrapped before conversion.
   *
   * @param parameters  parameters instance to retrieve the value from, not {@code null}
   * @param name        parameter name, not {@code null}
   *
   * @return  an {@link Optional} containing the string value, or an empty optional if the value is {@code null}
   *          or not a {@link CharSequence}
   */
  @Contract(pure = true)
  public static @NotNull Optional<String> getString(@NotNull Parameters parameters, @NotNull String name)
  {
    var value = parameters.getParameterValue(name);
    if (value != null)
    {
      if (value instanceof Optional<?> optional)
        value = optional.orElse(null);

      if (value instanceof CharSequence charSequence)
        return Optional.of(charSequence.toString());
    }

    return Optional.empty();
  }
}
