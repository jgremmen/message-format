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
package de.sayayi.lib.message.internal.parser;

import de.sayayi.lib.antlr4.AbstractAntlr4Parser;
import de.sayayi.lib.antlr4.AbstractVocabulary;
import de.sayayi.lib.antlr4.syntax.GenericSyntaxErrorFormatter;
import de.sayayi.lib.antlr4.syntax.SyntaxErrorFormatter;
import de.sayayi.lib.antlr4.walker.Walker;
import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.TextMessage;
import de.sayayi.lib.message.internal.part.TemplatePart;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.parameter.ParameterConfig;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import de.sayayi.lib.message.part.parameter.key.*;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.part.parameter.value.*;
import de.sayayi.lib.message.util.SpacesUtil;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;
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

import static de.sayayi.lib.antlr4.walker.Walker.WALK_EXIT_RULES_HEAP;
import static de.sayayi.lib.message.exception.MessageParserException.Type.MESSAGE;
import static de.sayayi.lib.message.exception.MessageParserException.Type.TEMPLATE;
import static de.sayayi.lib.message.internal.parser.MessageParser.*;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Character.isSpaceChar;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;
import static java.util.stream.Collector.Characteristics.UNORDERED;
import static java.util.stream.Collectors.toList;
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
  private static final SyntaxErrorFormatter SYNTAX_ERROR_FORMATTER =
      new GenericSyntaxErrorFormatter(1, 0, 0 ,2);

  private final @NotNull MessageFactory messageFactory;


  public MessageCompiler(@NotNull MessageFactory messageFactory)
  {
    super(SYNTAX_ERROR_FORMATTER);

    this.messageFactory = requireNonNull(messageFactory, "messageFactory must not be null");
  }


  /**
   * Compile the given message {@code text} into a space-aware message object.
   *
   * @param text  message text, not {@code null}
   *
   * @return  compiled message, never {@code null}
   *
   * @throws MessageParserException  in case the message could not be parsed
   */
  @Contract(pure = true)
  public @NotNull Message.WithSpaces compileMessage(@NotNull @Language("MessageFormat") String text) {
    return compileMessage(text, false);
  }


  /**
   * Compile the given template {@code text} into a space-aware message object.
   *
   * @param text  template text, not {@code null}
   *
   * @return  compiled template, never {@code null}
   *
   * @throws MessageParserException  in case the template could not be parsed
   */
  @Contract(pure = true)
  public @NotNull Message.WithSpaces compileTemplate(@NotNull @Language("MessageFormat") String text) {
    return compileMessage(text, true);
  }


  @Contract(pure = true)
  private @NotNull Message.WithSpaces compileMessage(@NotNull @Language("MessageFormat") String text, boolean template)
  {
    final var listener = new Listener(template);

    try {
      return parse(new Lexer(text), lexer -> new Parser(listener.tokenStream = new BufferedTokenStream(lexer)),
          Parser::message, listener, ctx -> requireNonNull(ctx.messageWithSpaces));
    } catch(MessageParserException ex) {
      throw ex.withType(template ? TEMPLATE : MESSAGE);
    }
  }


  @Override
  protected @NotNull RuntimeException createException(@NotNull Token startToken, @NotNull Token stopToken,
                                                      @NotNull String formattedMessage, @NotNull String errorMsg,
                                                      Exception cause) {
    return new MessageParserException(errorMsg, formattedMessage, cause);
  }


  @Override
  protected @NotNull String createTokenRecognitionMessage(@NotNull org.antlr.v4.runtime.Lexer lexer,
                                                          @NotNull String text, boolean hasEOF) {
    return "message syntax error at " + (hasEOF ? getEOFTokenDisplayText() : getQuotedDisplayText(text));
  }


  @Override
  protected @NotNull String createNoViableAlternativeMessage(@NotNull org.antlr.v4.runtime.Parser parser,
                                                             @NotNull Token startToken,
                                                             @NotNull Token offendingToken)
  {
    if (isEOFToken(startToken))
      return "incomplete message format";

    final var parserRuleContext = parser.getRuleContext();

    if (parserRuleContext instanceof ConfigMapElementContext)
      return "syntax error in message parameter map element at " + getTokenDisplayText(parser, offendingToken);

    return "message syntax error at " +
        getQuotedDisplayText(parser.getInputStream().getText(startToken, offendingToken));
  }


  private static final IntervalSet IVS_NAME = new IntervalSet(SQ_START, DQ_START, BOOL, NAME, NULL, EMPTY);
  private static final IntervalSet IVS_PARAMETER_CLOSE = new IntervalSet(COMMA, P_END);

  @Override
  protected @NotNull String createInputMismatchMessage(@NotNull org.antlr.v4.runtime.Parser parser,
                                                       @NotNull IntervalSet expectedTokens,
                                                       Token mismatchLocationNearToken)
  {
    final var parserRuleContext = parser.getRuleContext();

    if (IVS_NAME.equals(expectedTokens))
    {
      if (parserRuleContext instanceof SimpleStringContext &&
          parserRuleContext.parent instanceof ParameterNameContext)
        return "missing parameter name at " + getTokenDisplayText(parser, mismatchLocationNearToken);

      if (parserRuleContext instanceof ForceQuotedMessageContext &&
          parserRuleContext.parent instanceof ParameterPartContext)
      {
        if (isEOFToken(mismatchLocationNearToken))
          return "pre-mature end of message parameter reached; missing default message";
        else
          return "missing default message in parameter at " + getTokenDisplayText(parser, mismatchLocationNearToken);
      }

      return "missing string or name in parameter at " + getTokenDisplayText(parser, mismatchLocationNearToken);
    }

    if (IVS_PARAMETER_CLOSE.equals(expectedTokens) && parserRuleContext instanceof ParameterPartContext)
      return "end of message parameter expected at " + getTokenDisplayText(parser, mismatchLocationNearToken);

    return super.createInputMismatchMessage(parser, expectedTokens, mismatchLocationNearToken);
  }


  @Override
  protected @NotNull String createUnwantedTokenMessage(@NotNull org.antlr.v4.runtime.Parser parser,
                                                       @NotNull Token unwantedToken,
                                                       @NotNull IntervalSet expectedTokens)
  {
    final var ctx = parser.getContext();

    if (isEOFToken(unwantedToken) && ctx instanceof ParameterPartContext)
      return "pre-mature end of message parameter reached; missing '}'";

    return super.createUnwantedTokenMessage(parser, unwantedToken, expectedTokens);
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




  private final class Listener extends MessageParserBaseListener implements WalkerSupplier
  {
    private final boolean template;
    private TokenStream tokenStream;


    private Listener(boolean template) {
      this.template = template;
    }


    @Override
    public @NotNull Walker getWalker() {
      return WALK_EXIT_RULES_HEAP;
    }


    @Override
    public void exitMessage(MessageContext ctx) {
      ctx.messageWithSpaces = ctx.message0().messageWithSpaces;
    }


    @Override
    public void exitMessage0(Message0Context ctx)
    {
      var children = ctx.children;
      if (children == null || children.isEmpty())
        ctx.messageWithSpaces = EmptyMessage.INSTANCE;
      else
      {
        var parts = new ArrayList<MessagePart>();

        for(var part: children)
        {
          if (part instanceof ParameterPartContext)
            parts.add(((ParameterPartContext)part).part);
          else if (part instanceof TextPartContext)
            parts.add(((TextPartContext)part).part);
          else
          {
            if (template)
            {
              syntaxError("no nested template allowed")
                  .with(part)
                  .report();
            }

            parts.add(((TemplatePartContext)part).part);
          }
        }

        final MessagePart part0;

        if (parts.size() == 1 && (part0 = parts.get(0)) instanceof TextPart)
          ctx.messageWithSpaces = new TextMessage((TextPart)part0);
        else
        {
          parts.removeIf(this::exitMessage0_isRedundantTextPart);
          ctx.messageWithSpaces = new CompoundMessage(parts);
        }
      }
    }


    @Contract(pure = true)
    private boolean exitMessage0_isRedundantTextPart(@NotNull MessagePart messagePart)
    {
      if (messagePart instanceof TextPart)
      {
        var textPart = (TextPart)messagePart;
        return textPart.isEmpty() && textPart.isSpaceAround();
      }

      return false;
    }


    @Override
    public void exitTextPart(TextPartContext ctx)
    {
      ctx.part = messageFactory
          .getMessagePartNormalizer()
          .normalize(new TextPart(ctx.text().characters));
    }


    @Override
    public void exitText(TextContext ctx)
    {
      var chNodes = ctx.CH();
      var text = new char[chNodes.size()];
      int n = 0;

      for(var chNode: chNodes)
      {
        var chText = chNode.getText();
        var ch = chText.charAt(0);

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

      ctx.characters = new String(text, 0, n);
    }


    @Override
    public void exitQuotedMessage(QuotedMessageContext ctx) {
      ctx.messageWithSpaces = ctx.message0().messageWithSpaces;
    }


    @Override
    public void exitQuotedString(QuotedStringContext ctx)
    {
      var text = ctx.text();
      ctx.string = text == null ? "" : text.characters;
    }


    @Override
    public void exitSimpleString(SimpleStringContext ctx)
    {
      var nameOrKeyword = ctx.nameOrKeyword();
      ctx.string = nameOrKeyword != null ? nameOrKeyword.name : ctx.quotedString().string;
    }


    @Override
    public void exitForceQuotedMessage(ForceQuotedMessageContext ctx)
    {
      var quotedMessage = ctx.quotedMessage();
      if (quotedMessage != null)
        ctx.messageWithSpaces = quotedMessage.messageWithSpaces;
      else
      {
        //noinspection LanguageMismatch
        ctx.messageWithSpaces = messageFactory.parseMessage(ctx.simpleString().string);
      }
    }


    final Collector<ParameterConfigElementContext,Map<ConfigKey,ConfigValue>,Map<ConfigKey,ConfigValue>>
        PARAMETER_CONFIG_COLLECTOR = new Collector<>() {
      @Override public Supplier<Map<ConfigKey,ConfigValue>> supplier() { return LinkedHashMap::new; }
      @Override public BinaryOperator<Map<ConfigKey,ConfigValue>> combiner() { return foldCombiner(); }
      @Override public Set<Characteristics> characteristics() { return Set.of(IDENTITY_FINISH); }

      @Override
      public BiConsumer<Map<ConfigKey,ConfigValue>,ParameterConfigElementContext> accumulator()
      {
        return (map,cec) -> {
          for(var key: cec.configKeys)
            if (map.put(key, cec.configValue) != null)
            {
              syntaxError("duplicate config element " + key + " for parameter '" +
                  ((ParameterPartContext)cec.parent).parameterName().name + '\'')
                  .with(cec)
                  .report();
            }
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
      var mapElements =
          ctx.parameterConfigElement().stream().collect(PARAMETER_CONFIG_COLLECTOR);

      var forceQuotedMessage = ctx.forceQuotedMessage();
      if (forceQuotedMessage != null)
        mapElements.put(null, new ConfigValueMessage(forceQuotedMessage.messageWithSpaces));

      var name = ctx.parameterName().name;
      if (name.isEmpty())
      {
        syntaxError("parameter name must not be empty")
            .with(ctx.parameterName())
            .report();
      }

      var parameterFormat = ctx.parameterFormat();

      ctx.part = messageFactory.getMessagePartNormalizer().normalize(new ParameterPart(
          name, parameterFormat == null ? null : parameterFormat.format,
          isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          new ParameterConfig(mapElements)));
    }


    @Override
    public void exitParameterName(ParameterNameContext ctx) {
      ctx.name = ctx.simpleString().string;
    }


    @Override
    public void exitParameterFormat(ParameterFormatContext ctx) {
      ctx.format = ctx.nameOrKeyword().name;
    }


    final Collector<ConfigNamedElementContext,Map<String,ConfigValue>,Map<String,ConfigValue>>
        TEMPLATE_NAMED_PARAMETER_COLLECTOR = new Collector<>() {
      @Override public Supplier<Map<String,ConfigValue>> supplier() { return TreeMap::new; }
      @Override public BinaryOperator<Map<String,ConfigValue>> combiner() { return foldCombiner(); }
      @Override public Set<Characteristics> characteristics() { return Set.of(UNORDERED); }

      @Override
      public BiConsumer<Map<String,ConfigValue>,ConfigNamedElementContext> accumulator()
      {
        return (map,cpec) -> {
          var name = cpec.configKey.getName();

          if (map.containsKey(name))
          {
            syntaxError("duplicate template default parameter '" + name + "'")
                .with(cpec)
                .report();
          }

          map.put(name, cpec.configValue);
        };
      }


      @Override
      public Function<Map<String,ConfigValue>,Map<String,ConfigValue>> finisher() {
        return identity();
      }
    };


    final Collector<TemplateParameterDelegateContext,Map<String,String>,Map<String,String>>
        TEMPLATE_PARAMETER_DELEGATE_COLLECTOR = new Collector<>() {
      @Override public Supplier<Map<String,String>> supplier() { return HashMap::new; }
      @Override public BinaryOperator<Map<String,String>> combiner() { return foldCombiner(); }
      @Override public Set<Characteristics> characteristics() { return Set.of(UNORDERED); }

      @Override
      public BiConsumer<Map<String,String>,TemplateParameterDelegateContext> accumulator()
      {
        return (map,tpdc) -> {
          if (map.containsKey(tpdc.parameter))
          {
            syntaxError("duplicate template parameter delegate '" + tpdc.parameter + "'")
                .with(tpdc)
                .report();
          }

          map.put(tpdc.parameter, tpdc.delegatedParameter);
        };
      }


      @Override
      public Function<Map<String,String>,Map<String,String>> finisher() {
        return identity();
      }
    };


    @Override
    public void exitTemplatePart(TemplatePartContext ctx)
    {
      var name = ctx.templateName().name;
      if (name.isEmpty())
      {
        syntaxError("template name must not be empty")
            .with(ctx.templateName())
            .report();
      }

      ctx.part = new TemplatePart(
          name,
          isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          ctx.configNamedElement().stream().collect(TEMPLATE_NAMED_PARAMETER_COLLECTOR),
          ctx.templateParameterDelegate().stream().collect(TEMPLATE_PARAMETER_DELEGATE_COLLECTOR));
    }


    @Override
    public void exitTemplateName(TemplateNameContext ctx) {
      ctx.name = ctx.simpleString().string;
    }


    @Override
    public void exitTemplateParameterDelegate(TemplateParameterDelegateContext ctx)
    {
      ctx.parameter = ((SimpleStringContext)ctx.getChild(0)).string;
      ctx.delegatedParameter = ((SimpleStringContext)ctx.getChild(2)).string;
    }


    @Override
    public void exitParameterConfigElement(ParameterConfigElementContext ctx)
    {
      var configNamedElementContext = ctx.configNamedElement();
      if (configNamedElementContext != null)
      {
        ctx.configKeys = List.of(configNamedElementContext.configKey);
        ctx.configValue = configNamedElementContext.configValue;
      }
      else
      {
        var configMapElementContext = ctx.configMapElement();

        ctx.configKeys = configMapElementContext.configKeys;
        ctx.configValue = configMapElementContext.configValue;
      }
    }


    @Override
    public void exitConfigMapMessage(ConfigMapMessageContext ctx)
    {
      ctx.configKeys = ctx.configMapKeys().configKeys;
      ctx.configValue = new ConfigValueMessage(ctx.quotedMessage().messageWithSpaces);
    }


    @Override
    public void exitConfigMapString(ConfigMapStringContext ctx)
    {
      ctx.configKeys = ctx.configMapKeys().configKeys;
      ctx.configValue = new ConfigValueString(ctx.simpleString().string);
    }


    @Override
    public void exitConfigNamedBool(ConfigNamedBoolContext ctx)
    {
      ctx.configKey = new ConfigKeyName(ctx.NAME().getText());
      ctx.configValue = parseBoolean(ctx.BOOL().getText()) ? ConfigValueBool.TRUE : ConfigValueBool.FALSE;
    }


    @Override
    public void exitConfigNamedNumber(ConfigNamedNumberContext ctx)
    {
      ctx.configKey = new ConfigKeyName(ctx.NAME().getText());
      ctx.configValue = new ConfigValueNumber(parseLong(ctx.NUMBER().getText()));
    }


    @Override
    public void exitConfigNamedMessage(ConfigNamedMessageContext ctx)
    {
      ctx.configKey = new ConfigKeyName(ctx.NAME().getText());
      ctx.configValue = new ConfigValueMessage(ctx.quotedMessage().messageWithSpaces);
    }


    @Override
    public void exitConfigNamedString(ConfigNamedStringContext ctx)
    {
      ctx.configKey = new ConfigKeyName(ctx.NAME().getText());
      ctx.configValue = new ConfigValueString(ctx.simpleString().string);
    }


    @Override
    public void exitConfigMapKeys(ConfigMapKeysContext ctx)
    {
      ctx.configKeys = ctx
          .configMapKey()
          .stream()
          .map(configMapKeyContext -> configMapKeyContext.configKey)
          .collect(toList());
    }


    @Override
    public void exitConfigMapKeyNull(ConfigMapKeyNullContext ctx) {
      ctx.configKey = ctx.equalOperatorOptional().cmp == CompareType.EQ ? ConfigKeyNull.EQ : ConfigKeyNull.NE;
    }


    @Override
    public void exitConfigMapKeyEmpty(ConfigMapKeyEmptyContext ctx) {
      ctx.configKey = ctx.equalOperatorOptional().cmp == CompareType.EQ ? ConfigKeyEmpty.EQ : ConfigKeyEmpty.NE;
    }


    @Override
    public void exitConfigMapKeyBool(ConfigMapKeyBoolContext ctx) {
      ctx.configKey = parseBoolean(ctx.BOOL().getText()) ? ConfigKeyBool.TRUE : ConfigKeyBool.FALSE;
    }


    @Override
    public void exitConfigMapKeyNumber(ConfigMapKeyNumberContext ctx) {
      ctx.configKey = new ConfigKeyNumber(ctx.relationalOperatorOptional().cmp, parseLong(ctx.NUMBER().getText()));
    }


    @Override
    public void exitConfigMapKeyString(ConfigMapKeyStringContext ctx) {
      ctx.configKey = new ConfigKeyString(ctx.relationalOperatorOptional().cmp, ctx.quotedString().string);
    }


    @Override
    public void exitRelationalOperatorOptional(RelationalOperatorOptionalContext ctx)
    {
      var relationalOperator = ctx.relationalOperator();
      ctx.cmp = relationalOperator == null ? CompareType.EQ : relationalOperator.cmp;
    }


    @Override
    public void exitRelationalOperator(RelationalOperatorContext ctx)
    {
      var equalOperator = ctx.equalOperator();
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
      var equalOperator = ctx.equalOperator();
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
        var token = tokenStream.get(i);
        if (token.getType() != EOF)
        {
          var text = token.getText();
          return !SpacesUtil.isEmpty(text) && isSpaceChar(text.charAt(0));
        }
      }

      return false;
    }
  }


  @Contract(pure = true)
  private static <K,V> BinaryOperator<Map<K,V>> foldCombiner() {
    return (map1,map2) -> { map1.putAll(map2); return map1; };
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
      add(L_PAREN, "'('", "L_PAREN");
      add(R_PAREN, "')'", "R_PAREN");
    }
  };
}
