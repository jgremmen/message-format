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

import java.text.CollationKey;
import java.text.Collator;

import static java.util.Locale.GERMANY;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link CollationKeyFormatter}: formattable types, formatting, size and empty key comparison.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Collation key formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class CollationKeyFormatterTest extends AbstractFormatterTest
{
  private final MessageSupport messageSupport = createDefaultMessageSupport(new CollationKeyFormatter());


  @Test
  @DisplayName("Formattable types include CollationKey")
  void formattableTypes() {
    assertFormatterForType(new CollationKeyFormatter(), CollationKey.class);
  }


  @Test
  @DisplayName("Format source string and size")
  void formatSourceStringAndSize()
  {
    var key = Collator
        .getInstance(GERMANY)
        .getCollationKey("Straße");

    assertEquals(
        "Sorted by Straße",
        messageSupport
            .message("Sorted by %{key}")
            .with("key", key)
            .format());
    assertEquals(
        "6 characters",
        messageSupport
            .message("%{key,format:size} characters")
            .with("key", key)
            .format());
  }


  @Test
  @DisplayName("Empty key matches a collation key for an empty string")
  void emptyKey()
  {
    var collator = Collator.getInstance(GERMANY);
    var message = messageSupport.message("%{key,empty:'no sort key',!empty:'sort key'}");

    assertEquals(
        "no sort key",
        message
            .with("key", collator.getCollationKey(""))
            .format());
    assertEquals(
        "sort key",
        message
            .with("key", collator.getCollationKey("a"))
            .format());
  }
}
