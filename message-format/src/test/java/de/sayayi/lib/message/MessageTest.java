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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Locale.UK;
import static java.util.Locale.US;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Tests {@link Message}: parsing of single and multi locale messages, compare types in map keys, empty messages and
 * format string serialization.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
@DisplayName("Message")
public class MessageTest
{
  @Test
  @DisplayName("Parsing removes a trailing space from the message text")
  void parseTrimsTrailingSpace()
  {
    var messageSupport = MessageSupportFactory.shared();

    assertEquals("Just a simple message without parameters",
        messageSupport.message("Just a simple message without parameters ").format());
  }


  @Test
  @DisplayName("A localized message is formatted in the requested locale")
  void parseMultiLocale()
  {
    var texts = new HashMap<Locale,String>();

    texts.put(UK, "%{n} %{n,format:choice,1:'colour', :'colours'}.");
    texts.put(Locale.forLanguageTag("nl-NL"), "%{n} %{n, format:choice,1 : 'kleur',: 'kleuren'}.");
    texts.put(Locale.GERMAN, "%{n} %{n,format:choice,1: 'Farbe', :'Farben'}.");
    texts.put(US, "%{n} %{n,format:choice,1:'color', :'colors'}.");

    var msg = MessageFactory.getSharedInstance().parseMessage(texts);
    var messageSupport = MessageSupportFactory.shared();

    assertEquals("1 kleur.",
        messageSupport.message(msg).locale("nl-NL").with("n", 1).format());
    assertEquals("4 colours.",
        messageSupport.message(msg).locale(UK).with("n", 4).format());
  }


  @Test
  @DisplayName("Relational number keys select the negative, zero and positive message")
  void compareType()
  {
    var messageSupport = MessageSupportFactory.shared();
    var m = messageSupport.message("%{n,format:choice,<0:'negative',>0:'positive',:'zero'}").getMessage();

    assertEquals("negative",
        messageSupport.message(m).locale(UK).with("n", -1).format());
    assertEquals("zero",
        messageSupport.message(m).locale(UK).with("n", 0).format());
    assertEquals("positive",
        messageSupport.message(m).locale(UK).with("n", 1234).format());
  }


  @Test
  @DisplayName("An empty message is formatted as empty string")
  void emptyMessage()
  {
    var messageSupport = MessageSupportFactory.shared();

    assertEquals("", messageSupport.message("").format());
  }


  @Test
  @DisplayName("Locale-aware message has no format string")
  void asFormatStringLocaleAware()
  {
    var message = MessageFactory
        .getSharedInstance()
        .parseMessage(Map.of(UK, "colour", US, "color"));

    assertThrows(UnsupportedOperationException.class, () -> message.asFormatString(US_ASCII));
  }
}
