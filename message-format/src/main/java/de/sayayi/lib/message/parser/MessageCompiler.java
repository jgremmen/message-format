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

import de.sayayi.lib.antlr4.AbstractAntlr4Parser;
import de.sayayi.lib.antlr4.AbstractVocabulary;
import de.sayayi.lib.antlr4.syntax.GenericSyntaxErrorFormatter;
import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.data.map.*;
import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.ParameterizedMessage;
import de.sayayi.lib.message.internal.SpacesUtil;
import de.sayayi.lib.message.internal.TextMessage;
import de.sayayi.lib.message.internal.part.MessagePart;
import de.sayayi.lib.message.internal.part.ParameterPart;
import de.sayayi.lib.message.internal.part.TextPart;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.sayayi.lib.message.parser.MessageLexer.BOOL;
import static de.sayayi.lib.message.parser.MessageLexer.COLON;
import static de.sayayi.lib.message.parser.MessageLexer.COMMA;
import static de.sayayi.lib.message.parser.MessageLexer.DOUBLE_QUOTE_END;
import static de.sayayi.lib.message.parser.MessageLexer.DOUBLE_QUOTE_START;
import static de.sayayi.lib.message.parser.MessageLexer.EMPTY;
import static de.sayayi.lib.message.parser.MessageLexer.EQ;
import static de.sayayi.lib.message.parser.MessageLexer.GT;
import static de.sayayi.lib.message.parser.MessageLexer.GTE;
import static de.sayayi.lib.message.parser.MessageLexer.LT;
import static de.sayayi.lib.message.parser.MessageLexer.LTE;
import static de.sayayi.lib.message.parser.MessageLexer.NAME;
import static de.sayayi.lib.message.parser.MessageLexer.NE;
import static de.sayayi.lib.message.parser.MessageLexer.NULL;
import static de.sayayi.lib.message.parser.MessageLexer.NUMBER;
import static de.sayayi.lib.message.parser.MessageLexer.PARAM_END;
import static de.sayayi.lib.message.parser.MessageLexer.PARAM_START;
import static de.sayayi.lib.message.parser.MessageLexer.SINGLE_QUOTE_END;
import static de.sayayi.lib.message.parser.MessageLexer.SINGLE_QUOTE_START;
import static de.sayayi.lib.message.parser.MessageParser.*;
import static java.lang.Boolean.parseBoolean;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


/**
 * @author Jeroen Gremmen
 */
public final class MessageCompiler extends AbstractAntlr4Parser
{
  private final @NotNull MessageFactory messageFactory;


  public MessageCompiler(@NotNull MessageFactory messageFactory)
  {
    super(ErrorFormatter.INSTANCE);

    this.messageFactory = messageFactory;
  }


  @Contract(pure = true)
  public @NotNull Message.WithSpaces compileMessage(@NotNull String text)
  {
    final Listener listener = new Listener();

    return parse(new Lexer(text),
        lexer -> new Parser(listener.tokenStream = new BufferedTokenStream(lexer)),
        Parser::message, listener, ctx -> ctx.value);
  }


  @Override
  protected @NotNull MessageParserException createException(
      @NotNull Token startToken, @NotNull Token stopToken, @NotNull String formattedMessage,
      @NotNull String errorMsg, RecognitionException ex) {
    return new MessageParserException(formattedMessage, ex);
  }




  private static final class Lexer extends MessageLexer
  {
    public Lexer(@NotNull String message) {
      super(CharStreams.fromString(message));
    }


    @Override
    public Vocabulary getVocabulary() {
      return MessageCompiler.VOCABULARY;
    }
  }




  private static final class Parser extends MessageParser
  {
    public Parser(@NotNull TokenStream tokenStream) {
      super(tokenStream);
    }


    @Override
    public Vocabulary getVocabulary() {
      return MessageCompiler.VOCABULARY;
    }
  }




  private final class Listener extends MessageParserBaseListener
  {
    private TokenStream tokenStream;


    @Override
    public void exitMessage(MessageContext ctx) {
      ctx.value = ctx.message0().value;
    }


    @Override
    public void exitMessage0(Message0Context ctx)
    {
      if (ctx.children == null)
        ctx.value = EmptyMessage.INSTANCE;
      else
      {
        final List<MessagePart> parts = ctx.children
            .stream()
            .map(pt -> pt instanceof ParameterContext
                ? ((ParameterContext)pt).value
                : ((TextPartContext)pt).value)
            .collect(toList());
        final int partCount = parts.size();

        if (partCount == 0)
          ctx.value = EmptyMessage.INSTANCE;
        else if (partCount == 1 && parts.get(0) instanceof TextPart)
          ctx.value = new TextMessage((TextPart)parts.get(0));
        else
          ctx.value = new ParameterizedMessage(parts);
      }
    }


    @Override
    public void exitTextPart(TextPartContext ctx) {
      ctx.value = messageFactory.getMessagePartNormalizer().normalize(new TextPart(ctx.text().value));
    }


    @Override
    public void exitText(TextContext ctx)
    {
      final List<TerminalNode> chNodes = ctx.CH();
      final char[] text = new char[chNodes.size()];
      int n = 0;

      for(final TerminalNode chNode: chNodes)
      {
        final String chText = chNode.getText();
        char ch = chText.charAt(0);

        if (ch == '\\')
        {
          // handle escape characters
          ch = chText.length() == 2
              ? chText.charAt(1)
              : (char)Integer.parseInt(chText.substring(2), 16);
        }

        if (!SpacesUtil.isSpaceChar(ch))
          text[n++] = ch;
        else if (n == 0 || !SpacesUtil.isSpaceChar(text[n - 1]))
          text[n++] = ' ';
      }

      ctx.value = new String(text, 0, n);
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

      ctx.value = quotedMessage != null
          ? quotedMessage.value
          : messageFactory.parse(ctx.string().value);
    }


    @Override
    public void exitParameter(ParameterContext ctx)
    {
      final Map<MapKey,MapValue> mapElements = ctx.mapElement().stream()
          .collect(toMap(mec -> mec.key, mec -> mec.value, (a, b) -> b, LinkedHashMap::new));
      final ForceQuotedMessageContext forceQuotedMessage = ctx.forceQuotedMessage();
      if (forceQuotedMessage != null)
        mapElements.put(null, new MapValueMessage(forceQuotedMessage.value));

      ctx.value = messageFactory.getMessagePartNormalizer().normalize(new ParameterPart(
          ctx.name.name, ctx.format == null ? null : ctx.format.name,
          exitParameter_isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          exitParameter_isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          mapElements));
    }


    private boolean exitParameter_isSpaceAtTokenIndex(int i)
    {
      if (i >= 0)
      {
        final Token token = tokenStream.get(i);

        if (token.getType() != Token.EOF)
        {
          final String text = token.getText();
          return !SpacesUtil.isEmpty(text) && SpacesUtil.isSpaceChar(text.charAt(0));
        }
      }

      return false;
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
      ctx.key = parseBoolean(ctx.BOOL().getText()) ? MapKeyBool.TRUE : MapKeyBool.FALSE;
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
    public void exitMapValueString(MapValueStringContext ctx)
    {
      final StringContext stringContext = ctx.string();

      ctx.value = new MapValueString(stringContext != null ? stringContext.value : ctx.nameOrKeyword().name);
    }


    @Override
    public void exitMapValueNumber(MapValueNumberContext ctx) {
      ctx.value = new MapValueNumber(Long.parseLong(ctx.NUMBER().getText()));
    }


    @Override
    public void exitMapValueBool(MapValueBoolContext ctx) {
      ctx.value = parseBoolean(ctx.BOOL().getText()) ? MapValueBool.TRUE : MapValueBool.FALSE;
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


    @Override
    public void exitNameOrKeyword(NameOrKeywordContext ctx) {
      ctx.name = ctx.getChild(0).getText();
    }
  }




  private static final class ErrorFormatter extends GenericSyntaxErrorFormatter
  {
    private static final ErrorFormatter INSTANCE = new ErrorFormatter();


    private ErrorFormatter() {
      super(1, 0, 0);
    }
  }




  private static final Vocabulary VOCABULARY = new AbstractVocabulary() {
    @Override
    protected void addTokens()
    {
      add(BOOL, "'true' or 'false'", "BOOL");
      add(COLON, "':'", "COLON");
      add(COMMA, "','", "COMMA");
      add(DOUBLE_QUOTE_END, "\"", "DOUBLE_QUOTE_END");
      add(DOUBLE_QUOTE_START, "\"", "DOUBLE_QUOTE_START");
      add(EMPTY, "'empty'", "EMPTY");
      add(EQ, "'='", "EQ");
      add(GT, "'>'", "GT");
      add(GTE, "'>='", "GTE");
      add(LT, "'<'", "LT");
      add(LTE, "'<='", "LTE");
      add(NAME, "<name>", "NAME");
      add(NE, "'<>' or '!'", "NE");
      add(NULL, "'null'", "NULL");
      add(NUMBER, "<number>", "NUMBER");
      add(PARAM_END, "'}'", "PARAM_END");
      add(PARAM_START, "'%{'", "PARAM_START");
      add(SINGLE_QUOTE_END, "'", "SINGLE_QUOTE_END");
      add(SINGLE_QUOTE_START, "'", "SINGLE_QUOTE_START");
    }
  };
}
