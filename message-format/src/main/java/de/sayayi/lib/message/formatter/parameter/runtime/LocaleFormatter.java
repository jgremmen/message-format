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
 * This formatter uses the {@code locale} configuration key to select which aspect of the locale to format.
 * <p>
 * All display values are resolved using the formatting context's locale. For {@code country} and {@code language},
 * string map keys matching the country or language code can be used to override the display value.
 * <p>
 * If the configuration key is absent, the locale's display name is used. If the value does not match a known option,
 * formatting is delegated to the next available formatter.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the locale formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code locale}</td>
 *     <td>{@code name}, {@code country}, {@code lang}, {@code language}, {@code script}, {@code variant}</td>
 *     <td>{@code name}</td>
 *     <td>
 *       Aspect of the locale to format as a display name. For {@code country} and {@code language}, string keys
 *       matching the country or language code override the output. Any other value delegates to the next
 *       formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * The parameter value selects what is displayed, while the locale of the message selects the language it is displayed
 * in:
 * <pre>{@code
 * messageSupport
 *     .message("Language: %{lang}")
 *     .with("lang", Locale.UK)
 *     .locale(Locale.GERMANY)
 *     .format();
 * // "Language: Englisch (Vereinigtes Königreich)"
 * }</pre>
 * <p>
 * Display a single aspect of the locale:
 * <pre>{@code
 * messageSupport.message("%{l,locale:country}").with("l", Locale.US).locale(Locale.FRANCE).format();
 * // "États-Unis"
 *
 * messageSupport.message("%{l,locale:lang}").with("l", Locale.UK).locale(Locale.of("es", "ES")).format();
 * // "inglés"
 * }</pre>
 * <p>
 * Override the display name for specific countries:
 * <pre>{@code
 * messageSupport
 *     .message("%{l,locale:country,'GB':'Britain'}")
 *     .with("l", Locale.UK)
 *     .locale(Locale.US)
 *     .format();
 * // "Britain"
 * }</pre>
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


  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code locale} classifier. If the {@code locale} configuration key refers to a known locale aspect,
   * the {@link ClassifierContext#CLASSIFIER_STRING string} classifier is added as well.
   */
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


  /**
   * Formats the display name of the country of {@code locale}, using a message mapped to the country code if available.
   *
   * @param context  formatter context, not {@code null}
   * @param locale   locale to format, not {@code null}
   *
   * @return  formatted country, never {@code null}
   */
  private @NotNull Text formatLocaleCountry(@NotNull ParameterFormatterContext context, @NotNull Locale locale)
  {
    return formatUsingMappedString(context, locale.getCountry(), true)
        .orElseGet(() -> noSpaceText(locale.getDisplayCountry(context.getLocale())));
  }


  /**
   * Formats the display name of the language of {@code locale}, using a message mapped to the language code if
   * available.
   *
   * @param context  formatter context, not {@code null}
   * @param locale   locale to format, not {@code null}
   *
   * @return  formatted language, never {@code null}
   */
  private @NotNull Text formatLocaleLanguage(@NotNull ParameterFormatterContext context, @NotNull Locale locale)
  {
    return formatUsingMappedString(context, locale.getLanguage(), true)
        .orElseGet(() -> noSpaceText(locale.getDisplayLanguage(context.getLocale())));
  }


  /**
   * Formats the display name of {@code locale}.
   *
   * @param context  formatter context, not {@code null}
   * @param locale   locale to format, not {@code null}
   *
   * @return  formatted display name, never {@code null}
   */
  private @NotNull Text formatLocaleDisplayName(@NotNull ParameterFormatterContext context, @NotNull Locale locale) {
    return noSpaceText(locale.getDisplayName(context.getLocale()));
  }


  /**
   * Formats the display name of the script of {@code locale}.
   *
   * @param context  formatter context, not {@code null}
   * @param locale   locale to format, not {@code null}
   *
   * @return  formatted script, never {@code null}
   */
  private @NotNull Text formatLocaleScript(@NotNull ParameterFormatterContext context, @NotNull Locale locale) {
    return noSpaceText(locale.getDisplayScript(context.getLocale()));
  }


  /**
   * Formats the display name of the variant of {@code locale}.
   *
   * @param context  formatter context, not {@code null}
   * @param locale   locale to format, not {@code null}
   *
   * @return  formatted variant, never {@code null}
   */
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
