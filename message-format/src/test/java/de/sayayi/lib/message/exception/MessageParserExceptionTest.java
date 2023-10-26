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

import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static de.sayayi.lib.message.exception.MessageParserException.Type.MESSAGE;
import static de.sayayi.lib.message.exception.MessageParserException.Type.TEMPLATE;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.9.1
 */
@DisplayName("Message/template parse exception")
class MessageParserExceptionTest
{
  private static Stream<Arguments> getMessage_parameters()
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


  @DisplayName("Detailed exception messages")
  @ParameterizedTest(name = "msg: {5}")
  @MethodSource("getMessage_parameters")
  void testGetMessage(String code, String template, Locale locale, MessageParserException.Type type,
                      @NotNull String msg, @NotNull String displayText)
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
  void testGeneratedMessageCode()
  {
    assertEquals("failed to parse message for locale Dutch (Netherlands): ERROR\nSYNTAX",
        new MessageParserException("ERROR", "SYNTAX", null)
            .withCode("MSG[5JJRMZBVNJ-30UO]")
            .withLocale(new Locale("nl", "NL"))
            .getMessage());
  }
}
