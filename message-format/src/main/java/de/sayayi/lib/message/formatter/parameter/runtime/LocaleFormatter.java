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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractMultiSelectFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_STRING;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Locale} values.
 * <p>
 * This formatter uses the {@code locale} configuration key to select which aspect of the locale to format:
 * <ul>
 *   <li>{@code name} (default) &ndash; the locale's display name</li>
 *   <li>{@code country} &ndash; the locale's country display name</li>
 *   <li>{@code lang} or {@code language} &ndash; the locale's language display name</li>
 *   <li>{@code script} &ndash; the locale's script display name</li>
 *   <li>{@code variant} &ndash; the locale's variant display name</li>
 * </ul>
 * <p>
 * All display values are resolved using the formatting context's locale. For {@code country} and {@code language},
 * string map keys in the parameter configuration can be used to override the display value.
 * <p>
 * If the configuration key is absent, the locale's display name is used. If the value does not match a known option,
 * formatting is delegated to the next available formatter.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class LocaleFormatter extends AbstractMultiSelectFormatter<Locale>
{
  /**
   * Creates a new locale formatter with the configuration key {@code locale} and selection
   * options for the various locale aspects.
   */
  public LocaleFormatter()
  {
    super("locale", "name", true);

    register("country", this::formatLocaleCountry);
    register(new String[] { "lang", "language" }, this::formatLocaleLanguage);
    register("name", this::formatLocaleDisplayName);
    register("script", this::formatLocaleScript);
    register("variant", this::formatLocaleVariant);
  }


  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier("locale");

    return switch(context.getConfigValueString("locale").orElse("name")) {
      case "country", "language", "lang", "name", "script", "variant" -> {
        context.addClassifier(CLASSIFIER_STRING);
        yield true;
      }

      default -> false;
    };
  }


  private @NotNull Text formatLocaleCountry(@NotNull ParameterFormatterContext context, @NotNull Locale locale)
  {
    return formatUsingMappedString(context, locale.getCountry(), true)
        .orElseGet(() -> noSpaceText(locale.getDisplayCountry(context.getLocale())));
  }


  private @NotNull Text formatLocaleLanguage(@NotNull ParameterFormatterContext context, @NotNull Locale locale)
  {
    return formatUsingMappedString(context, locale.getLanguage(), true)
        .orElseGet(() -> noSpaceText(locale.getDisplayLanguage(context.getLocale())));
  }


  private @NotNull Text formatLocaleDisplayName(@NotNull ParameterFormatterContext context, @NotNull Locale locale) {
    return noSpaceText(locale.getDisplayName(context.getLocale()));
  }


  private @NotNull Text formatLocaleScript(@NotNull ParameterFormatterContext context, @NotNull Locale locale) {
    return noSpaceText(locale.getDisplayScript(context.getLocale()));
  }


  private @NotNull Text formatLocaleVariant(@NotNull ParameterFormatterContext context, @NotNull Locale locale) {
    return noSpaceText(locale.getDisplayVariant(context.getLocale()));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link Locale} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Locale.class));
  }
}
