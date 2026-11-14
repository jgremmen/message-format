/*
 * Copyright 2025 Jeroen Gremmen
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
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.part.normalizer.LRUMessagePartNormalizer;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static de.sayayi.lib.message.exception.MessageParserException.Type.MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests the error messages reported by the message lexer, parser and compiler for unrecognized tokens, input
 * mismatches, missing alternatives, unwanted tokens, duplicate configuration names and template entries, numbers out of
 * the long range, message values in post formatter configurations and a lone default map entry.
 *
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
@DisplayName("Message lexer/parser errors")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MessageErrorTest
{
  private static MessageCompiler COMPILER;


  @BeforeAll
  static void init() {
    COMPILER = new MessageCompiler(new MessageFactory(LRUMessagePartNormalizer.create(64)));
    //COMPILER.compileMessage("%{p,:");
  }


  static Stream<Arguments> errorMessages()
  {
    return Stream.of(
        arguments("Token recognition", "%{r{", "message syntax error at '{'"),
        arguments("Input mismatch", "%{64", "missing parameter name at '64'"),
        arguments("Input mismatch", "%{p'", "end of message parameter expected at \"'\""),
        arguments("Input mismatch", "%{p,:", "pre-mature end of message parameter reached; missing default message"),
        arguments("No viable alternative", "%{r,true:!}", "syntax error in message parameter map element at '!'"),
        arguments("Unwanted token", "%{r,d:6", "pre-mature end of message parameter reached; missing '}'"),
        arguments("Duplicate config name", "%{ p1, msg:yes, msg:no }",
            "duplicate config name 'msg' for parameter 'p1'"),
        arguments("Number above long range", "%{p,value:9223372036854775808}", "number value out of range"),
        arguments("Number below long range", "%{p,value:-9223372036854775809}", "number value out of range"),
        arguments("Post formatter with message config value", "%(clip, 'text', config:'%{message}')",
            "post-format config 'config' cannot be a message value"),
        arguments("Default map entry only", "%{ p, :' %{n} items' }",
            "default map entry can only be used in combination with other map entries"),
        arguments("Missing template name", "%[ '' ]", "missing template name at \"'\""),
        arguments("Duplicate template parameter delegate", "%[ xyz, a->b, a->c ]",
            "duplicate template parameter delegate 'a'"),
        arguments("Duplicate template default parameter", "%[ xyz, a=true, a=false ]",
            "duplicate template default parameter 'a'")
    );
  }


  @ParameterizedTest(name = "{0}: {1}")
  @DisplayName("Syntax errors are reported with a descriptive message")
  @MethodSource("errorMessages")
  void testErrorMessage(@SuppressWarnings("unused") String name, @Language("MessageFormat") String formatString, String errorMessage)
  {
    final var ex = assertThrowsExactly(
        MessageParserException.class,
        () -> COMPILER.compileMessage(formatString));

    assertEquals(errorMessage, ex.getErrorMessage());
    assertEquals(MESSAGE, ex.getType());
  }
}
