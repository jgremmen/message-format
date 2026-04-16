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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_NUMBER;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.*;
import static de.sayayi.lib.message.part.MapKey.NUMBER_TYPE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.util.MessageUtil.isEmpty;
import static java.lang.Math.signum;


/**
 * Parameter formatter for {@link Number} values and primitive numeric types.
 * <p>
 * The output format is controlled by the {@code number} configuration key, which accepts the following values:
 * <ul>
 *   <li>{@code integer} &ndash; formats the number as an integer</li>
 *   <li>{@code percent} &ndash; formats the number as a percentage</li>
 *   <li>{@code currency} &ndash; formats the number as a currency value</li>
 *   <li>{@code bool} &ndash; formats the number as a boolean (zero is {@code false}, non-zero is {@code true})</li>
 *   <li>A custom {@link DecimalFormat} pattern &ndash; formats the number using the given pattern</li>
 * </ul>
 * <p>
 * If the configuration key is absent, integer types are rendered using their plain {@code toString()} representation;
 * all other numeric types are formatted using a locale-specific number format.
 * <p>
 * Number map keys in the parameter configuration can be used to map specific numeric values to custom text.
 * <p>
 * Map key comparison supports {@code bool} keys (zero/non-zero), {@code number} keys (numeric comparison) and
 * {@code string} keys (parsed numeric comparison).
 *
 * @author Jeroen Gremmen
 */
public final class NumberFormatter
    extends AbstractParameterFormatter<Number>
    implements MapKeyComparator<Number>
{
  private static final Map<Locale,DecimalFormatSymbols> FORMAT_SYMBOLS_CACHE = new ConcurrentHashMap<>();


  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier(CLASSIFIER_NUMBER);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formats the number based on the {@code number} configuration key, or using number map keys if a match is found.
   * If no configuration is provided, integer types use their plain string representation while other types use a
   * locale-specific number format.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Number number)
  {
    // check configuration map for match
    final var msg = context
        .getMapMessage(number, NUMBER_TYPE)
        .orElse(null);
    if (msg != null)
      return context.format(msg);

    final var format = context
        .getConfigValueString("number")
        .orElse(null);

    // special case: show number as bool
    if ("bool".equals(format))
      return context.format(number, boolean.class);

    if ((format == null || "integer".equals(format)) &&
        (number instanceof BigInteger || number instanceof Long || number instanceof Integer ||
         number instanceof Short || number instanceof Byte || number instanceof AtomicInteger ||
         number instanceof AtomicLong || number instanceof LongAdder ||
         number instanceof LongAccumulator))
      return noSpaceText(number.toString());

    return noSpaceText(getFormatter(format, context).format(number));
  }


  private @NotNull NumberFormat getFormatter(String format, Parameters parameters)
  {
    final var locale = parameters.getLocale();

    if ("integer".equals(format))
      return NumberFormat.getIntegerInstance(locale);

    if ("percent".equals(format))
      return NumberFormat.getPercentInstance(locale);

    if ("currency".equals(format))
      return NumberFormat.getCurrencyInstance(locale);

    if (!isEmpty(format))
      return new DecimalFormat(format, FORMAT_SYMBOLS_CACHE.computeIfAbsent(locale, DecimalFormatSymbols::new));

    return NumberFormat.getNumberInstance(locale);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing formattable types for {@link Number} and all primitive numeric types, never {@code null}
   */
  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return Set.of(
        new FormattableType(Number.class),
        new FormattableType(byte.class),
        new FormattableType(short.class),
        new FormattableType(int.class),
        new FormattableType(long.class),
        new FormattableType(float.class),
        new FormattableType(double.class));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "number"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("number");
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull Number value, @NotNull ComparatorContext context)
  {
    final boolean bool;

    if (value instanceof Byte || value instanceof Short ||
        value instanceof Integer || value instanceof Long)
      bool = value.longValue() != 0;
    else if (value instanceof BigInteger)
      bool = ((BigInteger)value).signum() != 0;
    else if (value instanceof BigDecimal)
      bool = ((BigDecimal)value).signum() != 0;
    else
      bool = signum(value.doubleValue()) != 0;

    return context.getCompareType().match(bool == context.getBoolKeyValue() ? 0 : 1) ? LENIENT : MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Number number, @NotNull ComparatorContext context)
  {
    final var numberKeyValue = context.getNumberKeyValue();
    final var compareType = context.getCompareType();

    if (number instanceof Byte || number instanceof Short ||
        number instanceof Integer || number instanceof Long)
      return compareType.match(Long.compare(number.longValue(), numberKeyValue)) ? EXACT : MISMATCH;

    if (number instanceof BigInteger bigInteger)
      return compareType.match(bigInteger.compareTo(BigInteger.valueOf(numberKeyValue))) ? EXACT : MISMATCH;

    if (number instanceof BigDecimal bigDecimal)
      return compareType.match(bigDecimal.compareTo(BigDecimal.valueOf(numberKeyValue))) ? EXACT : MISMATCH;

    return compareType.match(Double.compare(number.doubleValue(), numberKeyValue)) ? EXACT : MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Number value, @NotNull ComparatorContext context)
  {
    if (value instanceof Byte || value instanceof Short ||
        value instanceof Integer || value instanceof Long)
      value = BigInteger.valueOf(value.longValue());
    else if (value instanceof Double || value instanceof Float)
      value = BigDecimal.valueOf(value.doubleValue());

    final var compareType = context.getCompareType();
    final var string = context.getStringKeyValue();

    if (value instanceof BigInteger bigInteger)
    {
      try {
        return compareType.match(bigInteger.compareTo(new BigInteger(string))) ? EQUIVALENT : MISMATCH;
      } catch(NumberFormatException ex) {
        value = new BigDecimal(bigInteger);
      }
    }

    if (value instanceof BigDecimal bigDecimal)
    {
      try {
        return compareType.match(bigDecimal.compareTo(new BigDecimal(string))) ? EQUIVALENT : MISMATCH;
      } catch(NumberFormatException ignored) {
      }
    }

    return MISMATCH;
  }
}
