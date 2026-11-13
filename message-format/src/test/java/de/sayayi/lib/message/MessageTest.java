/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Locale.UK;
import static java.util.Locale.US;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link Message}: parsing of single and multi locale messages, compare types in map keys, empty messages and
 * format string serialization.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public class MessageTest
{
  @Test
  void testParse1()
  {
    val messageSupport = MessageSupportFactory.shared();

    assertEquals("Just a simple message without parameters",
        messageSupport.message("Just a simple message without parameters ").format());
  }


  @Test
  void testParseMultiLocale()
  {
    val texts = new HashMap<Locale,String>();

    texts.put(UK, "%{n} %{n,format:choice,1:'colour', :'colours'}.");
    texts.put(Locale.forLanguageTag("nl-NL"), "%{n} %{n, format:choice,1 : 'kleur',: 'kleuren'}.");
    texts.put(Locale.GERMAN, "%{n} %{n,format:choice,1: 'Farbe', :'Farben'}.");
    texts.put(US, "%{n} %{n,format:choice,1:'color', :'colors'}.");

    val msg = MessageFactory.getSharedInstance().parseMessage(texts);
    val messageSupport = MessageSupportFactory.shared();

    assertEquals("1 kleur.",
        messageSupport.message(msg).locale("nl-NL").with("n", 1).format());
    assertEquals("4 colours.",
        messageSupport.message(msg).locale(UK).with("n", 4).format());
  }


  @Test
  void testCompareType()
  {
    val messageSupport = MessageSupportFactory.shared();
    val m = messageSupport.message("%{n,format:choice,<0:'negative',>0:'positive',:'zero'}").getMessage();

    assertEquals("negative",
        messageSupport.message(m).locale(UK).with("n", -1).format());
    assertEquals("zero",
        messageSupport.message(m).locale(UK).with("n", 0).format());
    assertEquals("positive",
        messageSupport.message(m).locale(UK).with("n", 1234).format());
  }


  @Test
  void testEmptyMessage()
  {
    val messageSupport = MessageSupportFactory.shared();

    assertEquals("", messageSupport.message("").format());
  }


  @Test
  @DisplayName("Format string is normalized and parses back into an equivalent message")
  @SuppressWarnings("LanguageMismatch")
  void testAsFormatString()
  {
    val messageFactory = MessageFactory.getSharedInstance();
    val formatStrings = Map.of(
        "%{a,(1,2):'x'}", "%{a,1:x,2:x}",
        "%{a , null : 'n' }", "%{a,null:n}",
        "%{a,!empty:\"x\"}", "%{a,<>empty:x}",
        "%{a,n:007}", "%{a,n:7}",
        "\u00e9 %{a,1:'%{b}'}", "\\u00e9 %{a,1:'%{b}'}");

    formatStrings.forEach((formatString, expected) -> {
      val message = messageFactory.parseMessage(formatString);
      val serialized = message.asFormatString(US_ASCII);

      assertEquals(expected, serialized);
      assertTrue(message.isSame(messageFactory.parseMessage(serialized)), formatString);
    });

    // leading and trailing spaces are not part of the format string
    assertEquals("hello world", messageFactory
        .parseMessage("  hello   world  ")
        .asFormatString(US_ASCII));
  }


  @Test
  @DisplayName("Locale-aware message has no format string")
  void testAsFormatStringLocaleAware()
  {
    val message = MessageFactory
        .getSharedInstance()
        .parseMessage(Map.of(UK, "colour", US, "color"));

    assertThrows(UnsupportedOperationException.class, () -> message.asFormatString(US_ASCII));
  }
}
