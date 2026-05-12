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
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.charset.Charset;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static java.nio.charset.StandardCharsets.*;
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
  @DisplayName("Format charset matched by alias only")
  void formatWithAliasMapping()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()));

    // Find an alias for UTF-8 that differs from the canonical name
    val alias = UTF_8.aliases().stream()
        .filter(a -> !a.equals(UTF_8.name()))
        .findFirst()
        .orElseThrow();

    assertEquals("Matched by alias", context
        .message("%{cs,'" + alias + "':'Matched by alias'}")
        .with("cs", UTF_8)
        .format());
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
  @DisplayName("EQ with canonical key, alias key and default selects correct match")
  void eqWithCanonicalAliasAndDefault()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()));

    // canonical 'UTF-8' matches UTF-8, alias 'latin1' matches ISO-8859-1, default for others
    @Language("MessageFormat")
    val msg = "%{cs,'UTF-8':'by-canonical','latin1':'by-alias',:'default'}";

    assertEquals("by-canonical", context.message(msg).with("cs", UTF_8).format());
    assertEquals("by-alias", context.message(msg).with("cs", ISO_8859_1).format());
    assertEquals("default", context.message(msg).with("cs", US_ASCII).format());
  }


  @Test
  @DisplayName("EQ with canonical key and alias key for the same charset")
  void eqCanonicalAndAliasForSameCharset()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()));

    // 'US-ASCII' is canonical, 'ASCII' is alias → canonical match (EXACT) wins over alias (EQUIVALENT)
    @Language("MessageFormat")
    val msg = "%{cs,'ASCII':'by-alias','US-ASCII':'by-canonical',:'default'}";

    assertEquals("by-canonical", context.message(msg).with("cs", US_ASCII).format());
  }


  @Test
  @DisplayName("NE with canonical key and alias key selects correct match")
  @SuppressWarnings("UnreachableMapEntry")
  void neWithCanonicalAndAliasKey()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()));

    // !'UTF-8' mismatches for UTF-8, matches for others;
    // !'latin1' leniently matches for ISO-8859-1 (alias), matches for others
    @Language("MessageFormat")
    val msg = "%{cs,!'UTF-8':'not-utf8',!'latin1':'not-latin1',:'default'}";

    // UTF-8: !'UTF-8' fails (canonical match), !'latin1' succeeds → "not-latin1"
    assertEquals("not-latin1", context.message(msg).with("cs", UTF_8).format());

    // ISO-8859-1: !'UTF-8' succeeds exactly, !'latin1' succeeds leniently → "not-utf8" wins
    assertEquals("not-utf8", context.message(msg).with("cs", ISO_8859_1).format());

    // US-ASCII: both NE keys succeed exactly, first in order wins → "not-utf8"
    assertEquals("not-utf8", context.message(msg).with("cs", US_ASCII).format());
  }


  @Test
  @DisplayName("No string key match and no default uses display name")
  void noMatchNoDefaultUsesDisplayName()
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new CharsetFormatter()));

    assertEquals(UTF_8.displayName(), context
        .message("%{cs,'ISO-8859-1':latin1}")
        .with("cs", UTF_8)
        .format());
  }
}
