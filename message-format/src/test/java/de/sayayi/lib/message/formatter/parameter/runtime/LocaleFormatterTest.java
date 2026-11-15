/*
 * Copyright 2023 Jeroen Gremmen
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

import de.sayayi.lib.message.internal.part.map.key.MapKeyString;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link LocaleFormatter}: formattable types and formatting of the country, language and name of a locale.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Locale formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("SpellCheckingInspection")
final class LocaleFormatterTest extends AbstractFormatterTest
{
  @Test
  @DisplayName("Formattable types include Locale")
  void formattableTypes() {
    assertFormatterForType(new LocaleFormatter(), Locale.class);
  }


  static Stream<Arguments> displayNames()
  {
    return Stream.of(
        arguments(FRANCE, US, "country", "États-Unis"),
        arguments(forLanguageTag("es-ES"), UK, "language", "inglés"),
        arguments(forLanguageTag("nl-BE"), UK, null, "Engels (Verenigd Koninkrijk)"),
        arguments(forLanguageTag("nl-BE"), GERMAN, "name", "Duits"),
        arguments(forLanguageTag("nl-BE"), KOREA, "name", "Koreaans (Zuid-Korea)"));
  }


  @ParameterizedTest(name = "{1} in {0} with locale:{2} -> {3}")
  @DisplayName("Country, language and name are displayed in the language of the formatting locale")
  @MethodSource("displayNames")
  void displayName(Locale formattingLocale, Locale value, String localeConfig, String expected)
  {
    var messageAccessor = createMessageAccessor(formattingLocale, new LocaleFormatter());

    assertEquals(
        noSpaceText(expected),
        localeConfig == null
            ? format(messageAccessor, value)
            : format(messageAccessor, value, "locale", localeConfig));
  }


  static Stream<Arguments> mappedValues()
  {
    return Stream.of(
        arguments(FRANCE, UK, "country", "GB", "The Great Kingdom"),
        arguments(forLanguageTag("es-ES"), FRANCE, "language", "fr", "francesa"));
  }


  @ParameterizedTest(name = "{1} with locale:{2} and key {3} -> {4}")
  @DisplayName("Country and language codes matching a string map key are formatted with the mapped message")
  @MethodSource("mappedValues")
  void mappedValue(Locale formattingLocale, Locale value, String localeConfig, String mapKey,
                       @Language("MessageFormat") String expected)
  {
    var messageAccessor = createMessageAccessor(formattingLocale, new LocaleFormatter());
    var messageFactory = messageAccessor.getMessageFactory();

    assertEquals(
        noSpaceText(expected),
        format(
            messageAccessor,
            value,
            Map.of("locale", new TypedValueString(messageFactory, localeConfig)),
            Map.of(new MapKeyString(mapKey), new TypedValueMessage(messageFactory.parseMessage(expected)))));
  }
}
