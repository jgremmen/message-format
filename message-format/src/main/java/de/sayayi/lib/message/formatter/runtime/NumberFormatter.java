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
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
import java.util.concurrent.atomic.LongAdder;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.NUMBER_TYPE;
import static java.util.Arrays.asList;


/**
 * @author Jeroen Gremmen
 */
public final class NumberFormatter extends AbstractParameterFormatter<Number>
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
         number instanceof AtomicLong || number instanceof LongAdder))
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
}