package de.sayayi.lib.message.parser;

import static de.sayayi.lib.message.parser.MessageLexer.TokenType.ARROW;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.BOOLEAN;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.COMMA;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.MAP_END;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.MAP_START;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.NAME;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.NUMBER;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.PARAM_END;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.PARAM_START;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.TEXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.junit.Test;

import de.sayayi.lib.message.parser.MessageLexer.Token;


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
    assertEquals(new Token(0, 5, TEXT, "test", 0, false, false), lexer.next());
    assertFalse(lexer.hasNext());

    lexer = new MessageLexer(" true  ").iterator();
    assertEquals(new Token(0, 6, TEXT, "true", 0, false, false), lexer.next());
    assertFalse(lexer.hasNext());
  }


  @Test
  public void testSpaceBefore()
  {
    Iterator<Token> lexer = new MessageLexer(" this is a test").iterator();
    assertEquals(new Token(0, 14, TEXT, "this is a test", 0, false, false), lexer.next());
    assertFalse(lexer.hasNext());

    lexer = new MessageLexer("abc %{ ' x%{} y' } def").iterator();
    assertEquals(new Token(0, 3, TEXT, "abc", 0, false, true), lexer.next());
    assertEquals(new Token(4, 5, PARAM_START, "%{", 0, true, false), lexer.next());
    assertEquals(new Token(9, 9, TEXT, "x", 0, false, false), lexer.next());
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
    assertEquals(new Token(11, 15, BOOLEAN, "FaLsE", 0, false, false), lexer.next());
    assertEquals(new Token(17, 17, PARAM_END, "}", 0, false, false), lexer.next());
    assertFalse(lexer.hasNext());
  }
}
