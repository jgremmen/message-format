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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import static de.sayayi.lib.message.MessageSupportFactory.createDefault;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Resource bundle adopter")
final class ResourceBundleAdopterTest
{
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
}
