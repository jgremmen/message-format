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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.text.DateFormat.FULL;
import static java.text.DateFormat.LONG;
import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.SHORT;
import static java.text.DateFormat.getDateInstance;


/**
 * @author Jeroen Gremmen
 */
public final class DateFormatter extends AbstractParameterFormatter
{
  @NotNull
  @Override
  @Contract(pure = true)
  public Text formatValue(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return nullText();

    return noSpaceText(getFormatter(getConfigFormat(format, data, true, null),
        parameters.getLocale()).format(value));
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Date.class);
  }


  protected DateFormat getFormatter(String format, Locale locale)
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
}
