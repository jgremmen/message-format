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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.EQ;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.NE;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.*;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Math.signum;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class BoolFormatter implements NamedParameterFormatter, ConfigKeyComparator<Object>
{
  private static final Set<ConfigKey.Type> BOOL_KEY_TYPES = unmodifiableSet(EnumSet.of(
      ConfigKey.Type.EMPTY,
      ConfigKey.Type.NULL,
      ConfigKey.Type.BOOL,
      ConfigKey.Type.STRING
  ));

  private static final Text[] BOOL_TEXT = new Text[] {
      noSpaceText("false"),
      noSpaceText("true")
  };


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
        type == int.class ||
        type == long.class ||
        type == short.class ||
        type == byte.class ||
        type == double.class ||
        type == float.class ||
        type == String.class ||
        type == Optional.class ||
        type == OptionalInt.class || type == OptionalLong.class ||
        type == NULL_TYPE;
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text format(@NotNull FormatterContext context, Object value)
  {
    // null value -> map or return null string
    if (value == null)
      return formatNull(context);

    // map true/false to mapped value or return its string representation
    // invalid value -> map or return empty string
    return convertValueToBool(value)
        .map(bool -> formatBool(context, bool))
        .orElseGet(() -> formatEmpty(context));
  }


  @Contract(pure = true)
  private @NotNull Text formatBool(@NotNull FormatterContext context, boolean bool)
  {
    return context
        .getConfigMapMessage(bool, BOOL_KEY_TYPES)
        .map(context::format)
        .orElseGet(() -> BOOL_TEXT[bool ? 1 : 0]);
  }


  @Contract(pure = true)
  private @NotNull Optional<Boolean> convertValueToBool(Object value)
  {
    // unwrap optional
    if (value instanceof Optional)
      value = ((Optional<?>)value).orElse(null);

    if (value instanceof Boolean)
      return Optional.of((Boolean)value);

    if (value instanceof String)
    {
      final String s = (String)value;

      if ("true".equals(s))
        return Optional.of(TRUE);
      else if ("false".equals(s))
        return Optional.of(FALSE);

      try {
        value = new BigDecimal(s);
      } catch(NumberFormatException ignored) {
      }
    }

    if (value instanceof Number)
      return convertNumberToBool((Number)value);

    if (value instanceof OptionalInt && ((OptionalInt)value).isPresent())
      return Optional.of(((OptionalInt)value).getAsInt() != 0);

    if (value instanceof OptionalLong && ((OptionalLong)value).isPresent())
      return Optional.of(((OptionalLong)value).getAsLong() != 0);

    return Optional.empty();
  }


  private @NotNull Optional<Boolean> convertNumberToBool(@NotNull Number number)
  {
    if (number instanceof Byte || number instanceof Short ||
        number instanceof Integer || number instanceof Long)
      return Optional.of(number.longValue() != 0);

    if (number instanceof BigInteger)
      return Optional.of(((BigInteger)number).signum() != 0);

    if (number instanceof BigDecimal)
      return Optional.of(((BigDecimal)number).signum() != 0);

    return Optional.of(signum(number.doubleValue()) != 0);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set with supported java types for this formatter, not {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return new HashSet<>(asList(
        new FormattableType(Boolean.class),
        new FormattableType(boolean.class)));
  }


  @Override
  public @NotNull MatchResult compareToConfigKey(@NotNull Object value,
                                                 @NotNull ComparatorContext context)
  {
    switch(context.getKeyType())
    {
      case NULL:
        return compareToNullKey(context.getCompareType());

      case EMPTY:
        return compareToEmptyKey(context.getCompareType());

      case BOOL:
        return compareToBoolKey(value, context.getBoolKeyValue());

      case STRING:
        return compareToStringKey(value, context.getCompareType(), context.getStringKeyValue());
    }

    return MISMATCH;
  }


  private @NotNull MatchResult compareToNullKey(@NotNull CompareType compareType) {
    return compareType.match(1) ? TYPELESS_EXACT : MISMATCH;
  }


  private @NotNull MatchResult compareToEmptyKey(@NotNull CompareType compareType) {
    return compareType.match(1) ? TYPELESS_LENIENT : MISMATCH;
  }


  private @NotNull MatchResult compareToBoolKey(Object value, boolean bool)
  {
    if (value instanceof Optional)
      value = ((Optional<?>)value).orElse(null);

    // boolean object for automatic formatter selection
    if (value instanceof Boolean)
      return ((Boolean)value) == bool ? EXACT : MISMATCH;

    // string and number objects for forced 'bool' formatter
    if (value instanceof String)
    {
      final String s = (String)value;

      if ("true".equals(s))
        return bool ? EQUIVALENT : MISMATCH;
      else if ("false".equals(s))
        return bool ? MISMATCH : EQUIVALENT;

      try {
        value = new BigDecimal(s);
      } catch(NumberFormatException ignored) {
      }
    }

    if (value instanceof Number)
      return compareNumberToBoolKey((Number)value, bool);

    if (value instanceof OptionalInt && ((OptionalInt)value).isPresent())
      return (((OptionalInt)value).getAsInt() != 0) == bool ? LENIENT : MISMATCH;

    if (value instanceof OptionalLong && ((OptionalLong)value).isPresent())
      return (((OptionalLong)value).getAsLong() != 0) == bool ? LENIENT : MISMATCH;

    return MISMATCH;
  }


  private @NotNull MatchResult compareNumberToBoolKey(Number number, boolean bool)
  {
    if (number instanceof Byte || number instanceof Short ||
        number instanceof Integer || number instanceof Long)
      return (number.longValue() != 0) == bool ? LENIENT : MISMATCH;

    if (number instanceof BigInteger)
      return (((BigInteger)number).signum() != 0) == bool ? LENIENT : MISMATCH;

    if (number instanceof BigDecimal)
      return (((BigDecimal)number).signum() != 0) == bool ? LENIENT : MISMATCH;

    return (signum(number.doubleValue()) != 0) == bool ? LENIENT : MISMATCH;
  }


  private @NotNull MatchResult compareToStringKey(Object value, @NotNull CompareType compareType,
                                                  @NotNull String string)
  {
    if (compareType == EQ || compareType == NE)
    {
      boolean bool;

      if ("true".equals(string))
        bool = true;
      else if ("false".equals(string))
        bool = false;
      else
        return MISMATCH;

      if (value instanceof Boolean)
        return compareBool(EQUIVALENT, (Boolean)value, compareType, bool);

      if ("true".equals(value))
        return compareBool(EXACT, true, compareType, bool);

      if ("false".equals(value))
        return compareBool(EXACT, false, compareType, bool);
    }

    return MISMATCH;
  }


  @Contract(pure = true)
  private @NotNull MatchResult compareBool(@NotNull MatchResult precision, boolean value,
                                           @NotNull CompareType compareType, boolean bool)
  {
    return compareType == EQ
        ? value == bool ? precision : MISMATCH
        : value != bool ? precision : MISMATCH;
  }
}
