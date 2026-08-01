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

import de.sayayi.lib.antlr4.AbstractVocabulary;
import de.sayayi.lib.antlr4.syntax.GenericSyntaxErrorFormatter;
import de.sayayi.lib.antlr4.syntax.SyntaxErrorFormatter;
import de.sayayi.lib.message.MessageSupport;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Vocabulary;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.util.CalculatorParser.*;


/**
 * Simple integer calculator parser used for testing {@link AbstractAntlr4Parser}.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
final class Calculator extends AbstractAntlr4Parser
{
  private static final SyntaxErrorFormatter SYNTAX_ERROR_FORMATTER =
      new GenericSyntaxErrorFormatter(1, 0, 0, 2);

  private static final Vocabulary VOCABULARY = new AbstractVocabulary() {
    @Override
    protected void addTokens()
    {
      add(NUMBER, "<number>", "NUMBER");
      add(PLUS, "'+'", "PLUS");
      add(MINUS, "'-'", "MINUS");
      add(STAR, "'*'", "STAR");
      add(SLASH, "'/'", "SLASH");
      add(LPAREN, "'('", "LPAREN");
      add(RPAREN, "')'", "RPAREN");
    }
  };


  Calculator(@NotNull MessageSupport messageSupport) {
    super(SYNTAX_ERROR_FORMATTER, messageSupport);
  }


  @Contract(pure = true)
  @SuppressWarnings("UnusedReturnValue")
  int calculate(@NotNull String expression) {
    return parse(new Lexer(expression), Parser::new, Parser::calc, new Listener(), ctx -> ctx.result);
  }




  private static final class Lexer extends CalculatorLexer
  {
    private Lexer(@NotNull String expression) {
      super(CharStreams.fromString(expression));
    }


    @Override
    public Vocabulary getVocabulary() {
      return Calculator.VOCABULARY;
    }
  }




  private static final class Parser extends CalculatorParser
  {
    private Parser(@NotNull Lexer lexer) {
      super(new BufferedTokenStream(lexer));
    }


    @Override
    public Vocabulary getVocabulary() {
      return Calculator.VOCABULARY;
    }
  }




  private final class Listener extends CalculatorBaseListener
  {
    @Override
    public void exitCalc(CalcContext ctx) {
      ctx.result = ctx.expr().result;
    }


    @Override
    public void exitNum(NumContext ctx) {
      var text = ctx.NUMBER().getText();

      try {
        ctx.result = Integer.parseInt(text);
      } catch(NumberFormatException ex) {
        syntaxErrorMessage("number %{value} is out of range")
            .with("value", text)
            .with(ctx)
            .withCause(ex)
            .report();
      }
    }


    @Override
    public void exitUnaryMinus(UnaryMinusContext ctx) {
      ctx.result = -ctx.expr().result;
    }


    @Override
    public void exitMulDiv(MulDivContext ctx)
    {
      final var left = ctx.expr(0).result;
      final var right = ctx.expr(1).result;

      if (ctx.op.getType() == STAR)
        ctx.result = left * right;
      else
      {
        if (right == 0)
        {
          syntaxErrorCode("div-by-0")
              .with("left", left)
              .with(ctx)
              .report();
        }

        ctx.result = left / right;
      }
    }


    @Override
    public void exitAddSub(AddSubContext ctx)
    {
      final var left = ctx.expr(0).result;
      final var right = ctx.expr(1).result;

      ctx.result = ctx.op.getType() == PLUS ? left + right : left - right;
    }


    @Override
    public void exitParens(ParensContext ctx) {
      ctx.result = ctx.expr().result;
    }
  }
}
