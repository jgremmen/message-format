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
package de.sayayi.lib.message.parser;

import lombok.val;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.parser.MessageLexer.CH;
import static de.sayayi.lib.message.parser.MessageLexer.P_START;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("Message format lexer")
public class MessageTokenizerTest
{
  @Test
  @SuppressWarnings("UnnecessaryUnicodeEscape")
  public void testDefaultMode()
  {
    val lexer = createFor("hi  \\u0021%{");

    // h
    Token token = lexer.nextToken();
    assertEquals("h", token.getText());
    assertEquals(CH, token.getType());

    // i
    token = lexer.nextToken();
    assertEquals("i", token.getText());
    assertEquals(CH, token.getType());

    // <space>
    token = lexer.nextToken();
    assertEquals("  ", token.getText());
    assertEquals(CH, token.getType());
    assertEquals(2, token.getCharPositionInLine());

    // \u0021
    token = lexer.nextToken();
    assertEquals("\\u0021", token.getText());
    assertEquals(CH, token.getType());
    assertEquals(4, token.getCharPositionInLine());

    // %{
    token = lexer.nextToken();
    assertEquals("%{", token.getText());
    assertEquals(P_START, token.getType());
    assertEquals(10, token.getCharPositionInLine());
  }


  @SuppressWarnings("SameParameterValue")
  private MessageLexer createFor(String msg) {
    return new MessageLexer(CharStreams.fromString(msg));
  }
}
