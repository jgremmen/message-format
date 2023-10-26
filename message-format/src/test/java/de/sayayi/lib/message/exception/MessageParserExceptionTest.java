/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.exception;

import de.sayayi.lib.message.exception.MessageParserException.Type;
import lombok.var;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.exception.MessageParserException.Type.MESSAGE;
import static de.sayayi.lib.message.exception.MessageParserException.Type.TEMPLATE;
import static java.util.Collections.singletonMap;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 * @since 0.9.1
 */
@DisplayName("Message/template parse exception")
@TestMethodOrder(OrderAnnotation.class)
class MessageParserExceptionTest
{
  private static Stream<Arguments> testGetMessage_parameters()
  {
    return Stream.of(
        Arguments.of(null, null, null, null,
            "ERROR\nSYNTAX",
            "(empty)"),
        Arguments.of(null, null, null, MESSAGE,
            "failed to parse message: ERROR\nSYNTAX",
            "failed to parse message"),
        Arguments.of(null, null, null, TEMPLATE,
            "failed to parse template: ERROR\nSYNTAX",
            "failed to parse template"),
        Arguments.of(null, null, GERMAN, null,
            "failed to parse message/template for locale German: ERROR\nSYNTAX",
            "failed to parse message/template for locale German"),
        Arguments.of(null, null, US, MESSAGE,
            "failed to parse message for locale English (United States): ERROR\nSYNTAX",
            "failed to parse message for locale English (United States)"),
        Arguments.of(null, null, FRENCH, TEMPLATE,
            "failed to parse template for locale French: ERROR\nSYNTAX",
            "failed to parse template for locale French"),
        Arguments.of(null, "tpl", null, TEMPLATE,
            "failed to parse template 'tpl': ERROR\nSYNTAX",
            "failed to parse template 'tpl'"),
        Arguments.of(null, "msg-with-default", ITALY, TEMPLATE,
            "failed to parse template 'msg-with-default' for locale Italian (Italy): ERROR\nSYNTAX",
            "failed to parse template 'msg-with-default' for locale Italian (Italy)"),
        Arguments.of("MSG-001", null, null, MESSAGE,
            "failed to parse message with code 'MSG-001': ERROR\nSYNTAX",
            "failed to parse message with code 'MSG-001'"),
        Arguments.of("MSG-003", null, TRADITIONAL_CHINESE, MESSAGE,
            "failed to parse message with code 'MSG-003' for locale Chinese (Taiwan): ERROR\nSYNTAX",
            "failed to parse message with code 'MSG-003' for locale Chinese (Taiwan)")
    );
  }


  @DisplayName("Detailed exception messages (directly constructed)")
  @ParameterizedTest(name = "msg: {5}")
  @MethodSource("testGetMessage_parameters")
  @Order(1)
  void testGetMessage(String code, String template, Locale locale, Type type, @NotNull String msg,
                      @SuppressWarnings("unused") @NotNull String displayText)
  {
    var ex = new MessageParserException("ERROR", "SYNTAX", new IllegalArgumentException());

    if (type != null)
      ex = ex.withType(type);

    if (locale != null)
      ex = ex.withLocale(locale);

    if (code != null)
      ex = ex.withCode(code);

    if (template != null)
      ex = ex.withTemplate(template);

    assertEquals(msg, ex.getMessage());
  }


  @Test
  @DisplayName("Exclude generated code from detailed exception message")
  @Order(3)
  void testGeneratedMessageCode()
  {
    assertEquals("failed to parse message for locale Dutch (Netherlands): ERROR\nSYNTAX",
        new MessageParserException("ERROR", "SYNTAX", null)
            .withCode("MSG[5JJRMZBVNJ-30UO]")
            .withLocale(new Locale("nl", "NL"))
            .getMessage());
  }


  @Test
  @DisplayName("Exclude generated template name from detailed exception message")
  @Order(4)
  void testGeneratedTemplateName()
  {
    assertEquals("failed to parse template for locale Dutch (Netherlands): ERROR\nSYNTAX",
        new MessageParserException("ERROR", "SYNTAX", null)
            .withTemplate("TPL[5JJRMZBVNJ-30UO]")
            .withLocale(new Locale("nl", "NL"))
            .getMessage());
  }


  private static Stream<Arguments> testParseFailure_parameters()
  {
    return Stream.of(
        Arguments.of(null, null, MESSAGE, "%{@var",
            "failed to parse message"),
        Arguments.of(null, null, TEMPLATE, "\\u0021\\'%{b,true:yes,false:no,,}",
            "failed to parse template"),
        Arguments.of(null, US, MESSAGE, "%{array,size{",
            "failed to parse message for locale English (United States)"),
        Arguments.of(null, FRENCH, TEMPLATE, "%[colon-ex]",
            "failed to parse template for locale French"),
        Arguments.of("MSG-001", null, MESSAGE, "%{f,>0.5:true,:false}",
            "failed to parse message with code 'MSG-001'"),
        Arguments.of("MSG-003", TRADITIONAL_CHINESE, MESSAGE, "%{8}",
            "failed to parse message with code 'MSG-003' for locale Chinese (Taiwan)")
    );
  }


  @DisplayName("Detailed exception messages (thrown by parser)")
  @ParameterizedTest(name = "parse: {4}")
  @MethodSource("testParseFailure_parameters")
  @SuppressWarnings("UnknownLanguage")
  @Order(2)
  void testParseFailure(String code, Locale locale, @NotNull Type type,
                        @Language("MessageFormat") @NotNull String parseString, @NotNull String msg)
  {
    final int n =
        (type == TEMPLATE ? 0b010 : 0b000) +
        (code != null ? 0b100 : 0b000) +
        (locale != null ? 0b001 : 0b000);

    assertTrue(assertThrows(MessageParserException.class, () -> {
      switch(n)
      {
        case 0b000:
          NO_CACHE_INSTANCE.parseMessage(parseString);
          break;

        case 0b001:
          NO_CACHE_INSTANCE.parseMessage(singletonMap(locale, parseString));
          break;

        case 0b100:
          NO_CACHE_INSTANCE.parseMessage(code, parseString);
          break;

        case 0b101:
          NO_CACHE_INSTANCE.parseMessage(code, singletonMap(locale, parseString));
          break;

        case 0b010:
          NO_CACHE_INSTANCE.parseTemplate(parseString);
          break;

        case 0b011:
          NO_CACHE_INSTANCE.parseTemplate(singletonMap(locale, parseString));
          break;
      }
    }).getMessage().startsWith(msg + ": "));
  }
}
