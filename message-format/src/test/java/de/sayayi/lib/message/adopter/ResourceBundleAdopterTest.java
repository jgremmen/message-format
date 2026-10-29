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
package de.sayayi.lib.message.adopter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.exception.MessageAdopterException;
import de.sayayi.lib.message.internal.MessageTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import static de.sayayi.lib.message.MessageSupportFactory.createDefault;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Resource bundle adopter")
final class ResourceBundleAdopterTest
{
  private static final String MESSAGE_BUNDLE = "adopter.messages";
  private static final String TEMPLATE_BUNDLE = "adopter.templates";


  @Test
  @DisplayName("Adopt a template from a single bundle")
  void adoptsTemplateFromSingleBundle()
  {
    final var messageSupport = createDefault();
    final var adopter = new ResourceBundleAdopter(messageSupport);

    adopter.adoptTemplates(ResourceBundle.getBundle(TEMPLATE_BUNDLE, ENGLISH));

    assertEquals("Hello, Alice!", messageSupport
        .message("%[greeting]")
        .with("name", "Alice")
        .locale(ENGLISH)
        .format());
  }


  @Test
  @DisplayName("Combine localized templates from multiple bundles")
  void combinesLocalizedTemplatesFromBundles()
  {
    final var messageSupport = createDefault();
    final var adopter = new ResourceBundleAdopter(messageSupport);

    adopter.adoptTemplates(List.of(
        ResourceBundle.getBundle(TEMPLATE_BUNDLE, ENGLISH),
        ResourceBundle.getBundle(TEMPLATE_BUNDLE, GERMAN)));

    assertEquals("Hello, Alice!", messageSupport
        .message("%[greeting]")
        .with("name", "Alice")
        .locale(ENGLISH)
        .format());
    assertEquals("Hallo, Alice!", messageSupport
        .message("%[greeting]")
        .with("name", "Alice")
        .locale(GERMAN)
        .format());
  }


  @Test
  @DisplayName("Adopt localized templates by base name and custom class loader")
  void adoptsTemplatesByBaseNameAndLocalesWithCustomClassLoader()
  {
    final var messageSupport = createDefault();
    final var adopter = new ResourceBundleAdopter(messageSupport);

    adopter.adoptTemplates(TEMPLATE_BUNDLE, Set.of(ENGLISH, GERMAN), getClass().getClassLoader());

    assertEquals("Hallo, Alice!", messageSupport.message("%[greeting]")
        .with("name", "Alice")
        .locale(GERMAN)
        .format());
  }


  @Test
  @DisplayName("Throw if only the base bundle exists for a requested locale")
  void throwsForMissingLocaleWithBaseBundle()
  {
    final var adopter = new ResourceBundleAdopter(createDefault());

    withDefaultLocale(GERMAN, () -> {
      assertThrows(MessageAdopterException.class, () -> adopter.adopt(MESSAGE_BUNDLE, Set.of(FRENCH)));
      assertThrows(MessageAdopterException.class, () -> adopter.adoptTemplates(MESSAGE_BUNDLE, Set.of(FRENCH)));
    });
  }


  @Test
  @DisplayName("Throw if only a parent locale bundle exists for a requested locale")
  void throwsForMissingLocaleWithParentBundle()
  {
    final var adopter = new ResourceBundleAdopter(createDefault());

    assertThrows(MessageAdopterException.class, () -> adopter.adopt(MESSAGE_BUNDLE, Set.of(GERMANY)));
    assertThrows(MessageAdopterException.class, () -> adopter.adoptTemplates(MESSAGE_BUNDLE, Set.of(GERMANY)));
  }


  @Test
  @DisplayName("Throw if only the default locale bundle exists for a requested locale")
  void throwsForMissingLocaleWithDefaultLocaleBundle()
  {
    final var adopter = new ResourceBundleAdopter(createDefault());

    withDefaultLocale(ENGLISH, () -> {
      assertThrows(MessageAdopterException.class, () -> adopter.adopt(TEMPLATE_BUNDLE, Set.of(FRENCH)));
      assertThrows(MessageAdopterException.class, () -> adopter.adoptTemplates(TEMPLATE_BUNDLE, Set.of(FRENCH)));
    });
  }


  @Test
  @DisplayName("Adopt messages for requested locales including inherited keys")
  void adoptsMessagesForRequestedLocales()
  {
    final var messageSupport = createDefault();
    final var adopter = new ResourceBundleAdopter(messageSupport);

    adopter.adopt(MESSAGE_BUNDLE, Set.of(ROOT, GERMAN));

    final var messageAccessor = messageSupport.getMessageAccessor();

    assertEquals(Set.of(ROOT, GERMAN), getLocales(messageAccessor.getMessageByCode("title")));
    assertEquals(Set.of(ROOT, GERMAN), getLocales(messageAccessor.getMessageByCode("farewell")));
    assertEquals("Willkommen", messageSupport.code("title").locale(GERMAN).format());
    assertEquals("Goodbye", messageSupport.code("farewell").locale(GERMAN).format());
  }


  @Test
  @DisplayName("Adopt messages for available locales under the bundle locales only")
  void adoptsMessagesForAvailableLocalesUnderBundleLocales()
  {
    final var messageSupport = createDefault();
    final var adopter = new ResourceBundleAdopter(messageSupport);

    withDefaultLocale(GERMAN, () -> adopter.adopt(MESSAGE_BUNDLE));

    final var messageAccessor = messageSupport.getMessageAccessor();

    assertEquals(Set.of(ROOT, GERMAN), getLocales(messageAccessor.getMessageByCode("title")));
    assertEquals("Welcome", messageSupport.code("title").locale(FRENCH).format());
    assertEquals("Willkommen", messageSupport.code("title").locale(GERMANY).format());
  }


  @Test
  @DisplayName("Adopt templates for available locales under the bundle locales only")
  void adoptsTemplatesForAvailableLocalesUnderBundleLocales()
  {
    final var messageSupport = createDefault();
    final var adopter = new ResourceBundleAdopter(messageSupport);

    withDefaultLocale(ENGLISH, () -> adopter.adoptTemplates(TEMPLATE_BUNDLE));

    final var template = messageSupport.getMessageAccessor().getTemplateByName("greeting");

    assertEquals(Set.of(ENGLISH, GERMAN),
        getLocales(assertInstanceOf(MessageTemplate.class, template).getMessage()));
  }


  private static Set<Locale> getLocales(Message message) {
    return assertInstanceOf(Message.LocaleAware.class, message).getLocales();
  }


  private static void withDefaultLocale(Locale locale, Executable executable)
  {
    final var defaultLocale = Locale.getDefault();

    try {
      Locale.setDefault(locale);
      assertDoesNotThrow(executable);
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }
}
