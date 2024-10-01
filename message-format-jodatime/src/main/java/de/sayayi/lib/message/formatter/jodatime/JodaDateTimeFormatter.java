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
package de.sayayi.lib.message.formatter.jodatime;

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joda.time.*;
import org.joda.time.base.BaseLocal;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
public final class JodaDateTimeFormatter extends AbstractParameterFormatter<Object>
{
  private static final Map<String,String> STYLE = new HashMap<>();


  static
  {
    STYLE.put(null, "MM");
    STYLE.put("short", "SS");
    STYLE.put("medium", "MM");
    STYLE.put("long", "LL");
    STYLE.put("full", "FF");
    STYLE.put("date", "M-");
    STYLE.put("time", "-M");
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Object value)
  {
    final String format = context.getConfigValueString("date").orElse(null);
    final DateTimeFormatter formatter;

    if (!STYLE.containsKey(format))
    {
      formatter = DateTimeFormat
          .forPattern(requireNonNull(format))
          .withLocale(context.getLocale());
    }
    else
    {
      final char[] style = STYLE.get(format).toCharArray();

      if (value instanceof LocalDate)
        style[1] = '-';
      else if (value instanceof LocalTime)
        style[0] = '-';

      if (style[0] == '-' && style[1] == '-')
        return emptyText();

      formatter = DateTimeFormat
          .forStyle(new String(style))
          .withLocale(context.getLocale());
    }

    return noSpaceText(value instanceof ReadablePartial
        ? formatter.print((ReadablePartial)value)
        : formatter.print((ReadableInstant)value));
  }


  @Override
  @Contract(pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return Set.of(
        new FormattableType(BaseLocal.class),
        new FormattableType(ReadableDateTime.class));
  }
}
