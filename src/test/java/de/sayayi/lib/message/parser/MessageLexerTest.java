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

import de.sayayi.lib.message.parser.MessageLexer.Token;
import org.junit.Test;

import java.util.Iterator;

import static de.sayayi.lib.message.parser.MessageLexer.TokenType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 * @author Jeroen Gremmen
 */
public class MessageLexerTest
{
  @Test
  public void testTextOnly()
  {
    Iterator<Token> lexer = new MessageLexer("this is a test").iterator();
    assertEquals(new Token(0, 13, TEXT, "this is a test", 0, false, false), lexer.next());
    assertFalse(lexer.hasNext());

    lexer = new MessageLexer("test  ").iterator();
    assertEquals(new Token(0, 5, TEXT, "test", 0, false, true), lexer.next());
    assertFalse(lexer.hasNext());

    lexer = new MessageLexer(" true  ").iterator();
    assertEquals(new Token(0, 6, TEXT, "true", 0, true, true), lexer.next());
    assertFalse(lexer.hasNext());
  }


  @Test
  public void testSpaceBefore()
  {
    Iterator<Token> lexer = new MessageLexer(" this is a test").iterator();
    assertEquals(new Token(0, 14, TEXT, "this is a test", 0, true, false), lexer.next());
    assertFalse(lexer.hasNext());

    lexer = new MessageLexer("abc %{ ' x%{} y' } def").iterator();
    assertEquals(new Token(0, 3, TEXT, "abc", 0, false, true), lexer.next());
    assertEquals(new Token(4, 5, PARAM_START, "%{", 0, true, false), lexer.next());
    assertEquals(new Token(8, 9, TEXT, "x", 0, true, false), lexer.next());
    assertEquals(new Token(10, 11, PARAM_START, "%{", 0, false, false), lexer.next());
    assertEquals(new Token(12, 12, PARAM_END, "}", 0, false, true), lexer.next());
    assertEquals(new Token(13, 14, TEXT, "y", 0, true, false), lexer.next());
    assertEquals(new Token(17, 17, PARAM_END, "}", 0, false, true), lexer.next());
    assertEquals(new Token(18, 21, TEXT, "def", 0, true, false), lexer.next());
    assertFalse(lexer.hasNext());
  }




  @Test
  public void testParameterOnly()
  {
    final Iterator<Token> lexer = new MessageLexer("%{abc}").iterator();
    assertEquals(new Token(0, 1, PARAM_START, "%{", 0, false, false), lexer.next());
    assertEquals(new Token(2, 4, NAME, "abc", 0, false, false), lexer.next());
    assertEquals(new Token(5, 5, PARAM_END, "}", 0, false, false), lexer.next());
    assertFalse(lexer.hasNext());
  }


  @Test
  public void testParameterContent()
  {
    // this is not a valid message but the lexer should return the correct tokens nonetheless
    final Iterator<Token> lexer = new MessageLexer("%{a,b{->true,false 4567'test'}}").iterator();
    assertEquals(new Token(0, 1, PARAM_START, "%{", 0, false, false), lexer.next());
    assertEquals(new Token(2, 2, NAME, "a", 0, false, false), lexer.next());
    assertEquals(new Token(3, 3, COMMA, ",", 0, false, false), lexer.next());
    assertEquals(new Token(4, 4, NAME, "b", 0, false, false), lexer.next());
    assertEquals(new Token(5, 5, MAP_START, "{", 0, false, false), lexer.next());
    assertEquals(new Token(6, 7, ARROW, "->", 0, false, false), lexer.next());
    assertEquals(new Token(8, 11, BOOLEAN, "true", 1, false, false), lexer.next());
    assertEquals(new Token(12, 12, COMMA, ",", 0, false, false), lexer.next());
    assertEquals(new Token(13, 17, BOOLEAN, "false", 0, false, false), lexer.next());
    assertEquals(new Token(19, 22, NUMBER, "4567", 4567, false, false), lexer.next());
    assertEquals(new Token(24, 27, TEXT, "test", 0, false, false), lexer.next());
    assertEquals(new Token(29, 29, MAP_END, "}", 0, false, false), lexer.next());
    assertEquals(new Token(30, 30, PARAM_END, "}", 0, false, false), lexer.next());
    assertFalse(lexer.hasNext());
  }


  @Test
  public void testParameterInSingleQuotedText()
  {
    // this is not a valid message but the lexer should return the correct tokens nonetheless
    final Iterator<Token> lexer = new MessageLexer("%{a'%{ b }',}").iterator();
    assertEquals(new Token(0, 1, PARAM_START, "%{", 0, false, false), lexer.next());
    assertEquals(new Token(2, 2, NAME, "a", 0, false, false), lexer.next());
    assertEquals(new Token(4, 5, PARAM_START, "%{", 0, false, false), lexer.next());
    assertEquals(new Token(7, 7, NAME, "b", 0, false, false), lexer.next());
    assertEquals(new Token(9, 9, PARAM_END, "}", 0, false, false), lexer.next());
    assertEquals(new Token(10, 10, TEXT, "", 0, false, false), lexer.next());
    assertEquals(new Token(11, 11, COMMA, ",", 0, false, false), lexer.next());
    assertEquals(new Token(12, 12, PARAM_END, "}", 0, false, false), lexer.next());
    assertFalse(lexer.hasNext());
  }


  @Test
  public void testParameterInDoubleQuotedText()
  {
    // this is not a valid message but the lexer should return the correct tokens nonetheless
    final Iterator<Token> lexer = new MessageLexer("%{a\"%{ b }\"FaLsE }").iterator();
    assertEquals(new Token(0, 1, PARAM_START, "%{", 0, false, false), lexer.next());
    assertEquals(new Token(2, 2, NAME, "a", 0, false, false), lexer.next());
    assertEquals(new Token(4, 5, PARAM_START, "%{", 0, false, false), lexer.next());
    assertEquals(new Token(7, 7, NAME, "b", 0, false, false), lexer.next());
    assertEquals(new Token(9, 9, PARAM_END, "}", 0, false, false), lexer.next());
    assertEquals(new Token(10, 10, TEXT, "", 0, false, false), lexer.next());
    assertEquals(new Token(11, 15, BOOLEAN, "FaLsE", 0, false, false), lexer.next());
    assertEquals(new Token(17, 17, PARAM_END, "}", 0, false, false), lexer.next());
    assertFalse(lexer.hasNext());
  }


  @Test
  public void testCompareType()
  {
    final Iterator<Token> lexer = new MessageLexer("%{a,{<0->'negative',=0->'zero',>0->'positive'}}").iterator();
    assertEquals(new Token(0, 1, PARAM_START, "%{", 0, false, false), lexer.next());
    assertEquals(new Token(2, 2, NAME, "a", 0, false, false), lexer.next());
    assertEquals(new Token(3, 3, COMMA, ",", 0, false, false), lexer.next());
    assertEquals(new Token(4, 4, MAP_START, "{", 0, false, false), lexer.next());
    assertEquals(new Token(5, 5, LT, "<", 0, false, false), lexer.next());
    assertEquals(new Token(6, 6, NUMBER, "0", 0, false, false), lexer.next());
    assertEquals(new Token(7, 8, ARROW, "->", 0, false, false), lexer.next());
    assertEquals(new Token(10, 17, TEXT, "negative", 0, false, false), lexer.next());
    assertEquals(new Token(19, 19, COMMA, ",", 0, false, false), lexer.next());
    assertEquals(new Token(20, 20, EQ, "=", 0, false, false), lexer.next());
    assertEquals(new Token(21, 21, NUMBER, "0", 0, false, false), lexer.next());
    assertEquals(new Token(22, 23, ARROW, "->", 0, false, false), lexer.next());
    assertEquals(new Token(25, 28, TEXT, "zero", 0, false, false), lexer.next());
    assertEquals(new Token(30, 30, COMMA, ",", 0, false, false), lexer.next());
    assertEquals(new Token(31, 31, GT, ">", 0, false, false), lexer.next());
    assertEquals(new Token(32, 32, NUMBER, "0", 0, false, false), lexer.next());
    assertEquals(new Token(33, 34, ARROW, "->", 0, false, false), lexer.next());
    assertEquals(new Token(36, 43, TEXT, "positive", 0, false, false), lexer.next());
    assertEquals(new Token(45, 45, MAP_END, "}", 0, false, false), lexer.next());
    assertEquals(new Token(46, 46, PARAM_END, "}", 0, false, false), lexer.next());
  }
}
