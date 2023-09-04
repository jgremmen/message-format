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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.EQ;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.NE;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.NUMBER_TYPE;
import static java.lang.Math.signum;
import static java.util.Arrays.asList;


/**
 * @author Jeroen Gremmen
 */
public final class NumberFormatter
    extends AbstractParameterFormatter<Number>
    implements ConfigKeyComparator<Number>
{
  private static final Map<Locale,DecimalFormatSymbols> FORMAT_SYMBOLS_CACHE =
      new ConcurrentHashMap<>();


  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Number number)
  {
    // check configuration map for match
    final Message.WithSpaces msg = context
        .getConfigMapMessage(number, NUMBER_TYPE)
        .orElse(null);
    if (msg != null)
      return context.format(msg);

    final String format = context.getConfigValueString("number").orElse(null);

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
    final Locale locale = parameters.getLocale();

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
    return new HashSet<>(asList(
        new FormattableType(Number.class),
        new FormattableType(byte.class),
        new FormattableType(short.class),
        new FormattableType(int.class),
        new FormattableType(long.class),
        new FormattableType(float.class),
        new FormattableType(double.class)
    ));
  }


  @Override
  public @NotNull MatchResult compareToConfigKey(@NotNull Number value, @NotNull ComparatorContext context)
  {
    final CompareType compareType = context.getCompareType();

    switch(context.getKeyType())
    {
      case EMPTY:
        return context.getCompareType().match(1) ? TYPELESS_EXACT : MISMATCH;

      case BOOL:
        return compareToBoolKey(value, compareType, context.getBoolKeyValue());

      case NUMBER:
        return compareToNumberKey(value, compareType, context.getNumberKeyValue());

      case STRING:
        return compareToStringKey(value, compareType, context.getStringKeyValue());
    }

    return MISMATCH;
  }


  private @NotNull MatchResult compareToBoolKey(Number value, @NotNull CompareType compareType,
                                                boolean bool)
  {
    if (compareType == EQ || compareType == NE)
    {
      if (value instanceof Byte || value instanceof Short ||
          value instanceof Integer || value instanceof Long)
        return (value.longValue() != 0) == bool ? LENIENT : MISMATCH;
      else if (value instanceof BigInteger)
        return (((BigInteger)value).signum() != 0) == bool ? LENIENT : MISMATCH;
      else if (value instanceof BigDecimal)
        return (((BigDecimal)value).signum() != 0) == bool ? LENIENT : MISMATCH;
      else
        return (signum(value.doubleValue()) != 0) == bool ? LENIENT : MISMATCH;
    }

    return MISMATCH;
  }


  private @NotNull MatchResult compareToNumberKey(Number value, @NotNull CompareType compareType,
                                                  long number)
  {
    if (value instanceof Byte || value instanceof Short ||
        value instanceof Integer || value instanceof Long)
      return compareType.match(Long.compare(value.longValue(), number)) ? EXACT : MISMATCH;

    if (value instanceof BigInteger)
    {
      return compareType.match(((BigInteger)value).compareTo(BigInteger.valueOf(number)))
          ? EXACT : MISMATCH;
    }

    if (value instanceof BigDecimal)
    {
      return compareType.match(((BigDecimal)value).compareTo(BigDecimal.valueOf(number)))
          ? EXACT : MISMATCH;
    }

    return compareType.match(Double.compare(value.doubleValue(), number)) ? EXACT : MISMATCH;
  }


  private @NotNull MatchResult compareToStringKey(Number value, @NotNull CompareType compareType,
                                                  @NotNull String string)
  {
    if (value instanceof Byte || value instanceof Short ||
        value instanceof Integer || value instanceof Long)
      value = BigInteger.valueOf(value.longValue());
    else if (value instanceof Double || value instanceof Float)
      value = BigDecimal.valueOf(value.doubleValue());

    if (value instanceof BigInteger)
    {
      final BigInteger bigint = (BigInteger)value;

      try {
        return compareType.match(bigint.compareTo(new BigInteger(string))) ? EQUIVALENT : MISMATCH;
      } catch(NumberFormatException ex) {
        value = new BigDecimal(bigint);
      }
    }

    if (value instanceof BigDecimal)
    {
      try {
        return compareType.match(((BigDecimal)value).compareTo(new BigDecimal(string)))
            ? EQUIVALENT : MISMATCH;
      } catch(NumberFormatException ignored) {
      }
    }

    return MISMATCH;
  }
}
