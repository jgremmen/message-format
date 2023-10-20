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

import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
class MessageFormatExceptionTest
{
  private static Stream<Arguments> getMessage_parameters()
  {
    return Stream.of(
        Arguments.of(null, null, null, null,
            "failed to format message"),
        Arguments.of(null, null, null, "p",
            "failed to format message parameter 'p'"),
        Arguments.of(null, null, GERMANY, null,
            "failed to format message for locale German (Germany)"),
        Arguments.of(null, null, GERMAN, "n",
            "failed to format message parameter 'n' for locale German"),
        Arguments.of(null, "msg", null, null,
            "failed to format template 'msg'"),
        Arguments.of(null, "msg-with-default", null, "m",
            "failed to format parameter 'm' in template 'msg-with-default'"),
        Arguments.of(null, "tpl", UK, null,
            "failed to format template 'tpl' for locale English (United Kingdom)"),
        Arguments.of(null, "name", ITALY, "p",
            "failed to format parameter 'p' in template 'name' for locale Italian (Italy)"),
        Arguments.of("MSG-001", null, null, null,
            "failed to format message with code 'MSG-001'"),
        Arguments.of("MSG-002", null, null, "number",
            "failed to format parameter 'number' for message with code 'MSG-002'"),
        Arguments.of("MSG-003", null, TRADITIONAL_CHINESE, null,
            "failed to format message with code 'MSG-003' and locale Chinese (Taiwan)"),
        Arguments.of("MSG-004", null, US, "flag",
            "failed to format parameter 'flag' for message with code 'MSG-004' and locale English (United States)"),
        Arguments.of("MFP1", "flow", null, null,
            "failed to format template 'flow' for message with code 'MFP1'"),
        Arguments.of("MFP2", "age-range", null, "ABC",
            "failed to format parameter 'ABC' in template 'age-range' for message with code 'MFP2'"),
        Arguments.of("MFP3", "test", new Locale("nl", "BE"), null,
            "failed to format template 'test' for message with code 'MFP3' and locale Dutch (Belgium)"),
        Arguments.of("MFP4", "bool", new Locale("fr", "CH"), "j",
            "failed to format parameter 'j' in template 'bool' for message with code 'MFP4' and locale French (Switzerland)")
    );
  }


  @DisplayName("Detailed exception messages")
  @ParameterizedTest(name = "msg: {4}")
  @MethodSource("getMessage_parameters")
  void testGetMessage(String code, String template, Locale locale, String parameter,
                      @NotNull String msg)
  {
    var ex = new MessageFormatException(null);

    if (code != null)
      ex = ex.withCode(code);

    if (template != null)
      ex = ex.withTemplate(template);

    if (locale != null)
      ex = ex.withLocale(locale);

    if (parameter != null)
      ex = ex.withParameter(parameter);

    assertEquals(msg, ex.getMessage());
  }


  @Test
  @DisplayName("Exclude generated code from detailed exception message")
  void testGeneratedMessageCode()
  {
    assertEquals("failed to format message parameter 'q' for locale Dutch (Netherlands)",
        new MessageFormatException(null)
            .withCode("MSG[5JJRMZBVNJ-30UO]")
            .withParameter("q")
            .withLocale(new Locale("nl", "NL"))
            .getMessage());
  }
}
