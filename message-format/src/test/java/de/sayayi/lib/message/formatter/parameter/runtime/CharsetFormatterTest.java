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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.internal.part.map.key.MapKeyString;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.charset.Charset;
import java.util.Map;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.22.1
 */
@DisplayName("Charset formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class CharsetFormatterTest extends AbstractFormatterTest
{
  @Test
  @DisplayName("Formattable types")
  void testFormattableTypes() {
    assertFormatterForType(new CharsetFormatter(), Charset.class);
  }


  @Test
  @DisplayName("Format UTF-8 charset with display name")
  void formatUtf8DisplayName()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()))
        .getMessageAccessor();

    assertEquals(noSpaceText("UTF-8"), format(messageAccessor, UTF_8));
  }


  @Test
  @DisplayName("Format ISO-8859-11 charset with display name")
  void formatIso885911DisplayName()
  {
    val charset = Charset.forName("ISO-8859-11");
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()))
        .getMessageAccessor();

    assertEquals(noSpaceText(charset.displayName()), format(messageAccessor, charset));
  }


  @Test
  @DisplayName("Format null charset")
  void formatNull()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()))
        .getMessageAccessor();

    assertEquals(nullText(), format(messageAccessor, null));
  }


  @Test
  @DisplayName("Format charset with canonical name mapped")
  void formatWithCanonicalNameMapping()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()))
        .getMessageAccessor();

    assertEquals(noSpaceText("Unicode UTF-8"), format(messageAccessor, UTF_8,
        Map.of(), Map.of(new MapKeyString("UTF-8"), new TypedValueString("Unicode UTF-8"))));
  }


  @Test
  @DisplayName("Format charset matched by alias only")
  void formatWithAliasMapping()
  {
    // UTF-8 has aliases like "unicode-1-1-utf-8", "UTF8", etc.
    // Use an alias that is NOT the canonical name to verify alias matching
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()))
        .getMessageAccessor();

    // Find an alias for UTF-8 that differs from the canonical name
    val alias = UTF_8.aliases().stream()
        .filter(a -> !a.equals(UTF_8.name()))
        .findFirst()
        .orElseThrow();

    assertEquals(noSpaceText("Matched by alias"), format(messageAccessor, UTF_8,
        Map.of(), Map.of(new MapKeyString(alias), new TypedValueString("Matched by alias"))));
  }


  @Test
  @DisplayName("Format ISO-8859-1 matched by alias 'latin1'")
  void formatIso88591WithLatin1Alias()
  {
    // ISO-8859-1 has alias "latin1" (among others)
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()))
        .getMessageAccessor();

    assertEquals(noSpaceText("Latin 1"), format(messageAccessor, ISO_8859_1,
        Map.of(), Map.of(new MapKeyString("latin1"), new TypedValueString("Latin 1"))));
  }


  @Test
  @DisplayName("Canonical name mapping takes precedence over alias mapping")
  void formatCanonicalNameTakesPrecedenceOverAlias()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()))
        .getMessageAccessor();

    // When both canonical name and alias are mapped, canonical name should win
    val alias = ISO_8859_1.aliases().stream()
        .filter(a -> !a.equals(ISO_8859_1.name()))
        .findFirst()
        .orElseThrow();

    assertEquals(noSpaceText("Canonical"), format(messageAccessor, ISO_8859_1,
        Map.of(),
        Map.of(new MapKeyString(ISO_8859_1.name()), new TypedValueString("Canonical"),
               new MapKeyString(alias), new TypedValueString("Alias"))));
  }



  @Test
  @DisplayName("Format charset via message template")
  void formatCharsetViaMessageTemplate()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()));

    assertEquals("Encoding: UTF-8", context
        .message("Encoding:  %{cs}")
        .with("cs", UTF_8)
        .format());
  }


  @Test
  @DisplayName("Format charset via message template with map")
  void formatCharsetViaMessageTemplateWithMap()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()));

    assertEquals("8-bit Unicode", context
        .message("%{cs,'UTF-8':'8-bit Unicode'}")
        .with("cs", UTF_8)
        .format());
  }


  @Test
  @DisplayName("Format charset with default map entry")
  void formatCharsetWithDefaultMapEntry()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()));

    assertEquals("unknown encoding", context
        .message("%{cs,'UTF-16':'wide','UTF-32':'very wide',:'unknown encoding'}")
        .with("cs", UTF_8)
        .format());
  }


  @Test
  @DisplayName("Default map entry is not used when canonical name matches")
  void formatCharsetDefaultNotUsedWhenNameMatches()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()));

    assertEquals("matched", context
        .message("%{cs,'UTF-8':'matched',:'fallback'}")
        .with("cs", UTF_8)
        .format());
  }


  @Test
  @DisplayName("Default map entry is not used when alias matches")
  void formatCharsetDefaultNotUsedWhenAliasMatches()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()));

    assertEquals("matched alias", context
        .message("%{cs,'latin1':'matched alias',:'fallback'}")
        .with("cs", ISO_8859_1)
        .format());
  }
}
