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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link ManifestFormatter}: formattable types and formatting of the main attributes and the entries.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Manifest formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ManifestFormatterTest extends AbstractFormatterTest
{
  private final MessageSupport messageSupport = createDefaultMessageSupport(new ManifestFormatter());


  @Test
  @DisplayName("Formattable types include Manifest")
  void formattableTypes() {
    assertFormatterForType(new ManifestFormatter(), Manifest.class);
  }


  @Test
  @DisplayName("Format main attributes")
  void mainAttributes()
  {
    var manifest = new Manifest();
    manifest
        .getMainAttributes()
        .putValue("Manifest-Version", "1.0");

    assertEquals(
        "Manifest-Version: 1.0",
        messageSupport
            .message("%{m,map-kv:'%{key}: %{value}'}")
            .with("m", manifest)
            .format());
    assertEquals(
        "Manifest-Version: 1.0",
        messageSupport
            .message("%{m,manifest:main-attributes,map-kv:'%{key}: %{value}'}")
            .with("m", manifest)
            .format());
  }


  @Test
  @DisplayName("Format entry names")
  void entries()
  {
    var manifest = new Manifest();
    manifest
        .getEntries()
        .put("de/sayayi/Main.class", new Attributes());

    assertEquals(
        "Signed: de/sayayi/Main.class",
        messageSupport
            .message("Signed: %{m,manifest:entries,map-kv:'%{key}'}")
            .with("m", manifest)
            .format());
  }
}
