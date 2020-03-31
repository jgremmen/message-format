/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.antlr.ANTLRInputStream;
import de.sayayi.lib.message.antlr.Lexer;
import de.sayayi.lib.message.antlr.Token;
import org.junit.Test;

import static de.sayayi.lib.message.parser.MessageLexer.CH;
import static de.sayayi.lib.message.parser.MessageLexer.PARAM_START;
import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class MessageTokenizerTest
{
  @Test
  public void testDefaultMode()
  {
    Lexer lexer = createFor("hi  \\u0021%{");

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
    assertEquals("!", token.getText());
    assertEquals(CH, token.getType());
    assertEquals(4, token.getCharPositionInLine());

    // %{
    token = lexer.nextToken();
    assertEquals("%{", token.getText());
    assertEquals(PARAM_START, token.getType());
    assertEquals(10, token.getCharPositionInLine());
  }


  private MessageLexer createFor(String msg) {
    return new MessageLexer(new ANTLRInputStream(msg));
  }
}
