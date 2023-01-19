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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
import java.util.concurrent.atomic.LongAdder;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class NumberFormatter extends AbstractParameterFormatter
{
  private static final BoolFormatter BOOL_FORMATTER = new BoolFormatter();

  private static final Map<Locale,DecimalFormatSymbols> FORMAT_SYMBOLS_CACHE = new ConcurrentHashMap<>();


  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object v, String format,
                                   @NotNull Parameters parameters, DataMap map)
  {
    if (v == null)
      return nullText();

    final Number value = (Number)v;

    // special case: show number as bool
    if ("bool".equals(format))
      return formatBoolean(messageContext, value, parameters, map);

    format = getConfigValueString(messageContext, "number", parameters, map);

    if ((format == null || "integer".equals(format)) &&
        (value instanceof BigInteger || value instanceof Long || value instanceof Integer ||
         value instanceof Short || value instanceof Byte || value instanceof AtomicInteger ||
         value instanceof AtomicLong || value instanceof LongAdder))
      return noSpaceText(value.toString());

    return noSpaceText(getFormatter(format, parameters).format(value));
  }


  private Text formatBoolean(@NotNull MessageContext messageContext, Number value, Parameters parameters,
                             DataMap map)
  {
    ParameterFormatter formatter = messageContext.getFormatter("bool", boolean.class);
    Set<FormattableType> types = formatter.getFormattableTypes();

    // if we got some default formatter, use a specific one instead
    if (!types.contains(new FormattableType(Boolean.class)) &&
        !types.contains(new FormattableType(boolean.class)))
      formatter = BOOL_FORMATTER;

    return formatter.format(messageContext, value, parameters, map);
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
      return new DecimalFormat(format, FORMAT_SYMBOLS_CACHE.computeIfAbsent(locale, DecimalFormatSymbols::new));

    return NumberFormat.getNumberInstance(locale);
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(Number.class));
  }
}