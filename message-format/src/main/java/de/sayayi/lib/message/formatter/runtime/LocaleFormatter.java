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

import de.sayayi.lib.message.formatter.AbstractMultiSelectFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class LocaleFormatter extends AbstractMultiSelectFormatter<Locale>
{
  public LocaleFormatter()
  {
    super("locale", "name", true);

    register("country", this::formatLocaleCountry);
    register("language", this::formatLocaleLanguage);
    register("name", this::formatLocaleDisplayName);
    register("script", this::formatLocaleScript);
    register("variant", this::formatLocaleVariant);
  }


  private @NotNull Text formatLocaleCountry(@NotNull FormatterContext context, @NotNull Locale locale)
  {
    return formatUsingMappedString(context, locale.getCountry(), true)
        .orElseGet(() -> noSpaceText(locale.getDisplayCountry(context.getLocale())));
  }


  private @NotNull Text formatLocaleLanguage(@NotNull FormatterContext context, @NotNull Locale locale)
  {
    return formatUsingMappedString(context, locale.getLanguage(), true)
        .orElseGet(() -> noSpaceText(locale.getDisplayLanguage(context.getLocale())));
  }


  private @NotNull Text formatLocaleDisplayName(@NotNull FormatterContext context, @NotNull Locale locale) {
    return noSpaceText(locale.getDisplayName(context.getLocale()));
  }


  private @NotNull Text formatLocaleScript(@NotNull FormatterContext context, @NotNull Locale locale) {
    return noSpaceText(locale.getDisplayScript(context.getLocale()));
  }


  private @NotNull Text formatLocaleVariant(@NotNull FormatterContext context, @NotNull Locale locale) {
    return noSpaceText(locale.getDisplayVariant(context.getLocale()));
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Locale.class));
  }
}
