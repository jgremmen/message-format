/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.internal.message;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupportFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.stream.Stream;

import static java.util.Locale.forLanguageTag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests the locale selection of {@link LocalizedMessageBundleWithCode}.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Localized message bundle")
final class LocalizedMessageBundleWithCodeTest
{
  @SuppressWarnings("SpellCheckingInspection")
  static Stream<Arguments> localeSelectionCases()
  {
    final var scripts = bundle("sr-Latn", "Zdravo", "sr-Cyrl", "Здраво", "und", "Hello");
    final var scriptBeforeCountry = bundle("sr-Latn-RS", "Latn RS", "sr-Cyrl", "Cyrl");
    final var noScriptBeforeOtherScript = bundle("sr-Latn", "Latn", "sr", "no script");
    final var chinese = bundle("zh-Hans", "简体", "zh-Hant", "繁體");
    final var noScripts = bundle("en", "en", "en-US", "en-US", "de-DE", "de-DE");

    return Stream.of(
        arguments(scripts, "sr-Latn", "Zdravo"),
        arguments(scripts, "sr-Cyrl", "Здраво"),
        arguments(scripts, "sr-Cyrl-RS", "Здраво"),
        arguments(scripts, "sr", "Zdravo"),
        arguments(scripts, "de", "Hello"),
        arguments(scriptBeforeCountry, "sr-Cyrl-RS", "Cyrl"),
        arguments(scriptBeforeCountry, "sr-Latn-BA", "Latn RS"),
        arguments(noScriptBeforeOtherScript, "sr-Cyrl", "no script"),
        arguments(chinese, "zh-Hant-TW", "繁體"),
        arguments(chinese, "zh-Hans-CN", "简体"),
        arguments(noScripts, "en-US", "en-US"),
        arguments(noScripts, "en-GB", "en"),
        arguments(noScripts, "de-AT", "de-DE"),
        arguments(noScripts, "fr", "en"));
  }


  @ParameterizedTest(name = "{1} -> {2}")
  @MethodSource("localeSelectionCases")
  @DisplayName("Selects the message matching language, script and country, in this order of significance")
  void localeSelection(@NotNull Message.WithCode bundle, @NotNull String languageTag, @NotNull String expected)
  {
    assertEquals(
        expected,
        MessageSupportFactory
            .shared()
            .message(bundle)
            .locale(languageTag)
            .format());
  }


  private static @NotNull Message.WithCode bundle(@NotNull String @NotNull ... tagsAndTexts)
  {
    final var messageFactory = MessageFactory.getSharedInstance();
    final var localizedMessages = new LinkedHashMap<Locale,Message>();

    for(var n = 0; n < tagsAndTexts.length; n += 2)
    {
      final var tag = tagsAndTexts[n];

      localizedMessages.put(
          "und".equals(tag) ? Locale.ROOT : forLanguageTag(tag),
          messageFactory.parseMessage(tagsAndTexts[n + 1]));
    }

    return new LocalizedMessageBundleWithCode("BUNDLE", localizedMessages);
  }
}
