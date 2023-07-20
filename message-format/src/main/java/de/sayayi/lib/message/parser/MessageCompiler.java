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

import de.sayayi.lib.antlr4.AbstractAntlr4Parser;
import de.sayayi.lib.antlr4.AbstractVocabulary;
import de.sayayi.lib.antlr4.syntax.GenericSyntaxErrorFormatter;
import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.TextMessage;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.TemplatePart;
import de.sayayi.lib.message.part.TextPart;
import de.sayayi.lib.message.part.parameter.ParamConfig;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import de.sayayi.lib.message.part.parameter.key.*;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.part.parameter.value.*;
import de.sayayi.lib.message.util.SpacesUtil;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static de.sayayi.lib.message.parser.MessageLexer.BOOL;
import static de.sayayi.lib.message.parser.MessageLexer.COLON;
import static de.sayayi.lib.message.parser.MessageLexer.COMMA;
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
import static de.sayayi.lib.message.parser.MessageLexer.P_END;
import static de.sayayi.lib.message.parser.MessageLexer.P_START;
import static de.sayayi.lib.message.parser.MessageParser.CH;
import static de.sayayi.lib.message.parser.MessageParser.DQ_END;
import static de.sayayi.lib.message.parser.MessageParser.DQ_START;
import static de.sayayi.lib.message.parser.MessageParser.SQ_END;
import static de.sayayi.lib.message.parser.MessageParser.SQ_START;
import static de.sayayi.lib.message.parser.MessageParser.TPL_END;
import static de.sayayi.lib.message.parser.MessageParser.TPL_START;
import static de.sayayi.lib.message.parser.MessageParser.*;
import static de.sayayi.lib.message.util.StreamUtil.foldCombiner;
import static de.sayayi.lib.message.util.StreamUtil.unmodifyableMapFinisher;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Character.isSpaceChar;
import static java.lang.Integer.parseInt;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;
import static java.util.stream.Collector.Characteristics.UNORDERED;
import static org.antlr.v4.runtime.Token.EOF;


/**
 * This class provides methods for compiling messages and templates.
 *
 * @author Jeroen Gremmen
 * @since 0.5.0
 */
@SuppressWarnings("UnknownLanguage")
public final class MessageCompiler extends AbstractAntlr4Parser
{
  private final @NotNull MessageFactory messageFactory;


  public MessageCompiler(@NotNull MessageFactory messageFactory)
  {
    super(ErrorFormatter.INSTANCE);

    this.messageFactory = requireNonNull(messageFactory, "messageFactory must not be null");
  }


  /**
   * Compile the given message {@code text} into a spaces aware message object.
   *
   * @param text  message text, not {@code null}
   *
   * @return  compiled message, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Message.WithSpaces compileMessage(
      @NotNull @Language("MessageFormat") String text) {
    return compileMessage(text, false);
  }


  /**
   * Compile the given template {@code text} into a spaces aware message object.
   *
   * @param text  template text, not {@code null}
   *
   * @return  compiled template, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Message.WithSpaces compileTemplate(
      @NotNull @Language("MessageFormat") String text) {
    return compileMessage(text, true);
  }


  @Contract(pure = true)
  private @NotNull Message.WithSpaces compileMessage(
      @NotNull @Language("MessageFormat") String text, boolean template)
  {
    final Listener listener = new Listener(template);

    return parse(new Lexer(text),
        lexer -> new Parser(listener.tokenStream = new BufferedTokenStream(lexer)),
        Parser::message, listener, ctx -> requireNonNull(ctx.value));
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
    private final boolean template;
    private TokenStream tokenStream;


    private Listener(boolean template) {
      this.template = template;
    }


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
        final List<MessagePart> parts = new ArrayList<>();

        for(final ParseTree part: ctx.children)
        {
          if (part instanceof ParameterPartContext)
            parts.add(((ParameterPartContext)part).value);
          else if (part instanceof TextPartContext)
            parts.add(((TextPartContext)part).value);
          else
          {
            if (template)
              syntaxError((TemplatePartContext)part, "no nested template allowed");

            parts.add(((TemplatePartContext)part).value);
          }
        }

        final int partCount = parts.size();

        if (partCount == 0)
          ctx.value = EmptyMessage.INSTANCE;
        else if (partCount == 1 && parts.get(0) instanceof TextPart)
          ctx.value = new TextMessage((TextPart)parts.get(0));
        else
        {
          parts.removeIf(this::exitMessage0_isRedundantTextPart);
          ctx.value = new CompoundMessage(parts);
        }
      }
    }


    @Contract(pure = true)
    private boolean exitMessage0_isRedundantTextPart(@NotNull MessagePart messagePart)
    {
      if (messagePart instanceof TextPart)
      {
        final TextPart textPart = (TextPart)messagePart;
        return "".equals(textPart.getText()) && textPart.isSpaceAround();
      }

      return false;
    }


    @Override
    public void exitTextPart(TextPartContext ctx)
    {
      ctx.value = messageFactory
          .getMessagePartNormalizer()
          .normalize(new TextPart(ctx.text().value));
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
              : (char)parseInt(chText.substring(2), 16);
        }

        if (!isSpaceChar(ch))
          text[n++] = ch;
        else if (n == 0 || !isSpaceChar(text[n - 1]))
          text[n++] = ' ';
      }

      ctx.value = new String(text, 0, n);
    }


    @Override
    public void exitQuotedMessage(QuotedMessageContext ctx) {
      ctx.value = ctx.message0().value;
    }


    @Override
    public void exitQuotedString(QuotedStringContext ctx)
    {
      final TextContext text = ctx.text();
      ctx.value = text == null ? "" : text.value;
    }


    @Override
    public void exitForceQuotedMessage(ForceQuotedMessageContext ctx)
    {
      final QuotedMessageContext quotedMessage = ctx.quotedMessage();
      if (quotedMessage != null)
        ctx.value = quotedMessage.value;
      else
      {
        final NameOrKeywordContext nameOrKeyword = ctx.nameOrKeyword();
        ctx.value = messageFactory.parseMessage(
            nameOrKeyword != null ? nameOrKeyword.name : ctx.quotedString().value);
      }
    }


    final Collector<ConfigElementContext,Map<ConfigKey,ConfigValue>,Map<ConfigKey,ConfigValue>>
        PARAMETER_CONFIG_COLLECTOR =
        new Collector<ConfigElementContext,Map<ConfigKey,ConfigValue>,Map<ConfigKey,ConfigValue>>() {
      @Override public Supplier<Map<ConfigKey,ConfigValue>> supplier() { return LinkedHashMap::new; }
      @Override public BinaryOperator<Map<ConfigKey,ConfigValue>> combiner() { return foldCombiner(); }
      @Override public Set<Characteristics> characteristics() { return singleton(IDENTITY_FINISH); }

      @Override
      public BiConsumer<Map<ConfigKey,ConfigValue>,ConfigElementContext> accumulator()
      {
        return (map,cec) -> {
          final ConfigKey key = cec.key;

          if (map.containsKey(key))
          {
            final String parameter = ((ParameterPartContext)cec.parent).name.name;
            syntaxError(cec, "duplicate config element " + key + " for parameter '" +
                parameter + '\'');
          }

          map.put(key, cec.value);
        };
      }

      @Override
      public Function<Map<ConfigKey,ConfigValue>,Map<ConfigKey,ConfigValue>> finisher() {
        return identity();
      }
    };


    @Override
    public void exitParameterPart(ParameterPartContext ctx)
    {
      final Map<ConfigKey,ConfigValue> mapElements =
          ctx.configElement().stream().collect(PARAMETER_CONFIG_COLLECTOR);
      final ForceQuotedMessageContext forceQuotedMessage = ctx.forceQuotedMessage();
      if (forceQuotedMessage != null)
        mapElements.put(null, new ConfigValueMessage(forceQuotedMessage.value));

      ctx.value = messageFactory.getMessagePartNormalizer().normalize(new ParameterPart(
          ctx.name.name, ctx.format == null ? null : ctx.format.name,
          isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          new ParamConfig(mapElements)));
    }


    final Collector<ConfigParameterElementContext,Map<String,ConfigValue>,Map<String,ConfigValue>>
        TEMPLATE_CONFIG_PARAMETER_COLLECTOR =
        new Collector<ConfigParameterElementContext,Map<String,ConfigValue>,Map<String,ConfigValue>>() {
      @Override public Supplier<Map<String,ConfigValue>> supplier() { return TreeMap::new; }
      @Override public BinaryOperator<Map<String,ConfigValue>> combiner() { return foldCombiner(); }
      @Override public Set<Characteristics> characteristics() { return singleton(UNORDERED); }

      @Override
      public BiConsumer<Map<String,ConfigValue>,ConfigParameterElementContext> accumulator()
      {
        return (map,cpec) -> {
          final String name = cpec.key.getName();

          if (map.containsKey(name))
            syntaxError(cpec, "duplicate template default parameter '" + name + "'");

          map.put(name, cpec.value);
        };
      }

      @Override
      public Function<Map<String,ConfigValue>,Map<String,ConfigValue>> finisher() {
        return unmodifyableMapFinisher();
      }
    };


    @Override
    public void exitTemplatePart(TemplatePartContext ctx)
    {
      ctx.value = new TemplatePart(ctx.nameOrKeyword().name,
          isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          ctx.configParameterElement().stream().collect(TEMPLATE_CONFIG_PARAMETER_COLLECTOR));
    }


    @Override
    public void exitConfigElement(ConfigElementContext ctx)
    {
      final ConfigParameterElementContext cpec = ctx.configParameterElement();

      if (cpec != null)
      {
        ctx.key = cpec.key;
        ctx.value = cpec.value;
      }
      else
      {
        final ConfigMapElementContext cmec = ctx.configMapElement();

        ctx.key = cmec.key;
        ctx.value = cmec.value;
      }
    }


    @Override
    public void exitConfigMapMessage(ConfigMapMessageContext ctx)
    {
      ctx.key = ctx.configMapKey().key;
      ctx.value = new ConfigValueMessage(ctx.quotedMessage().value);
    }


    @Override
    public void exitConfigMapString(ConfigMapStringContext ctx)
    {
      final QuotedStringContext quotedStringContext = ctx.quotedString();

      ctx.key = ctx.configMapKey().key;
      ctx.value = new ConfigValueString(quotedStringContext != null
          ? quotedStringContext.value : ctx.nameOrKeyword().name);
    }


    @Override
    public void exitConfigParameterBool(ConfigParameterBoolContext ctx)
    {
      ctx.key = new ConfigKeyName(ctx.NAME().getText());
      ctx.value = parseBoolean(ctx.BOOL().getText()) ? ConfigValueBool.TRUE : ConfigValueBool.FALSE;
    }


    @Override
    public void exitConfigParameterNumber(ConfigParameterNumberContext ctx)
    {
      ctx.key = new ConfigKeyName(ctx.NAME().getText());
      ctx.value = new ConfigValueNumber(Long.parseLong(ctx.NUMBER().getText()));
    }


    @Override
    public void exitConfigParameterMessage(ConfigParameterMessageContext ctx)
    {
      ctx.key = new ConfigKeyName(ctx.NAME().getText());
      ctx.value = new ConfigValueMessage(ctx.quotedMessage().value);
    }


    @Override
    public void exitConfigParameterString(ConfigParameterStringContext ctx)
    {
      final QuotedStringContext quotedStringContext = ctx.quotedString();

      ctx.key = new ConfigKeyName(ctx.NAME().getText());
      ctx.value = new ConfigValueString(quotedStringContext != null
          ? quotedStringContext.value : ctx.nameOrKeyword().name);
    }


    @Override
    public void exitConfigMapKeyNull(ConfigMapKeyNullContext ctx) {
      ctx.key = new ConfigKeyNull(ctx.equalOperatorOptional().cmp);
    }


    @Override
    public void exitConfigMapKeyEmpty(ConfigMapKeyEmptyContext ctx) {
      ctx.key = new ConfigKeyEmpty(ctx.equalOperatorOptional().cmp);
    }


    @Override
    public void exitConfigMapKeyBool(ConfigMapKeyBoolContext ctx) {
      ctx.key = parseBoolean(ctx.BOOL().getText()) ? ConfigKeyBool.TRUE : ConfigKeyBool.FALSE;
    }


    @Override
    public void exitConfigMapKeyNumber(ConfigMapKeyNumberContext ctx) {
      ctx.key = new ConfigKeyNumber(ctx.relationalOperatorOptional().cmp, ctx.NUMBER().getText());
    }


    @Override
    public void exitConfigMapKeyString(ConfigMapKeyStringContext ctx) {
      ctx.key = new ConfigKeyString(ctx.relationalOperatorOptional().cmp, ctx.quotedString().value);
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


    private boolean isSpaceAtTokenIndex(int i)
    {
      if (i >= 0)
      {
        final Token token = tokenStream.get(i);

        if (token.getType() != EOF)
        {
          final String text = token.getText();
          return !SpacesUtil.isEmpty(text) && isSpaceChar(text.charAt(0));
        }
      }

      return false;
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
      add(CH, "<character>", "CH");
      add(COLON, "':'", "COLON");
      add(COMMA, "','", "COMMA");
      add(DQ_END, "\"", "DQ_END");
      add(DQ_START, "\"", "DQ_START");
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
      add(P_END, "'}'", "P_END");
      add(P_START, "'%{'", "P_START");
      add(SQ_END, "'", "SQ_END");
      add(SQ_START, "'", "SQ_START");
      add(TPL_START, "'%['", "TPL_START");
      add(TPL_END, "']'", "TPL_END");
    }
  };
}
