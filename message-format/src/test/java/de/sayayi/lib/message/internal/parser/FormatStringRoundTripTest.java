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
import lombok.val;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Round trip tests: parse &rarr; {@code asFormatString} &rarr; parse must yield the same message.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Format string round trip")
@TestMethodOrder(MethodOrderer.DisplayName.class)
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
  void testRoundTrip(@Language("MessageFormat") String formatString)
  {
    val message = MESSAGE_FACTORY.parseMessage(formatString);

    for(val charset: new java.nio.charset.Charset[] { UTF_8, US_ASCII })
    {
      @Language("MessageFormat")
      val serialized = message.asFormatString(charset);

      val reparsed = MESSAGE_FACTORY.parseMessage(serialized);

      assertTrue(message.isSame(reparsed),
          () -> "'" + formatString + "' -> '" + serialized + "' (" + charset + ") is not the same message");
    }
  }
}
