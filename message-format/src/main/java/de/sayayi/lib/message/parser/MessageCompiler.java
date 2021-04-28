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
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.DataNumber;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.*;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.ParameterizedMessage;
import de.sayayi.lib.message.internal.TextMessage;
import de.sayayi.lib.message.internal.part.MessagePart;
import de.sayayi.lib.message.internal.part.ParameterPart;
import de.sayayi.lib.message.internal.part.TextPart;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static java.lang.Character.isSpaceChar;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;


/**
 * @author Jeroen Gremmen
 */
@NoArgsConstructor(access = PRIVATE)
public final class MessageCompiler
{
  public static Message.WithSpaces compileMessage(String text) {
    return new Parser(text).message().value;
  }




  private static final class Lexer extends MessageLexer
  {
    @SuppressWarnings("deprecation")
    public Lexer(@NotNull String message) {
      super(new ANTLRInputStream(message));
    }


    @Override
    public Vocabulary getVocabulary() {
      return MessageVocabulary.INSTANCE;
    }
  }




  private static final class Parser extends MessageParser
  {
    private final String message;


    public Parser(@NotNull String message)
    {
      super(new BufferedTokenStream(new Lexer(message)));

      this.message = message;

      addParseListener(new ParserListener());

      removeErrorListeners();  // remove default console polluter...
      addErrorListener(new ErrorListener());

      setErrorHandler(new ErrorHandler());
    }


    @Override
    public Vocabulary getVocabulary() {
      return MessageVocabulary.INSTANCE;
    }


    @Override
    public void exitRule()
    {
      // fix ANTLR bug
      if (!getErrorHandler().inErrorRecoveryMode(this))
        super.exitRule();
    }




    private final class ParserListener extends MessageParserBaseListener
    {
      @Override
      public void exitMessage(MessageContext ctx) {
        ctx.value = ctx.message0().value;
      }


      @Override
      public void exitMessage0(Message0Context ctx)
      {
        final List<MessagePart> parts = ctx.children.stream()
            .map(pt -> pt instanceof ParameterContext
                ? ((ParameterContext)pt).value : ((TextPartContext)pt).value)
            .collect(toList());
        final int partCount = parts.size();

        if (partCount == 0)
          ctx.value = EmptyMessage.INSTANCE;
        else if (partCount == 1 && parts.get(0) instanceof TextPart)
          ctx.value = new TextMessage((TextPart)parts.get(0));
        else
          ctx.value = new ParameterizedMessage(parts);
      }


      @Override
      public void exitTextPart(TextPartContext ctx) {
        ctx.value = new TextPart(ctx.text().value);
      }


      @Override
      public void exitText(TextContext ctx)
      {
        final StringBuilder text = new StringBuilder();

        for(final TerminalNode chNode: ctx.CH())
        {
          final String chText = chNode.getText();
          final char firstChar = chText.charAt(0);

          if (isSpaceChar(firstChar))
            text.append(' ');
          else if (firstChar == '\\')
          {
            // handle escape characters
            text.append(chText.length() == 2
                ? chText.charAt(1) : (char)Integer.parseInt(chText.substring(2), 16));
          }
          else
            text.append(firstChar);
        }

        ctx.value = text.toString();
      }


      @Override
      public void exitQuotedMessage(QuotedMessageContext ctx) {
        ctx.value = ctx.message0().value;
      }


      @Override
      public void exitString(StringContext ctx)
      {
        final TextContext text = ctx.text();
        ctx.value = text == null ? "" : text.value;
      }


      @Override
      public void exitForceQuotedMessage(ForceQuotedMessageContext ctx)
      {
        final QuotedMessageContext quotedMessage = ctx.quotedMessage();
        ctx.value = quotedMessage != null ? quotedMessage.value : MessageFactory.parse(ctx.string().value);
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


      @Override
      public void exitDataString(DataStringContext ctx) {
        ctx.value = new DataString(ctx.string().value);
      }


      @Override
      public void exitDataNumber(DataNumberContext ctx) {
        ctx.value = new DataNumber(ctx.NUMBER().getText());
      }


      @Override
      public void exitDataMap(DataMapContext ctx) {
        ctx.value = new DataMap(ctx.map().value);
      }


      @Override
      public void exitMap(MapContext ctx)
      {
        ctx.value = ctx.mapElements().value;

        final ForceQuotedMessageContext forceQuotedMessage = ctx.forceQuotedMessage();
        if (forceQuotedMessage != null)
          ctx.value.put(null, new MapValueMessage(forceQuotedMessage.value));
      }


      @Override
      public void exitMapElements(MapElementsContext ctx)
      {
        ctx.value = ctx.mapElement().stream()
            .collect(toMap(mec -> mec.key, mec -> mec.value, (a, b) -> b, LinkedHashMap::new));
      }


      @Override
      public void exitMapElement(MapElementContext ctx)
      {
        ctx.key = ctx.mapKey().key;
        ctx.value = ctx.mapValue().value;
      }


      @Override
      public void exitMapKeyString(MapKeyStringContext ctx) {
        ctx.key = new MapKeyString(ctx.relationalOperatorOptional().cmp, ctx.string().value);
      }


      @Override
      public void exitMapKeyNumber(MapKeyNumberContext ctx) {
        ctx.key = new MapKeyNumber(ctx.relationalOperatorOptional().cmp, ctx.NUMBER().getText());
      }


      @Override
      public void exitMapKeyBool(MapKeyBoolContext ctx) {
        ctx.key = new MapKeyBool(ctx.BOOL().getText());
      }


      @Override
      public void exitMapKeyNull(MapKeyNullContext ctx) {
        ctx.key = new MapKeyNull(ctx.equalOperatorOptional().cmp);
      }


      @Override
      public void exitMapKeyEmpty(MapKeyEmptyContext ctx) {
        ctx.key = new MapKeyEmpty(ctx.equalOperatorOptional().cmp);
      }


      @Override
      public void exitMapKeyName(MapKeyNameContext ctx) {
        ctx.key = new MapKeyName(ctx.NAME().getText());
      }


      @Override
      public void exitMapValueString(MapValueStringContext ctx) {
        ctx.value = new MapValueString(ctx.string().value);
      }


      @Override
      public void exitMapValueNumber(MapValueNumberContext ctx) {
        ctx.value = new MapValueNumber(ctx.NUMBER().getText());
      }


      @Override
      public void exitMapValueBool(MapValueBoolContext ctx) {
        ctx.value = new MapValueBool(ctx.BOOL().getText());
      }


      @Override
      public void exitMapValueMessage(MapValueMessageContext ctx) {
        ctx.value = new MapValueMessage(ctx.quotedMessage().value);
      }


      @Override
      public void exitRelationalOperatorOptional(RelationalOperatorOptionalContext ctx)
      {
        final RelationalOperatorContext relationalOperator = ctx.relationalOperator();
        ctx.cmp = relationalOperator == null ? CompareType.EQ : relationalOperator.cmp;
      }


      @Override
      public void exitRelationalOperator(RelationalOperatorContext ctx)
      {
        final EqualOperatorContext equalOperator = ctx.equalOperator();

        if (equalOperator != null)
          ctx.cmp = equalOperator.cmp;
        else
          switch(((TerminalNode)ctx.getChild(0)).getSymbol().getType())
          {
            case LTE:
              ctx.cmp = CompareType.LTE;
              break;

            case LT:
              ctx.cmp = CompareType.LT;
              break;

            case GT:
              ctx.cmp = CompareType.GT;
              break;

            case GTE:
              ctx.cmp = CompareType.GTE;
              break;
          }
      }


      @Override
      public void exitEqualOperatorOptional(EqualOperatorOptionalContext ctx)
      {
        final EqualOperatorContext equalOperator = ctx.equalOperator();
        ctx.cmp = equalOperator == null ? CompareType.EQ : equalOperator.cmp;
      }


      @Override
      public void exitEqualOperator(EqualOperatorContext ctx) {
        ctx.cmp = ctx.EQ() != null ? CompareType.EQ : CompareType.NE;
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
  }




  private static final class ErrorHandler extends DefaultErrorStrategy
  {
    @Override
    protected String getTokenErrorDisplay(Token t) {
      return t != null && t.getType() == Token.EOF ? "end of message" : super.getTokenErrorDisplay(t);
    }
  }
}
