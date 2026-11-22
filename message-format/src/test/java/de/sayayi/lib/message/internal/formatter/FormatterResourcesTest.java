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
package de.sayayi.lib.message.internal.formatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Locale;

import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 * @since 0.26.0
 */
@DisplayName("Formatter resources")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class FormatterResourcesTest
{
  private static final Locale SWISS_GERMAN = Locale.of("de", "CH");


  private static FormatterResources testResources() {
    return new FormatterResources(FormatterResourcesTest.class, "TestResources");
  }


  @Test
  @DisplayName("Root resources are used for a locale without property file")
  void testRootFallback()
  {
    final var resources = testResources();

    assertEquals("Hello", resources.getResource("greeting", ROOT));
    assertEquals("Hello", resources.getResource("greeting", JAPANESE));
  }


  @Test
  @DisplayName("Specific locale overrides general locales key by key")
  void testCandidateMerge()
  {
    final var resources = testResources();

    assertEquals("Grüezi", resources.getResource("greeting", SWISS_GERMAN));
    assertEquals("Tschüss", resources.getResource("farewell", SWISS_GERMAN));
    assertEquals("root", resources.getResource("only-root", SWISS_GERMAN));
    assertEquals("Hallo", resources.getResource("greeting", GERMANY));
  }


  @Test
  @DisplayName("Missing key returns null")
  void testMissingKey() {
    assertNull(testResources().getResource("missing", GERMAN));
  }


  @Test
  @DisplayName("JVM default locale is not used as fallback")
  void testNoDefaultLocaleFallback()
  {
    final var defaultLocale = getDefault();

    try {
      setDefault(GERMAN);

      assertEquals("Hello", testResources().getResource("greeting", FRENCH));
    } finally {
      setDefault(defaultLocale);
    }
  }


  @Test
  @DisplayName("Missing property files yield no resources")
  void testMissingBundle() {
    assertNull(new FormatterResources(FormatterResourcesTest.class, "NoSuchResources").getResource("greeting", ROOT));
  }


  @Test
  @DisplayName("Shared resources are readable from the library module")
  void testSharedResources()
  {
    assertSame(FormatterResources.shared(), FormatterResources.shared());
    assertNull(FormatterResources.shared().getResource("missing", ENGLISH));
  }
}
