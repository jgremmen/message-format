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
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;


/**
 * Tests the error messages reported by the message lexer and parser for unrecognized tokens, input mismatches, missing
 * alternatives and unwanted tokens.
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


  @Test
  @DisplayName("Token recognition message")
  void tokenRecognitionMessage()
  {
    var ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{r{"));
    assertEquals("message syntax error at '{'", ex.getErrorMessage());
  }


  @Test
  @DisplayName("Input mismatch message")
  void inputMismatchMessage()
  {
    var ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{64"));
    assertEquals("missing parameter name at '64'", ex.getErrorMessage());

    ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{p'"));
    assertEquals("end of message parameter expected at \"'\"", ex.getErrorMessage());

    ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{p,:"));
    assertEquals("pre-mature end of message parameter reached; missing default message", ex.getErrorMessage());
  }


  @Test
  @DisplayName("No viable alternative message")
  void parserNoViableAlternative()
  {
    var ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{r,true:!}"));
    assertEquals("syntax error in message parameter map element at '!'", ex.getErrorMessage());
  }


  @Test
  @DisplayName("Unwanted token message")
  void unwantedTokenMessage()
  {
    var ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{r,d:6"));
    assertEquals("pre-mature end of message parameter reached; missing '}'", ex.getErrorMessage());
  }
}
