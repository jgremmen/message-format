/*
 * Copyright 2020 Jeroen Gremmen
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

import java.util.Locale;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.STRING_EMPTY_TYPE;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class LocaleFormatter extends AbstractParameterFormatter
{
  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Object value)
  {
    final Locale locale = (Locale)value;

    switch(context.getConfigValueString("locale").orElse("name"))
    {
      case "country":
        return noSpaceText(context
            .getConfigMapMessage(locale.getCountry(), STRING_EMPTY_TYPE)
            .map(message -> message.format(context.getMessageSupport(), context))
            .orElseGet(() -> locale.getDisplayCountry(context.getLocale())));

      case "language":
        return noSpaceText(context
            .getConfigMapMessage(locale.getLanguage(), STRING_EMPTY_TYPE)
            .map(message -> message.format(context.getMessageSupport(), context))
            .orElseGet(() -> locale.getDisplayLanguage(context.getLocale())));

      case "name":
        return noSpaceText(locale.getDisplayName(context.getLocale()));

      case "script":
        return noSpaceText(locale.getDisplayScript(context.getLocale()));

      case "variant":
        return noSpaceText(locale.getDisplayVariant(context.getLocale()));

      default:
        return context.delegateToNextFormatter();
    }
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(Locale.class));
  }
}
