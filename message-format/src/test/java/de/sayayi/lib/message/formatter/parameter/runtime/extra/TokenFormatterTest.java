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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.antlr4.syntax.SyntaxErrorException;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.formatter.parameter.runtime.NumberFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.util.Calculator;
import de.sayayi.lib.message.util.CalculatorLexer;
import de.sayayi.lib.message.util.CalculatorParser;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("Token formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class TokenFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;
  private Token token;


  @BeforeEach
  void init()
  {
    var formatterService = new GenericFormatterService();

    formatterService.addFormatter(new TokenFormatter());
    formatterService.addFormatter(new NumberFormatter());

    var configurableMessageSupport = MessageSupportFactory.create(formatterService);
    configurableMessageSupport.addMessage("div-by-0", "division by zero: %{left} / 0");

    messageSupport = configurableMessageSupport.seal();

    // Use Calculator to produce a SyntaxErrorException containing a Token.
    // "3 4" has a missing operator between numbers, so '4' at column 2 is the error token.
    token = assertThrowsExactly(SyntaxErrorException.class,
        () -> new Calculator(messageSupport).calculate("3 4")).getStartToken();
  }


  @Test
  @DisplayName("Formattable type is Token")
  void testFormattableType() {
    assertFormatterForType(new TokenFormatter(), Token.class);
  }


  @Test
  @DisplayName("Format token text (default)")
  void testFormatText()
  {
    assertEquals("4", messageSupport.message("%{t}").with("t", token).format());
    assertEquals("4", messageSupport.message("%{t,token:'text'}").with("t", token).format());
  }


  @Test
  @DisplayName("Format token type")
  void testFormatType()
  {
    assertEquals(Integer.toString(CalculatorParser.NUMBER),
        messageSupport.message("%{t,token:'type'}").with("t", token).format());
  }


  @Test
  @DisplayName("Format token channel")
  void testFormatChannel()
  {
    assertEquals(Integer.toString(CalculatorLexer.DEFAULT_TOKEN_CHANNEL),
        messageSupport.message("%{t,token:'channel'}").with("t", token).format());
  }


  @Test
  @DisplayName("Format token line")
  void testFormatLine() {
    assertEquals("1", messageSupport.message("%{t,token:'line'}").with("t", token).format());
  }


  @Test
  @DisplayName("Format token column")
  void testFormatColumn() {
    assertEquals("2", messageSupport.message("%{t,token:'column'}").with("t", token).format());
  }


  @Test
  @DisplayName("Format token position (line:column)")
  void testFormatPosition() {
    assertEquals("1:2", messageSupport.message("%{t,token:'position'}").with("t", token).format());
  }


  @Test
  @DisplayName("Format token with invalid token config returns null text")
  void testFormatInvalidConfig() {
    assertEquals("", messageSupport.message("%{t,token:'invalid'}").with("t", token).format());
  }




  @Nested
  @DisplayName("Position with different inputs")
  class PositionTest
  {
    @Test
    @DisplayName("Token at start of expression has column 0")
    void testTokenAtStart()
    {
      // "/" is an invalid start token at column 0
      var calculator = new Calculator(messageSupport);
      var ex = assertThrowsExactly(SyntaxErrorException.class, () -> calculator.calculate("/"));
      var startToken = ex.getStartToken();

      assertEquals("1:0", messageSupport
          .message("%{t,token:'position'}").with("t", startToken).format());
    }


    @Test
    @DisplayName("Token with adjusted 1st-column")
    void testAdjustedColumn()
    {
      assertEquals("3", messageSupport
          .message("%{t,token:'column',token-1st-column:1}").with("t", token).format());
    }


    @Test
    @DisplayName("Token with adjusted 1st-line")
    void testAdjustedLine()
    {
      assertEquals("5", messageSupport
          .message("%{t,token:'line',token-1st-line:5}").with("t", token).format());
    }


    @Test
    @DisplayName("Custom position format with line only")
    void testCustomPositionFormatLineOnly()
    {
      final var mockToken = mock(Token.class);

      when(mockToken.getLine()).thenReturn(42);
      when(mockToken.getCharPositionInLine()).thenReturn(7);

      assertEquals("line 42", messageSupport
          .message("%{t,token:position,token-position-format:'line %{line}'}")
          .with("t", mockToken).format());
    }


    @Test
    @DisplayName("Custom position format with line and column")
    void testCustomPositionFormat()
    {
      final var mockToken = mock(Token.class);

      when(mockToken.getLine()).thenReturn(3);
      when(mockToken.getCharPositionInLine()).thenReturn(12);

      assertEquals("(3, 12)", messageSupport
          .message("%{t,token:position,token-position-format:'(%{line}, %{column})'}")
          .with("t", mockToken).format());
    }


    @Test
    @DisplayName("Custom position format when token has no column")
    void testCustomPositionFormatNoColumn()
    {
      final var mockToken = mock(Token.class);

      when(mockToken.getLine()).thenReturn(5);
      when(mockToken.getCharPositionInLine()).thenReturn(-1);

      assertEquals("(5, )", messageSupport
          .message("%{t,token:position,token-position-format:'(%{line}, %{column})'}")
          .with("t", mockToken).format());
    }


    @Test
    @DisplayName("Custom position format with no line yields empty")
    void testCustomPositionFormatNoLine()
    {
      final var mockToken = mock(Token.class);

      when(mockToken.getLine()).thenReturn(0);
      when(mockToken.getCharPositionInLine()).thenReturn(5);

      assertEquals("", messageSupport
          .message("%{t,token:position,token-position-format:'%{line}-%{column}'}")
          .with("t", mockToken).format());
    }


    @Test
    @DisplayName("Custom position format with adjusted 1st-line and 1st-column")
    void testCustomPositionFormatAdjusted()
    {
      final var mockToken = mock(Token.class);

      when(mockToken.getLine()).thenReturn(1);
      when(mockToken.getCharPositionInLine()).thenReturn(0);

      assertEquals("0/1", messageSupport
          .message("%{t,token:'position',token-position-format:'%{line}/%{column}',token-1st-line:0,token-1st-column:1}")
          .with("t", mockToken)
          .format());
    }
  }
}
