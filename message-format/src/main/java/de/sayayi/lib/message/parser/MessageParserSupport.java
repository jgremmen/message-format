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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.ParameterizedMessage;
import de.sayayi.lib.message.internal.TextMessage;
import de.sayayi.lib.message.internal.part.MessagePart;
import de.sayayi.lib.message.internal.part.ParameterPart;
import de.sayayi.lib.message.internal.part.TextPart;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Character.isSpaceChar;


/**
 * @author Jeroen Gremmen
 */
public final class MessageParserSupport extends MessageParser
{
  private final String message;


  private MessageParserSupport(String message)
  {
    super(new BufferedTokenStream(new MessageLexer(new ANTLRInputStream(message))));

    this.message = message;

    addParseListener(new ParserListener());

    removeErrorListeners();  // remove default console polluter...
    addErrorListener(new ErrorListener());

    setErrorHandler(new ErrorHandler());
  }


  public static Message.WithSpaces parse(String text)
  {
    MessageContext messageContext = new MessageParserSupport(text).message();

    return messageContext.value;
  }


  @Override
  public Vocabulary getVocabulary() {
    return Vocab.INSTANCE;
  }




  private final class ParserListener extends MessageParserBaseListener
  {
    @Override
    public void exitTextPart(TextPartContext ctx) {
      ctx.value = new TextPart(ctx.text().value);
    }


    @Override
    public void exitMessage0(Message0Context ctx)
    {
      final List<MessagePart> parts = ctx.parts;
      final int partCount = parts.size();

      if (partCount == 0)
        ctx.value = EmptyMessage.INSTANCE;
      else if (partCount == 1 && parts.get(0) instanceof TextPart)
        ctx.value = new TextMessage((TextPart)parts.get(0));
      else
        ctx.value = new ParameterizedMessage(parts);
    }


    @Override
    public void exitParameter(ParameterContext ctx)
    {
      final DataContext data = ctx.data();

      ctx.value = new ParameterPart(ctx.name.getText(),
          ctx.format == null ? null : ctx.format.getText(),
          exitParameter_isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          exitParameter_isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          data == null ? null : data.value);
    }


    @SuppressWarnings("java:S100")
    private boolean exitParameter_isSpaceAtTokenIndex(int i)
    {
      if (i >= 0)
      {
        final Token token = _input.get(i);

        if (token.getType() != Token.EOF)
        {
          final String text = token.getText();
          return text != null && !text.isEmpty() && isSpaceChar(text.charAt(0));
        }
      }

      return false;
    }
  }




  private static final class ErrorHandler extends DefaultErrorStrategy
  {
    @Override
    protected String getTokenErrorDisplay(Token t) {
      return t != null && t.getType() == Token.EOF ? "end of message" : super.getTokenErrorDisplay(t);
    }
  }




  private final class ErrorListener extends BaseErrorListener
  {
    @Override
    public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                            String msg, RecognitionException ex)
    {
      final Token token = (Token)offendingSymbol;
      final StringBuilder text = new StringBuilder(message);

      text.insert(0, ":\n");
      text.insert(0, msg);
      text.append('\n');

      final char[] spaces = new char[charPositionInLine];
      Arrays.fill(spaces, ' ');
      text.append(spaces);

      int stopIndex = token.getType() == Token.EOF ? charPositionInLine : token.getStopIndex();
      final char[] marker = new char[stopIndex + 1 - charPositionInLine];
      Arrays.fill(marker, '^');
      text.append(marker);

      throw new MessageParserException(message, charPositionInLine, stopIndex, text.toString(), ex);
    }
  }




  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class Vocab implements Vocabulary
  {
    private static final Vocabulary INSTANCE = new Vocab();

    private static final Map<Integer,Name> TOKEN_NAMES = new HashMap<>();
    private static int maxTokenType;


    static
    {
      add(MessageLexer.COLON, "':'", "COLON");
      add(MessageLexer.COMMA, "','", "COMMA");
      add(MessageLexer.DOUBLE_QUOTE_END, "\"", "DOUBLE_QUOTE_END");
      add(MessageLexer.DOUBLE_QUOTE_START, "\"", "DOUBLE_QUOTE_START");
      add(MessageLexer.EMPTY, "'empty'", "EMPTY");
      add(MessageLexer.EQ, "'='", "EQ");
      add(MessageLexer.GT, "'>'", "GT");
      add(MessageLexer.GTE, "'>='", "GTE");
      add(MessageLexer.LT, "'<'", "LT");
      add(MessageLexer.LTE, "'<='", "LTE");
      add(MessageLexer.M_BOOL, "'true' or 'false'", "BOOL");
      add(MessageLexer.M_NUMBER, "<number>", "NUMBER");
      add(MessageLexer.MAP_END, "'}'", "MAP_END");
      add(MessageLexer.MAP_START, "'{'", "MAP_START");
      add(MessageLexer.NAME, "<name>", "NAME");
      add(MessageLexer.NE, "'<='", "NE");
      add(MessageLexer.NULL, "'null'", "NULL");
      add(MessageLexer.P_BOOL, "'true' or 'false'", "BOOL");
      add(MessageLexer.P_NUMBER, "<number>", "NUMBER");
      add(MessageLexer.PARAM_END, "'}'", "PARAM_END");
      add(MessageLexer.PARAM_START, "'%{'", "PARAM_START");
      add(MessageLexer.SINGLE_QUOTE_END, "'", "SINGLE_QUOTE_END");
      add(MessageLexer.SINGLE_QUOTE_START, "'", "SINGLE_QUOTE_START");
    }


    @Override
    public int getMaxTokenType() {
      return maxTokenType;
    }


    @Override
    public String getLiteralName(int tokenType) {
      return TOKEN_NAMES.containsKey(tokenType) ? TOKEN_NAMES.get(tokenType).literal : null;
    }


    @Override
    public String getSymbolicName(int tokenType) {
      return TOKEN_NAMES.containsKey(tokenType) ? TOKEN_NAMES.get(tokenType).symbol : null;
    }


    @Override
    public String getDisplayName(int tokenType)
    {
      if (!TOKEN_NAMES.containsKey(tokenType))
        return Integer.toString(tokenType);

      return TOKEN_NAMES.get(tokenType).literal;
    }


    private static void add(int tokenType, String literal, String symbolic)
    {
      TOKEN_NAMES.put(tokenType, new Name(literal, symbolic));

      if (tokenType > maxTokenType)
        maxTokenType = tokenType;
    }




    @AllArgsConstructor
    private static final class Name
    {
      final String literal;
      final String symbol;
    }
  }
}
