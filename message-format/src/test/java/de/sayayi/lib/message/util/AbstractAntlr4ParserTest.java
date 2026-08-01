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
package de.sayayi.lib.message.util;

import de.sayayi.lib.antlr4.syntax.SyntaxErrorException;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;


/**
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("AbstractAntlr4Parser")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AbstractAntlr4ParserTest
{
  private Calculator calculator;


  @BeforeEach
  public void init()
  {
    var messageSupport = MessageSupportFactory
        .create(DefaultFormatterService.getSharedInstance());

    messageSupport.addMessage("div-by-0", "division by zero: %{left} / 0");

    calculator = new Calculator(messageSupport.seal());
  }




  @Nested
  @DisplayName("Syntax errors")
  class SyntaxErrorTest
  {
    @Test
    @DisplayName("Invalid character in input")
    void testInvalidCharacter()
    {
      assertEquals("token recognition error at: '&'",
          assertThrowsExactly(SyntaxErrorException.class, () -> calculator.calculate("3 & 4")).getErrorMessage());
    }


    @Test
    @DisplayName("Incomplete expression (trailing operator)")
    void testIncompleteExpression()
    {
      assertEquals("mismatched input <EOF> expecting {<number>, '-', '('}",
          assertThrowsExactly(SyntaxErrorException.class, () -> calculator.calculate("3 +")).getErrorMessage());
    }


    @Test
    @DisplayName("Missing operator between numbers")
    void testMissingOperator()
    {
      assertEquals("extraneous input '4' expecting <EOF>",
          assertThrowsExactly(SyntaxErrorException.class, () -> calculator.calculate("3 4")).getErrorMessage());
    }


    @Test
    @DisplayName("Unmatched opening parenthesis")
    void testUnmatchedOpeningParen()
    {
      assertEquals("missing ')' at <EOF>",
          assertThrowsExactly(SyntaxErrorException.class, () -> calculator.calculate("(3 + 4")).getErrorMessage());
    }


    @Test
    @DisplayName("Unmatched closing parenthesis")
    void testUnmatchedClosingParen()
    {
      assertEquals("extraneous input ')' expecting <EOF>",
          assertThrowsExactly(SyntaxErrorException.class, () -> calculator.calculate("3 + 4)")).getErrorMessage());
    }


    @Test
    @DisplayName("Empty input")
    void testEmptyInput()
    {
      assertEquals("mismatched input <EOF> expecting {<number>, '-', '('}",
          assertThrowsExactly(SyntaxErrorException.class, () -> calculator.calculate("")).getErrorMessage());
    }


    @Test
    @DisplayName("Only operator")
    void testOnlyOperator()
    {
      assertEquals("mismatched input '/' expecting {<number>, '-', '('}",
          assertThrowsExactly(SyntaxErrorException.class, () -> calculator.calculate("/")).getErrorMessage());
    }
  }




  @Nested
  @DisplayName("Semantic errors")
  class SemanticErrorTest
  {
    @Test
    @DisplayName("Division by zero")
    void testDivisionByZero()
    {
      assertEquals("""
          division by zero: 10 / 0
  
            10 / 0
            ^^^^^^
          """,
          assertThrowsExactly(SyntaxErrorException.class, () -> calculator.calculate("10 / 0")).getMessage());
    }


    @Test
    @DisplayName("Division by zero in subexpression")
    void testDivisionByZeroSubexpression()
    {
      assertEquals("""
          division by zero: -2 / 0
          
            (1 - 3) / 0 + 5
            ^^^^^^^^^^^
          """,
          assertThrowsExactly(SyntaxErrorException.class, () -> calculator.calculate("(1 - 3) / 0 + 5")).getMessage());
    }


    @Test
    @DisplayName("Number out of range")
    void testNumberOutOfRange()
    {
      var ex = assertThrowsExactly(SyntaxErrorException.class,
          () -> calculator.calculate("1 + 99999999999999999999"));

      assertEquals("""
          number 99999999999999999999 is out of range
          
            1 + 99999999999999999999
                ^^^^^^^^^^^^^^^^^^^^
          """,
          ex.getMessage());
    }
  }
}
