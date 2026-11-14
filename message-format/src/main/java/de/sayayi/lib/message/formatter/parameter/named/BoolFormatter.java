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


/**
 * Named parameter formatter that converts a parameter value to a boolean representation. It is also the type-based
 * formatter for {@link Boolean} and {@code boolean} values.
 * <p>
 * This formatter is selected in two ways:
 * <ul>
 *   <li>
 *     By name, using {@code format:bool} in a message parameter, e.g. {@code %{myParam,format:bool}}. It then accepts
 *     all types listed in {@link #canFormat(Class)} and converts the value to a boolean as described below.
 *   </li>
 *   <li>
 *     By type, without a {@code format} configuration, for {@link Boolean} and {@code boolean} values only (see
 *     {@link #getFormattableTypes()}). Values of other types are formatted by the formatter for their type and are not
 *     converted; a number, for example, is formatted as a number, and a string {@code "true"} as a string.
 *   </li>
 * </ul>
 * <p>
 * When selected by name, the value is converted to a boolean as follows:
 * <ul>
 *   <li>{@link Boolean} values are used directly</li>
 *   <li>
 *     {@link Number} values (including the primitive number types) are interpreted as {@code false} if zero,
 *     {@code true} otherwise; a number that is not finite ({@code NaN} or infinite) cannot be converted
 *   </li>
 *   <li>
 *     {@link String} values {@code "true"} and {@code "false"} are recognized literally; numeric strings are parsed and
 *     treated as numbers; other strings cannot be converted
 *   </li>
 *   <li>
 *     {@link java.util.Optional Optional}, {@link OptionalInt} and {@link OptionalLong} are unwrapped before
 *     conversion; an empty optional cannot be converted
 *   </li>
 *   <li>{@code null} values are handled separately using the null map key</li>
 * </ul>
 * A value that cannot be converted is matched against the empty map key only.
 * <p>
 * The resulting boolean value can be mapped to custom text using map keys of type {@code bool}, {@code empty} and
 * {@code null} in the parameter configuration. The bool keys are matched against the converted value, never against the
 * original value. String keys never match, even if their text is {@code true} or {@code false}. If no mapping is
 * provided, the formatter defaults to the text {@code "true"} or {@code "false"}.
 * <p>
 * <b>Examples:</b>
 * <p>
 * Any non-zero number is {@code true}, which makes this formatter useful for counters and legacy flag columns:
 * <pre>{@code
 * messageSupport
 *     .message("%{errors,format:bool,true:'failed',false:'passed'}")
 *     .with("errors", 4)
 *     .format();
 * // "failed"
 * }</pre>
 * <p>
 * Strings are recognized literally or parsed as a number. A value that cannot be converted to a boolean is matched by
 * the {@code empty} key, while {@code null} is matched by the {@code null} key:
 * <pre>{@code
 * var message = "%{flag,format:bool,true:'on',false:'off',empty:'invalid',null:'not set'}";
 *
 * messageSupport.message(message).with("flag", "0").format();
 * // "off"
 *
 * messageSupport.message(message).with("flag", "maybe").format();
 * // "invalid"
 *
 * messageSupport.message(message).with("flag", null).format();
 * // "not set"
 * }</pre>
 * <p>
 * Without map keys, the boolean value itself is formatted. Optional values are unwrapped first:
 * <pre>{@code
 * messageSupport
 *     .message("Enabled: %{enabled,format:bool}")
 *     .with("enabled", Optional.of(1))
 *     .format();
 * // "Enabled: true"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class BoolFormatter implements NamedParameterFormatter, MapKeyComparator<Object>
{
  /** Map key types used to look up a mapped message for a boolean value. */
  private static final Set<MapKey.Type> BOOL_KEY_TYPES = Set.of(EMPTY, NULL, BOOL);

  /** Default texts for {@code false} (index 0) and {@code true} (index 1). */
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


  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code bool} classifier.
   *
   * @return  always {@code true}
   */
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
   * Converts the given {@code value} to a boolean and formats it using the parameter map configuration. If the value is
   * {@code null}, the null map key is used. If the value cannot be converted to a boolean, the empty map key is used.
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


  /**
   * Formats a boolean value, using a mapped message if available or the default text otherwise.
   *
   * @param context  formatter context, not {@code null}
   * @param bool     boolean value to format
   *
   * @return  formatted text, never {@code null}
   */
  @Contract(pure = true)
  private @NotNull Text formatBool(@NotNull ParameterFormatterContext context, boolean bool)
  {
    return context
        .getMapMessage(bool, BOOL_KEY_TYPES)
        .map(context::format)
        .orElseGet(() -> BOOL_TEXT[bool ? 1 : 0]);
  }


  /**
   * Converts {@code value} into a boolean, unwrapping optionals and parsing numeric strings.
   *
   * @param value  value to convert, or {@code null}
   *
   * @return  optional containing the boolean value, or an empty optional if {@code value} cannot be converted, never
   *          {@code null}
   */
  @Contract(pure = true)
  private @NotNull Optional<Boolean> convertValueToBool(Object value)
  {
    if (value instanceof Optional<?> optional)
      value = optional.orElse(null);

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


  /**
   * Converts a number into a boolean: zero is {@code false}, any other finite value is {@code true}. A number that is
   * not finite ({@code NaN} or infinite) is neither {@code true} nor {@code false}.
   *
   * @param number  number to convert, not {@code null}
   *
   * @return  optional containing the boolean value, or an empty optional if {@code number} is not finite, never
   *          {@code null}
   */
  private @NotNull Optional<Boolean> convertNumberToBool(@NotNull Number number)
  {
    if (number instanceof Byte || number instanceof Short ||
        number instanceof Integer || number instanceof Long)
      return Optional.of(number.longValue() != 0);

    if (number instanceof BigInteger)
      return Optional.of(((BigInteger)number).signum() != 0);

    if (number instanceof BigDecimal)
      return Optional.of(((BigDecimal)number).signum() != 0);

    final var doubleValue = number.doubleValue();

    return Double.isFinite(doubleValue)
        ? Optional.of(doubleValue != 0)
        : Optional.empty();
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
