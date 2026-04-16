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

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_BOOL;
import static de.sayayi.lib.message.part.MapKey.MatchResult;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.EXACT;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.MapKey.Type.*;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Math.signum;


/**
 * Named parameter formatter that converts a parameter value to a boolean representation.
 * <p>
 * This formatter is selected by using the name {@code bool} in a message parameter, e.g.
 * {@code %{myParam,format:bool}}.
 * <p>
 * It accepts a wide range of input types and converts them to a boolean value:
 * <ul>
 *   <li>{@link Boolean} values are used directly</li>
 *   <li>{@link Number} values are interpreted as {@code false} if zero, {@code true} otherwise</li>
 *   <li>
 *     {@link String} values {@code "true"} and {@code "false"} are recognized literally; numeric strings are parsed
 *     and treated as numbers
 *   </li>
 *   <li>
 *     {@link java.util.Optional Optional}, {@link OptionalInt} and {@link OptionalLong} are unwrapped before
 *     conversion
 *   </li>
 *   <li>{@code null} values are handled separately using the null map key</li>
 * </ul>
 * <p>
 * The resulting boolean value can be mapped to custom text using map keys of type {@code bool}, {@code string},
 * {@code empty} and {@code null} in the parameter configuration. If no mapping is provided, the formatter defaults
 * to the text {@code "true"} or {@code "false"}.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class BoolFormatter implements NamedParameterFormatter, MapKeyComparator<Object>
{
  private static final Set<MapKey.Type> BOOL_KEY_TYPES = Set.of(EMPTY, NULL, BOOL, STRING);

  private static final Text[] BOOL_TEXT = new Text[] {
      noSpaceText("false"),
      noSpaceText("true")
  };


  /**
   * {@inheritDoc}
   *
   * @return  {@code "bool"}, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "bool";
  }


  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier(CLASSIFIER_BOOL);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * This formatter can handle {@link Boolean}, {@link Number} (and primitive numeric types), {@link String},
   * {@link java.util.Optional Optional}, {@link OptionalInt}, {@link OptionalLong} and {@code null} values.
   */
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


  /**
   * {@inheritDoc}
   * <p>
   * Converts the given {@code value} to a boolean and formats it using the parameter map configuration. If the
   * value is {@code null}, the null map key is used. If the value cannot be converted to a boolean, the empty map
   * key is used.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value)
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
  private @NotNull Text formatBool(@NotNull ParameterFormatterContext context, boolean bool)
  {
    return context
        .getMapMessage(bool, BOOL_KEY_TYPES)
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

    if (value instanceof String string)
    {
      if ("true".equals(string))
        return Optional.of(TRUE);
      else if ("false".equals(string))
        return Optional.of(FALSE);

      try {
        value = new BigDecimal(string);
      } catch(NumberFormatException ignored) {
      }
    }

    if (value instanceof Number)
      return convertNumberToBool((Number)value);

    if (value instanceof OptionalInt optionalInt && optionalInt.isPresent())
      return Optional.of(optionalInt.getAsInt() != 0);

    if (value instanceof OptionalLong optionalLong && optionalLong.isPresent())
      return Optional.of(optionalLong.getAsLong() != 0);

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
   * @return  a set containing {@link Boolean} and {@code boolean} formattable types, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return Set.of(
        new FormattableType(Boolean.class),
        new FormattableType(boolean.class));
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull Object value, @NotNull ComparatorContext context) {
    return context.getCompareType().match((Boolean)value == context.getBoolKeyValue() ? 0 : 1) ? EXACT : MISMATCH;
  }
}
