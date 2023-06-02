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

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.text.DateFormat.*;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class DateFormatter extends AbstractParameterFormatter
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Object value)
  {
    try {
      return noSpaceText(getFormatter(
          context.getConfigValueString("date").orElse(null),
          context.getLocale()).format(value));
    } catch(IllegalArgumentException ex) {
      return context.delegateToNextFormatter();
    }
  }


  @Contract(pure = true)
  private @NotNull DateFormat getFormatter(String format, @NotNull Locale locale)
  {
    if ("full".equals(format))
      return getDateInstance(FULL, locale);

    if ("long".equals(format))
      return getDateInstance(LONG, locale);

    if (format == null || format.isEmpty() || "medium".equals(format))
      return getDateInstance(MEDIUM, locale);

    if ("short".equals(format))
      return getDateInstance(SHORT, locale);

    return new SimpleDateFormat(format, locale);
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(Date.class));
  }
}
