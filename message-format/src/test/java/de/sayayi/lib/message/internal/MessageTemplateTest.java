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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.MessageFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.Locale.*;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link MessageTemplate}: the string representation of plain and localized templates.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Message template")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MessageTemplateTest
{
  @Test
  @DisplayName("toString() of a template with a single message")
  void testToString()
  {
    assertEquals("Template(\"Hello %{name}\")",
        MessageFactory.getSharedInstance().parseTemplate("Hello %{name}").toString());
  }


  @Test
  @DisplayName("toString() of a localized template")
  void testToStringLocalized()
  {
    final var template = MessageFactory.getSharedInstance().parseTemplate(Map.of(
        ENGLISH, "Hello %{name}",
        GERMANY, "Hallo %{name}",
        ROOT, "Hi"));
    final var string = template.toString();

    assertEquals("Template(", string.substring(0, 9));
    assertEquals(")", string.substring(string.length() - 1));
    assertEquals(Set.of("en=\"Hello %{name}\"", "de-DE=\"Hallo %{name}\"", "ROOT=\"Hi\""),
        stream(string.substring(9, string.length() - 1).split(",")).collect(toSet()));
  }
}
