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
package de.sayayi.lib.message.internal.parser;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.part.normalizer.LRUMessagePartNormalizer;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.Charset;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Round trip tests: parse &rarr; {@code asFormatString} &rarr; parse must yield the same message. Also tests the
 * normalized form returned by {@code asFormatString}.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Format string round trip")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("UnnecessaryUnicodeEscape")
final class FormatStringRoundTripTest
{
  private static final MessageFactory MESSAGE_FACTORY = new MessageFactory(LRUMessagePartNormalizer.create(64));


  @ParameterizedTest(name = "{0}")
  @DisplayName("Round trip")
  @ValueSource(strings = {
      // plain text and escapes
      "",
      "Hello World!",
      "100\\% sure, \\%{no-param}, \\%[no-template] and \\%(no-post-format)",
      "backslash \\\\ and \\\\u0041 and \\\\x41",
      "C:\\\\temp\\\\dir",
      "C:\\temp and lone \\ backslash \\",
      "quotes ' and \" in text",
      "Smile 😀 now",
      "Cafe\u0301 नमस्ते \u200d",
      "a\\u000ab",
      "\u00e9 %{a,1:'%{b}'}",
      "unassigned \\u0378 and \\udb40\\udc80",
      "%{p,cfg:'unassigned \\u0378'}",

      // config values
      "%{p,cfg:true}",
      "%{p,cfg:'true'}",
      "%{p,cfg:'false'}",
      "%{p,cfg:'null'}",
      "%{p,cfg:'empty'}",
      "%{p,cfg:'format'}",
      "%{p,cfg:null}",
      "%{p,cfg:name}",
      "%{p,cfg:'two words'}",
      "%{p,cfg:'back\\\\slash'}",
      "%{p,cfg:'quote \\' inside'}",
      "%{p,cfg:\"quote ' inside\"}",
      "%{p,cfg:'msg %{q}'}",
      "%{p,cfg:42}",

      // map values
      "%{p,true:'true',false:false,null:'null',empty:empty,:'format'}",
      "%{p,1:yes,2:'two words',3:'back\\\\slash',:'%{p} items'}",
      "%{p,'key\\\\':value,'k\\'ey':'v\"alue'}",
      "%{a,(1,2):'x'}",
      "%{a , null : 'n' }",
      "%{a,!empty:\"x\"}",
      "%{a,n:007}",

      // templates
      "%[tpl]",
      "%[tpl,p='true']",
      "%[tpl,p='abc']",
      "%[tpl,p='back\\\\slash']",
      "%[tpl,p=true,q=42]",
      "%[tpl,p->q]",

      // post formatters
      "%(clip,'%{p}',size:10)",
      "%(clip,'true',size:10)",
      "%(clip,'back\\\\slash',size:10)"
  })
  void roundTrip(@Language("MessageFormat") String formatString)
  {
    var message = MESSAGE_FACTORY.parseMessage(formatString);

    for(var charset: new Charset[] { UTF_8, US_ASCII })
    {
      @Language("MessageFormat")
      var serialized = message.asFormatString(charset);

      assertTrue(
          message.isSame(MESSAGE_FACTORY.parseMessage(serialized)),
          () -> "'" + formatString + "' -> '" + serialized + "' (" + charset + ") is not the same message");
    }
  }


  static Stream<Arguments> normalizedFormatStrings()
  {
    return Stream.of(
        arguments("%{a,(1,2):'x'}", "%{a,1:x,2:x}"),
        arguments("%{a , null : 'n' }", "%{a,null:n}"),
        arguments("%{a,!empty:\"x\"}", "%{a,<>empty:x}"),
        arguments("%{a,n:007}", "%{a,n:7}"),
        arguments("\u00e9 %{a,1:'%{b}'}", "\\u00e9 %{a,1:'%{b}'}"),
        // leading and trailing spaces are not part of the format string
        arguments("  hello   world  ", "hello world"));
  }


  @ParameterizedTest(name = "{0}")
  @DisplayName("Format string is normalized")
  @MethodSource("normalizedFormatStrings")
  void normalizedForm(@Language("MessageFormat") String formatString, String expected)
  {
    assertEquals(
        expected,
        MESSAGE_FACTORY
            .parseMessage(formatString)
            .asFormatString(US_ASCII));
  }


  @Test
  @DisplayName("Supplementary characters are only escaped if the charset cannot encode them")
  void supplementaryCharacters()
  {
    var message = MESSAGE_FACTORY.parseMessage("Smile \uD83D\uDE00 now");

    assertEquals(
        "Smile \ud83d\ude00 now",
        message.asFormatString(UTF_8));
    assertEquals(
        "Smile \\ud83d\\ude00 now",
        message.asFormatString(US_ASCII));
  }


  @Test
  @DisplayName("Characters rejected by the lexer are escaped even if the charset can encode them")
  void lexerRejectedCharacters()
  {
    var message = MESSAGE_FACTORY.parseMessage("a\\u0378b \\udb40\\udc80 \\ud800");

    assertEquals("a\\u0378b \\udb40\\udc80 \\ud800", message.asFormatString(UTF_8));
  }
}
