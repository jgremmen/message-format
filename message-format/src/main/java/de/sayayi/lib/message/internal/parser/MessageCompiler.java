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
import de.sayayi.lib.message.internal.part.config.MessagePartConfig;
import de.sayayi.lib.message.internal.part.map.MessagePartMap;
import de.sayayi.lib.message.internal.part.map.key.*;
import de.sayayi.lib.message.internal.part.parameter.ParameterPart;
import de.sayayi.lib.message.internal.part.post.PostFormatterPart;
import de.sayayi.lib.message.internal.part.template.TemplatePart;
import de.sayayi.lib.message.internal.part.text.TextPart;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MapKey.CompareType;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.TypedValue;
import de.sayayi.lib.message.util.SpacesUtil;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collector;

import static de.sayayi.lib.antlr4.walker.Walker.WALK_EXIT_RULES_HEAP;
import static de.sayayi.lib.message.exception.MessageParserException.Type.MESSAGE;
import static de.sayayi.lib.message.exception.MessageParserException.Type.TEMPLATE;
import static de.sayayi.lib.message.internal.parser.MessageParser.*;
import static de.sayayi.lib.message.util.MessageUtil.isKebabCaseName;
import static de.sayayi.lib.message.util.MessageUtil.isKebabOrLowerCamelCaseName;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Character.isSpaceChar;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.util.Objects.requireNonNull;
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

    if (parserRuleContext instanceof MapEntryDefaultContext && isEOFToken(offendingToken))
      return "pre-mature end of message parameter reached; missing default message";

    if (parserRuleContext instanceof MapEntryContext)
      return "syntax error in message parameter map element at " + getTokenDisplayText(parser, offendingToken);

    return "message syntax error at " +
        getQuotedDisplayText(parser.getInputStream().getText(startToken, offendingToken));
  }


  @Override
  protected @NotNull String createInputMismatchMessage(@NotNull org.antlr.v4.runtime.Parser parser,
                                                       @NotNull IntervalSet expectedTokens,
                                                       Token mismatchLocationNearToken)
  {
    final var parserRuleContext = parser.getRuleContext();

    if (parserRuleContext instanceof NameOrKeywordContext &&
        new IntervalSet(BOOL, NAME, NULL, EMPTY, FORMAT).equals(expectedTokens))
    {
      if (parserRuleContext.parent instanceof ParameterNameContext)
        return "missing parameter name at " + getTokenDisplayText(parser, mismatchLocationNearToken);

      if (parserRuleContext.parent instanceof TemplateNameContext)
        return "missing template name at " + getTokenDisplayText(parser, mismatchLocationNearToken);
    }

    if (new IntervalSet(SQ_START, DQ_START, BOOL, NAME, NULL, EMPTY, FORMAT).equals(expectedTokens))
    {
      if (parserRuleContext instanceof MapEntryDefaultContext &&
          parserRuleContext.parent instanceof ParameterPartContext)
      {
        if (isEOFToken(mismatchLocationNearToken))
          return "pre-mature end of message parameter reached; missing default message";
        else
          return "missing default message in parameter at " + getTokenDisplayText(parser, mismatchLocationNearToken);
      }

      return "missing string or name in parameter at " + getTokenDisplayText(parser, mismatchLocationNearToken);
    }

    if (parserRuleContext instanceof ParameterPartContext &&
        new IntervalSet(COMMA, P_END).equals(expectedTokens))
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
    @SuppressWarnings("IfCanBeSwitch")
    public void exitMessage0(Message0Context ctx)
    {
      var children = ctx.children;
      if (children == null || children.isEmpty())
        ctx.messageWithSpaces = EmptyMessage.INSTANCE;
      else
      {
        final var parts = new ArrayList<MessagePart>();

        for(var part: children)
        {
          if (part instanceof ParameterPartContext)
            parts.add(((ParameterPartContext)part).part);
          else if (part instanceof TextPartContext)
            parts.add(((TextPartContext)part).part);
          else if (part instanceof PostFormatPartContext)
            parts.add(((PostFormatPartContext)part).part);
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

        if (parts.size() == 1 && (part0 = parts.getFirst()) instanceof TextPart)
          ctx.messageWithSpaces = new TextMessage((TextPart)part0);
        else
        {
          parts.removeIf(this::exitMessage0_isRedundantTextPart);
          ctx.messageWithSpaces = new CompoundMessage(parts);
        }
      }
    }


    @Contract(pure = true)
    private boolean exitMessage0_isRedundantTextPart(@NotNull MessagePart messagePart) {
      return messagePart instanceof TextPart textPart && textPart.isEmpty() && textPart.isSpaceAround();
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
      final var chNodes = ctx.CH();
      final var text = new char[chNodes.size()];
      var n = 0;

      for(var chNode: chNodes)
      {
        final var chText = chNode.getText();
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
      final var text = ctx.text();
      ctx.string = text == null ? "" : text.characters;
    }


    @Override
    public void exitSimpleString(SimpleStringContext ctx)
    {
      final var nameOrKeyword = ctx.nameOrKeyword();
      ctx.string = nameOrKeyword != null ? nameOrKeyword.name : ctx.quotedString().string;
    }


    final Collector<ConfigDefinitionContext,Map<String, TypedValue<?>>,Map<String, TypedValue<?>>>
        PARAMETER_CONFIG_DEFINITION_COLLECTOR = new AbstractMapCollector<>(TreeMap::new) {
      @Override
      protected void accumulator(@NotNull Map<String, TypedValue<?>> map, @NotNull ConfigDefinitionContext context)
      {
        if (map.put(context.name, context.value) != null)
        {
          syntaxError("duplicate config name " + context.name + " for parameter '" +
              ((ParameterPartContext)context.parent).parameterName().name + '\'')
              .with(context)
              .report();
        }
      }
    };


    final Collector<MapEntryContext,Map<MapKey, TypedValue<?>>,Map<MapKey, TypedValue<?>>>
        PARAMETER_MAP_ENTRY_COLLECTOR = new AbstractMapCollector<>(LinkedHashMap::new) {
      @Override
      protected void accumulator(@NotNull Map<MapKey, TypedValue<?>> map, @NotNull MapEntryContext context)
      {
        for(var key: context.keys)
          if (map.put(key, context.value) != null)
          {
            syntaxError("duplicate config element " + key + " for parameter '" +
                ((ParameterPartContext)context.parent).parameterName().name + '\'')
                .with(context)
                .report();
          }
      }
    };


    @Override
    public void exitParameterPart(ParameterPartContext ctx)
    {
      final var mapElements = ctx.mapEntry().stream().collect(PARAMETER_MAP_ENTRY_COLLECTOR);
      final var mapEntryDefault = ctx.mapEntryDefault();

      if (mapEntryDefault != null)
        mapElements.put(null, new TypedValueMessage(mapEntryDefault.messageWithSpaces));

      final var parameterFormat = ctx.parameterFormat();
      final var format = switch(parameterFormat.size()) {
        case 0 -> null;
        case 1 -> parameterFormat.getFirst().format;
        default -> {
          syntaxError("parameter format can occur only once")
              .with(parameterFormat.get(1))
              .report();
          yield null;  // never reached
        }
      };

      ctx.part = messageFactory.getMessagePartNormalizer().normalize(new ParameterPart(
          ctx.parameterName().name, format,
          isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          new MessagePartConfig(ctx.configDefinition().stream().collect(PARAMETER_CONFIG_DEFINITION_COLLECTOR)),
          new MessagePartMap(mapElements)));
    }


    @Override
    public void exitParameterName(ParameterNameContext ctx)
    {
      if (!isKebabOrLowerCamelCaseName(ctx.name = ctx.nameOrKeyword().name))
      {
        syntaxError("parameter name must match the camel- or kebab-case naming convention")
            .with(ctx)
            .report();
      }
    }


    @Override
    public void exitParameterFormat(ParameterFormatContext ctx)
    {
      if (!isKebabCaseName(ctx.format = ctx.nameOrKeyword().name))
      {
        syntaxError("parameter format must match the kebab case naming convention")
            .with(ctx)
            .report();
      }
    }


    final Collector<ConfigDefinitionContext,Map<String, TypedValue<?>>,Map<String, TypedValue<?>>>
        TEMPLATE_CONFIG_DEFINITION_COLLECTOR = new AbstractMapCollector<>(TreeMap::new) {
      @Override
      protected void accumulator(@NotNull Map<String, TypedValue<?>> map, @NotNull ConfigDefinitionContext context) {
        if (map.put(context.name, context.value) != null)
        {
          syntaxError("duplicate template default parameter '" + context.name + "'")
              .with(context)
              .report();
        }
      }
    };


    final Collector<TemplateParameterDelegateContext,Map<String,String>,Map<String,String>>
        TEMPLATE_PARAMETER_DELEGATE_COLLECTOR = new AbstractMapCollector<>(HashMap::new) {
      @Override
      protected void accumulator(@NotNull Map<String,String> map, @NotNull TemplateParameterDelegateContext context)
      {
        if (map.put(context.parameter, context.delegatedParameter) != null)
        {
          syntaxError("duplicate template parameter delegate '" + context.parameter + "'")
              .with(context)
              .report();
        }
      }
    };


    @Override
    public void exitTemplatePart(TemplatePartContext ctx)
    {
      ctx.part = new TemplatePart(
          ctx.templateName().name,
          isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          ctx.configDefinition().stream().collect(TEMPLATE_CONFIG_DEFINITION_COLLECTOR),
          ctx.templateParameterDelegate().stream().collect(TEMPLATE_PARAMETER_DELEGATE_COLLECTOR));
    }


    @Override
    public void exitTemplateName(TemplateNameContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.nameOrKeyword().name))
      {
        syntaxError("template name must match the kebab case naming convention")
            .with(ctx)
            .report();
      }
    }


    @Override
    public void exitTemplateParameterDelegate(TemplateParameterDelegateContext ctx)
    {
      ctx.parameter = ((SimpleStringContext)ctx.getChild(0)).string;
      ctx.delegatedParameter = ((SimpleStringContext)ctx.getChild(2)).string;
    }


    final Collector<ConfigDefinitionContext,Map<String, TypedValue<?>>,Map<String, TypedValue<?>>>
        POST_FORMAT_CONFIG_DEFINITION_COLLECTOR = new AbstractMapCollector<>(TreeMap::new) {
      @Override
      protected void accumulator(@NotNull Map<String, TypedValue<?>> map, @NotNull ConfigDefinitionContext context)
      {
        if (map.put(context.name, context.value) != null)
        {
          syntaxError("duplicate config name " + context.name + " for post formatter '" +
              ((PostFormatPartContext)context.parent).postFormatName().name + '\'')
              .with(context)
              .report();
        }
      }
    };


    @Override
    public void exitPostFormatPart(PostFormatPartContext ctx)
    {
      ctx.part = new PostFormatterPart(
          ctx.postFormatName().name,
          ctx.quotedMessage().messageWithSpaces,
          isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          new MessagePartConfig(ctx.configDefinition().stream().collect(POST_FORMAT_CONFIG_DEFINITION_COLLECTOR)));
    }


    @Override
    public void exitPostFormatName(PostFormatNameContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.nameOrKeyword().name))
      {
        syntaxError("post-format name must match the kebab case naming convention")
            .with(ctx)
            .report();
      }
    }


    @Override
    public void exitMapEntryMessage(MapEntryMessageContext ctx)
    {
      ctx.keys = ctx.mapKeys().keys;
      ctx.value = new TypedValueMessage(ctx.quotedMessage().messageWithSpaces);
    }


    @Override
    public void exitMapEntryString(MapEntryStringContext ctx)
    {
      ctx.keys = ctx.mapKeys().keys;
      ctx.value = new TypedValueString(ctx.simpleString().string);
    }


    @Override
    public void exitMapEntryDefault(MapEntryDefaultContext ctx)
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


    @Override
    public void exitConfigDefinitionBool(ConfigDefinitionBoolContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.NAME().getText()))
      {
        syntaxError("config name for boolean value must match the kebab case naming convention")
            .with(ctx.NAME())
            .report();
      }

      ctx.value = parseBoolean(ctx.BOOL().getText()) ? TypedValueBool.TRUE : TypedValueBool.FALSE;
    }


    @Override
    public void exitConfigDefinitionNumber(ConfigDefinitionNumberContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.NAME().getText()))
      {
        syntaxError("config name for numerical value must match the kebab case naming convention")
            .with(ctx.NAME())
            .report();
      }

      ctx.value = new TypedValueNumber(parseLong(ctx.NUMBER().getText()));
    }


    @Override
    public void exitConfigDefinitionMessage(ConfigDefinitionMessageContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.NAME().getText()))
      {
        syntaxError("config name for message value must match the kebab case naming convention")
            .with(ctx.NAME())
            .report();
      }

      ctx.value = new TypedValueMessage(ctx.quotedMessage().messageWithSpaces);
    }


    @Override
    public void exitConfigDefinitionString(ConfigDefinitionStringContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.NAME().getText()))
      {
        syntaxError("config name for string value must match the kebab case naming convention")
            .with(ctx.NAME())
            .report();
      }

      ctx.value = new TypedValueString(ctx.simpleString().string);
    }


    @Override
    public void exitMapKeys(MapKeysContext ctx)
    {
      ctx.keys = ctx
          .mapKey()
          .stream()
          .map(configMapKeyContext -> configMapKeyContext.key)
          .toList();
    }


    @Override
    public void exitMapKeyNull(MapKeyNullContext ctx)
    {
      final var equalOperator = ctx.equalOperator();

      ctx.key = equalOperator == null || equalOperator.cmp == CompareType.EQ
          ? MapKeyNull.EQ
          : MapKeyNull.NE;
    }


    @Override
    public void exitMapKeyEmpty(MapKeyEmptyContext ctx)
    {
      final var equalOperator = ctx.equalOperator();

      ctx.key = equalOperator == null || equalOperator.cmp == CompareType.EQ
          ? MapKeyEmpty.EQ
          : MapKeyEmpty.NE;
    }


    @Override
    public void exitMapKeyBool(MapKeyBoolContext ctx) {
      ctx.key = parseBoolean(ctx.BOOL().getText()) ? MapKeyBool.TRUE : MapKeyBool.FALSE;
    }


    @Override
    public void exitMapKeyNumber(MapKeyNumberContext ctx)
    {
      final var relationalOperator = ctx.relationalOperator();

      ctx.key = new MapKeyNumber(
          relationalOperator == null ? CompareType.EQ : relationalOperator.cmp,
          parseLong(ctx.NUMBER().getText()));
    }


    @Override
    public void exitMapKeyString(MapKeyStringContext ctx)
    {
      final var relationalOperator = ctx.relationalOperator();

      ctx.key = new MapKeyString(
          relationalOperator == null ? CompareType.EQ : relationalOperator.cmp,
          ctx.quotedString().string);
    }


    @Override
    public void exitRelationalOperator(RelationalOperatorContext ctx)
    {
      var equalOperator = ctx.equalOperator();
      if (equalOperator != null)
        ctx.cmp = equalOperator.cmp;
      else
        switch(((TerminalNode)ctx.getChild(0)).getSymbol().getType()) {
          case LTE -> ctx.cmp = CompareType.LTE;
          case LT -> ctx.cmp = CompareType.LT;
          case GT -> ctx.cmp = CompareType.GT;
          case GTE -> ctx.cmp = CompareType.GTE;
        }
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
          final var text = token.getText();
          return !SpacesUtil.isEmpty(text) && isSpaceChar(text.charAt(0));
        }
      }

      return false;
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
      add(FORMAT, "'format'", "FORMAT");
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
      add(PF_START, "'%('", "PF_START");
      add(PF_END, "')'", "PF_END");
      add(L_PAREN, "'('", "L_PAREN");
      add(R_PAREN, "')'", "R_PAREN");
    }
  };
}
