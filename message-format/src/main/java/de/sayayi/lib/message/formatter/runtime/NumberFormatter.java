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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
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

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.NUMBER_TYPE;
import static java.lang.Math.signum;


/**
 * @author Jeroen Gremmen
 */
public final class NumberFormatter
    extends AbstractParameterFormatter<Number>
    implements ConfigKeyComparator<Number>
{
  private static final Map<Locale,DecimalFormatSymbols> FORMAT_SYMBOLS_CACHE = new ConcurrentHashMap<>();


  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Number number)
  {
    // check configuration map for match
    var msg = context
        .getConfigMapMessage(number, NUMBER_TYPE)
        .orElse(null);
    if (msg != null)
      return context.format(msg);

    var format = context
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
    var locale = parameters.getLocale();

    if ("integer".equals(format))
      return NumberFormat.getIntegerInstance(locale);

    if ("percent".equals(format))
      return NumberFormat.getPercentInstance(locale);

    if ("currency".equals(format))
      return NumberFormat.getCurrencyInstance(locale);

    if (format != null && !format.isEmpty())
    {
      return new DecimalFormat(format,
          FORMAT_SYMBOLS_CACHE.computeIfAbsent(locale, DecimalFormatSymbols::new));
    }

    return NumberFormat.getNumberInstance(locale);
  }


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


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("number");
  }


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


  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Number number, @NotNull ComparatorContext context)
  {
    var numberKeyValue = context.getNumberKeyValue();
    var compareType = context.getCompareType();

    if (number instanceof Byte || number instanceof Short ||
        number instanceof Integer || number instanceof Long)
      return compareType.match(Long.compare(number.longValue(), numberKeyValue)) ? EXACT : MISMATCH;

    if (number instanceof BigInteger)
      return compareType.match(((BigInteger)number).compareTo(BigInteger.valueOf(numberKeyValue))) ? EXACT : MISMATCH;

    if (number instanceof BigDecimal)
      return compareType.match(((BigDecimal)number).compareTo(BigDecimal.valueOf(numberKeyValue))) ? EXACT : MISMATCH;

    return compareType.match(Double.compare(number.doubleValue(), numberKeyValue)) ? EXACT : MISMATCH;
  }


  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Number value, @NotNull ComparatorContext context)
  {
    if (value instanceof Byte || value instanceof Short ||
        value instanceof Integer || value instanceof Long)
      value = BigInteger.valueOf(value.longValue());
    else if (value instanceof Double || value instanceof Float)
      value = BigDecimal.valueOf(value.doubleValue());

    var compareType = context.getCompareType();
    var string = context.getStringKeyValue();

    if (value instanceof BigInteger)
    {
      var bigint = (BigInteger)value;

      try {
        return compareType.match(bigint.compareTo(new BigInteger(string))) ? EQUIVALENT : MISMATCH;
      } catch(NumberFormatException ex) {
        value = new BigDecimal(bigint);
      }
    }

    if (value instanceof BigDecimal)
    {
      try {
        return compareType.match(((BigDecimal)value).compareTo(new BigDecimal(string))) ? EQUIVALENT : MISMATCH;
      } catch(NumberFormatException ignored) {
      }
    }

    return MISMATCH;
  }
}
