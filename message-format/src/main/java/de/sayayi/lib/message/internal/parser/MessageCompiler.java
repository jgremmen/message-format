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
import de.sayayi.lib.message.internal.message.CompoundMessage;
import de.sayayi.lib.message.internal.message.EmptyMessage;
import de.sayayi.lib.message.internal.message.TextMessage;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.internal.part.config.MessagePartConfig;
import de.sayayi.lib.message.internal.part.map.MessagePartMap;
import de.sayayi.lib.message.internal.part.map.key.*;
import de.sayayi.lib.message.internal.part.parameter.ParameterPart;
import de.sayayi.lib.message.internal.part.post.PostFormatterPart;
import de.sayayi.lib.message.internal.part.template.TemplatePart;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MapKey.CompareType;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.TypedValue;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.*;

import static de.sayayi.lib.antlr4.walker.Walker.WALK_EXIT_RULES_HEAP;
import static de.sayayi.lib.message.exception.MessageParserException.Type.MESSAGE;
import static de.sayayi.lib.message.exception.MessageParserException.Type.TEMPLATE;
import static de.sayayi.lib.message.internal.parser.MessageParser.*;
import static de.sayayi.lib.message.util.MessageUtil.*;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Character.isSpaceChar;
import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNull;
import static org.antlr.v4.runtime.Token.EOF;


/**
 * Compiler for parsing message format strings and templates into their structural {@link Message} representation
 * using an ANTLR-based parser. This is the main entry point for turning message format text into an object model that
 * can be formatted at runtime.
 * <p>
 * The compiler supports the full message format syntax including parameters ({@code %{...}}), templates
 * ({@code %[...]}), post-formatters ({@code %(...)}), map entries with typed keys and values and quoted strings.
 * <p>
 * Use {@link #compileMessage(String)} to compile a message format string and {@link #compileTemplate(String)} to
 * compile a template format string. Both methods validate the input syntax and throw a {@link MessageParserException}
 * with a descriptive error message if parsing fails.
 *
 * @author Jeroen Gremmen
 * @since 0.5.0
 *
 * @see MessageFactory
 */
@ApiStatus.Internal
public final class MessageCompiler extends AbstractAntlr4Parser
{
  /** Formats syntax error locations reported for compiled message text. */
  private static final SyntaxErrorFormatter SYNTAX_ERROR_FORMATTER =
      new GenericSyntaxErrorFormatter(1, 0, 0 ,2);

  private final @NotNull MessageFactory messageFactory;


  /**
   * Creates a new message compiler using the given message factory.
   *
   * @param messageFactory  factory used for normalizing and creating message parts, not {@code null}
   */
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


  /**
   * Compiles message text with the shared parser pipeline for either full messages or templates.
   *
   * @param text      message or template text, not {@code null}
   * @param template  {@code true} to compile template input, {@code false} to compile a regular message
   *
   * @return  compiled message model, never {@code null}
   *
   * @throws MessageParserException  if the input contains invalid message-format syntax
   */
  @Contract(pure = true)
  private @NotNull Message.WithSpaces compileMessage(@NotNull @Language("MessageFormat") String text,
                                                     boolean template)
  {
    final var listener = new Listener(template);

    try {
      return parse(new Lexer(text), lexer -> new Parser(listener.tokenStream = new BufferedTokenStream(lexer)),
          Parser::message, listener, ctx -> requireNonNull(ctx.messageWithSpaces));
    } catch(MessageParserException ex) {
      throw ex.withType(template ? TEMPLATE : MESSAGE);
    }
  }


  /**
   * Creates the parser exception type used for syntax errors raised while compiling message text.
   *
   * @param startToken        first token covered by the parser error
   * @param stopToken         last token covered by the parser error
   * @param formattedMessage  formatted error description including location information
   * @param errorMsg          raw parser error message
   * @param cause             underlying parser exception, if available
   *
   * @return  runtime exception to throw for the parse failure
   */
  @Override
  protected @NotNull RuntimeException createException(@NotNull Token startToken, @NotNull Token stopToken,
                                                      @NotNull String formattedMessage, @NotNull String errorMsg,
                                                      Exception cause) {
    return new MessageParserException(errorMsg, formattedMessage, cause);
  }


  /**
   * Creates the error message used when lexing encounters text that cannot be tokenized.
   *
   * @param lexer   lexer that detected the invalid input
   * @param text    offending text fragment
   * @param hasEOF  {@code true} if the invalid fragment reaches the end of the input
   *
   * @return  human-readable token recognition error message
   */
  @Override
  protected @NotNull String createTokenRecognitionMessage(@NotNull org.antlr.v4.runtime.Lexer lexer,
                                                          @NotNull String text, boolean hasEOF) {
    return "message syntax error at " + (hasEOF ? getEOFTokenDisplayText() : getQuotedDisplayText(text));
  }


  /**
   * Creates an error message for parser branches that cannot be resolved from the current input.
   *
   * @param parser          parser that detected the problem
   * @param startToken      first token of the unmatched input sequence
   * @param offendingToken  token near the point where parsing failed
   *
   * @return  human-readable description of the parse failure
   */
  @Override
  protected @NotNull String createNoViableAlternativeMessage(@NotNull org.antlr.v4.runtime.Parser parser,
                                                             @NotNull Token startToken, @NotNull Token offendingToken)
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


  /**
   * Creates an error message for tokens that do not match the current parser expectations.
   *
   * @param parser                     parser that detected the mismatch
   * @param expectedTokens             tokens that would have been valid at the current position
   * @param mismatchLocationNearToken  token near the mismatch location
   *
   * @return  human-readable description of the mismatch
   */
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


  /**
   * Creates an error message for required tokens that are missing from the input.
   *
   * @param parser                    parser that detected the missing token
   * @param expectedTokens            tokens that were expected at the current position
   * @param missingLocationNearToken  token near the point where input is incomplete
   *
   * @return  human-readable description of the missing token
   */
  @Override
  protected @NotNull String createMissingTokenMessage(@NotNull org.antlr.v4.runtime.Parser parser,
                                                      @NotNull IntervalSet expectedTokens,
                                                      Token missingLocationNearToken)
  {
    if (isEOFToken(missingLocationNearToken))
      return "pre-mature end of message parameter reached; missing " + expectedTokens.toString(parser.getVocabulary());

    return super.createMissingTokenMessage(parser, expectedTokens, missingLocationNearToken);
  }


  /**
   * Creates an error message for unexpected tokens that appear in otherwise valid input.
   *
   * @param parser          parser that detected the unwanted token
   * @param unwantedToken   unexpected token
   * @param expectedTokens  tokens that would have been accepted instead
   *
   * @return  human-readable description of the unwanted token
   */
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




  /**
   * Custom vocabulary providing human-readable token display names for error messages. Maps token types
   * (e.g. {@code P_START}, {@code COMMA}) to descriptive labels (e.g. {@code '%\{'}, {@code ','}) so that syntax
   * errors are easier to understand.
   */
  private static final Vocabulary VOCABULARY = new AbstractVocabulary() {
    /** Adds user-friendly display names for message parser tokens. */
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




  /**
   * ANTLR lexer that tokenizes message format strings. Extends the generated {@link MessageLexer} to use the
   * compiler's custom {@link #VOCABULARY} for producing human-readable error messages.
   */
  private static final class Lexer extends MessageLexer
  {
    /**
     * Creates a lexer for message-format input.
     *
     * @param message  message text to tokenize, not {@code null}
     */
    private Lexer(@NotNull String message) {
      super(CharStreams.fromString(message));
    }


    /**
     * Returns the custom vocabulary used for readable token names in error messages.
     *
     * @return  vocabulary used by this lexer
     */
    @Override
    public Vocabulary getVocabulary() {
      return MessageCompiler.VOCABULARY;
    }
  }




  /**
   * ANTLR parser that processes message format token streams. Extends the generated {@link MessageParser} to use the
   * compiler's custom {@link #VOCABULARY} for producing human-readable error messages.
   */
  private static final class Parser extends MessageParser
  {
    /**
     * Creates a parser for message-format tokens.
     *
     * @param tokenStream  token stream to parse, not {@code null}
     */
    private Parser(@NotNull TokenStream tokenStream) {
      super(tokenStream);
    }


    /**
     * Returns the custom vocabulary used for readable token names in error messages.
     *
     * @return  vocabulary used by this parser
     */
    @Override
    public Vocabulary getVocabulary() {
      return MessageCompiler.VOCABULARY;
    }
  }




  /**
   * Parse tree listener that builds the {@link Message} object model from the ANTLR parse tree.
   * This listener is invoked as the parser walks the tree and is responsible for:
   * <ul>
   *   <li>
   *     Constructing message parts (text, parameters, templates, post-formatters)
   *   </li>
   *   <li>
   *     Parsing and validating map keys and typed values
   *   </li>
   *   <li>
   *     Enforcing naming conventions (kebab-case, lower camelCase) for parameters, templates and configuration keys
   *   </li>
   *   <li>
   *     Detecting duplicate map entries, config definitions and template parameter defaults
   *   </li>
   *   <li>
   *     Assembling the final {@link Message.WithSpaces} from its constituent parts
   *   </li>
   * </ul>
   */
  private final class Listener extends MessageParserBaseListener implements WalkerSupplier
  {
    /** Message fragment used in validation errors for kebab-case names. */
    private static final String KEBAB_CASE_MATCH = "must match the kebab case naming convention";

    /** Message fragment used in validation errors for kebab-case or lower camel case names. */
    private static final String KEBAB_LOWER_CAMEL_CASE_MATCH =
        "must match the kebab- or lower camel case naming convention";

    private final boolean template;
    private TokenStream tokenStream;


    /**
     * Creates a listener for either full message parsing or template parsing.
     *
     * @param template  {@code true} when compiling template input, {@code false} for full messages
     */
    private Listener(boolean template) {
      this.template = template;
    }


    /**
     * Returns the parse-tree walker strategy used to build message parts from completed rules.
     *
     * @return  walker used for listener callbacks
     */
    @Override
    public @NotNull Walker getWalker() {
      return WALK_EXIT_RULES_HEAP;
    }


    /**
     * Stores the compiled top-level message result.
     *
     * @param ctx  parsed message context
     */
    @Override
    public void exitMessage(MessageContext ctx) {
      ctx.messageWithSpaces = ctx.message0().messageWithSpaces;
    }


    /**
     * Builds a message body from its parsed text, parameter, template and post-formatter parts.
     *
     * @param ctx  parsed message-body context
     */
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
              syntaxError("no nested template allowed").with(part).report();

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


    /**
     * Determines whether a text part is empty spacer content that can be dropped from a compound message.
     *
     * @param messagePart  message part to inspect
     *
     * @return  {@code true} if the part is redundant spacer text
     */
    @Contract(pure = true)
    private boolean exitMessage0_isRedundantTextPart(@NotNull MessagePart messagePart) {
      return messagePart instanceof TextPart textPart && textPart.isEmpty() && textPart.isSpaceAround();
    }


    /**
     * Builds a normalized text part from parsed message text.
     *
     * @param ctx  parsed text-part context
     */
    @Override
    public void exitTextPart(TextPartContext ctx)
    {
      ctx.part = messageFactory
          .getMessagePartNormalizer()
          .normalize(new TextPart(ctx.text().characters));
    }


    /**
     * Resolves text content by unescaping escaped characters and normalizing whitespace.
     *
     * @param ctx  parsed text context
     */
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


    /**
     * Exposes the compiled content of a quoted message.
     *
     * @param ctx  parsed quoted-message context
     */
    @Override
    public void exitQuotedMessage(QuotedMessageContext ctx) {
      ctx.messageWithSpaces = ctx.message0().messageWithSpaces;
    }


    /**
     * Resolves a quoted string literal to its plain string value.
     *
     * @param ctx  parsed quoted-string context
     */
    @Override
    public void exitQuotedString(QuotedStringContext ctx)
    {
      final var text = ctx.text();
      ctx.string = text == null ? "" : text.characters;
    }


    /**
     * Resolves a simple string from either a quoted literal or a bare name-like token.
     *
     * @param ctx  parsed simple-string context
     */
    @Override
    public void exitSimpleString(SimpleStringContext ctx)
    {
      final var nameOrKeyword = ctx.nameOrKeyword();
      ctx.string = nameOrKeyword != null ? nameOrKeyword.name : ctx.quotedString().string;
    }


    /**
     * Builds a parameter part from its parsed name, format, configuration and map entries.
     *
     * @param ctx  parsed parameter-part context
     */
    @Override
    public void exitParameterPart(ParameterPartContext ctx)
    {
      final Map<String,TypedValue<?>> config;
      final Map<MapKey,TypedValue.MessageValue> map;
      String format = null;

      var parameterEntries = ctx.parameterEntries();
      if (parameterEntries != null)
      {
        format = parameterEntries.format;
        config = parameterEntries.config;
        map = parameterEntries.map;
      }
      else
      {
        config = Map.of();
        map = Map.of();
      }

      ctx.part = messageFactory.getMessagePartNormalizer().normalize(new ParameterPart(
          ctx.parameterName().name, format,
          isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          new MessagePartConfig(config),
          new MessagePartMap(map)));
    }


    /**
     * Validates and stores a parameter name.
     *
     * @param ctx  parsed parameter-name context
     */
    @Override
    public void exitParameterName(ParameterNameContext ctx)
    {
      if (!isKebabOrLowerCamelCaseName(ctx.name = ctx.nameOrKeyword().name))
        syntaxError("parameter name " + KEBAB_LOWER_CAMEL_CASE_MATCH).with(ctx).report();
    }


    /**
     * Collects a parameter's format, configuration values and conditional map entries.
     *
     * @param ctx  parsed parameter-entry collection context
     */
    @Override
    public void exitParameterEntries(ParameterEntriesContext ctx)
    {
      MapEntryDefaultContext _mapEntryDefaultContext = null;

      ctx.config = new TreeMap<>();
      ctx.map = new LinkedHashMap<>();

      for(var entryContext: ctx.parameterEntry())
        switch(entryContext.getChild(0))
        {
          case ParameterFormatContext formatContext -> {
            if (ctx.format != null)
            {
              syntaxError("config parameter 'format' can occur only once")
                  .with(formatContext)
                  .report();
            }

            ctx.format = formatContext.format;
          }
          case ConfigDefinitionContext configDefinitionContext -> {
            if (ctx.config.put(configDefinitionContext.name, configDefinitionContext.value) != null)
            {
              syntaxError("duplicate config name '" + configDefinitionContext.name + "' for parameter '" +
                  ((ParameterPartContext)ctx.parent).parameterName().name + '\'')
                  .with(configDefinitionContext)
                  .report();
            }
          }
          case MapEntryContext mapEntryContext -> {
            for(var key: mapEntryContext.keys)
              if (ctx.map.put(key, mapEntryContext.value) != null)
              {
                syntaxError("duplicate map entry key " + key + " for parameter '" +
                    ((ParameterPartContext)ctx.parent).parameterName().name + '\'')
                    .with(mapEntryContext)
                    .report();
              }
          }
          case MapEntryDefaultContext mapEntryDefaultContext -> {
            if (_mapEntryDefaultContext != null)
            {
              syntaxError("default map entry can occur only once")
                  .with(mapEntryDefaultContext)
                  .report();
            }

            ctx.map.put(null, new TypedValueMessage(mapEntryDefaultContext.messageWithSpaces));
            _mapEntryDefaultContext = mapEntryDefaultContext;
          }
          default -> {}
        }

      if (_mapEntryDefaultContext != null && ctx.map.size() == 1)
      {
        syntaxError("default map entry can only be used in combination with other map entries")
            .with(_mapEntryDefaultContext)
            .report();
      }
    }


    /**
     * Validates and stores the format name assigned to a parameter.
     *
     * @param ctx  parsed parameter-format context
     */
    @Override
    public void exitParameterFormat(ParameterFormatContext ctx)
    {
      if (!isKebabCaseName(ctx.format = ctx.nameOrKeyword().name))
        syntaxError("parameter format " + KEBAB_CASE_MATCH).with(ctx).report();
    }


    /** Collects template default parameter values keyed by parameter name. */
    final ContextToMapCollector<TemplateParameterDefaultContext,String,TypedValue<?>>
        TEMPLATE_PARAMETER_DEFAULT_COLLECTOR =
        new ContextToMapCollector<>(TreeMap::new, (map, context) -> {
          if (map.put(context.parameter, context.value) != null)
          {
            syntaxError("duplicate template default parameter '" + context.parameter + "'")
                .with(context)
                .report();
          }
        });


    /** Collects template parameter delegations keyed by template parameter name. */
    final ContextToMapCollector<TemplateParameterDelegateContext,String,String> TEMPLATE_PARAMETER_DELEGATE_COLLECTOR =
        new ContextToMapCollector<>(HashMap::new, (map, context) -> {
          if (!context.parameter.equals(context.delegatedParameter) &&
              map.put(context.parameter, context.delegatedParameter) != null)
          {
            syntaxError("duplicate template parameter delegate '" + context.parameter + "'")
                .with(context)
                .report();
          }
        });


    /**
     * Builds a template part with its default values and delegated parameter mappings.
     *
     * @param ctx  parsed template-part context
     */
    @Override
    public void exitTemplatePart(TemplatePartContext ctx)
    {
      ctx.part = new TemplatePart(
          ctx.templateName().name,
          isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          ctx.templateParameterDefault().stream().collect(TEMPLATE_PARAMETER_DEFAULT_COLLECTOR),
          ctx.templateParameterDelegate().stream().collect(TEMPLATE_PARAMETER_DELEGATE_COLLECTOR));
    }


    /**
     * Validates and stores a template name.
     *
     * @param ctx  parsed template-name context
     */
    @Override
    public void exitTemplateName(TemplateNameContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.nameOrKeyword().name))
        syntaxError("template name " + KEBAB_CASE_MATCH).with(ctx).report();
    }


    /**
     * Validates a template parameter delegation and stores both parameter names.
     *
     * @param ctx  parsed template-parameter delegate context
     */
    @Override
    public void exitTemplateParameterDelegate(TemplateParameterDelegateContext ctx)
    {
      final var parameterNames = ctx.nameOrKeyword();
      final var leftParameter = parameterNames.get(0);

      if (!isKebabOrLowerCamelCaseName(ctx.parameter = leftParameter.name))
      {
        syntaxError("parameter delegate: template parameter name " + KEBAB_LOWER_CAMEL_CASE_MATCH)
            .with(leftParameter)
            .report();
      }

      final var rightParameter = parameterNames.get(1);

      if (!isKebabOrLowerCamelCaseName(ctx.delegatedParameter = rightParameter.name))
      {
        syntaxError("parameter delegate: target parameter name " + KEBAB_LOWER_CAMEL_CASE_MATCH)
            .with(rightParameter)
            .report();
      }
    }


    /**
     * Builds a default boolean value for a template parameter.
     *
     * @param ctx  parsed template-parameter default context
     */
    @Override
    public void exitTemplateParameterDefaultBool(TemplateParameterDefaultBoolContext ctx)
    {
      if (!isKebabOrLowerCamelCaseName(ctx.parameter = ctx.nameOrKeyword().name))
      {
        syntaxError("parameter name for default boolean value " + KEBAB_LOWER_CAMEL_CASE_MATCH)
            .with(ctx.nameOrKeyword())
            .report();
      }

      ctx.value = parseBoolean(ctx.BOOL().getText()) ? TypedValueBool.TRUE : TypedValueBool.FALSE;
    }


    /**
     * Builds a default numeric value for a template parameter.
     *
     * @param ctx  parsed template-parameter default context
     */
    @Override
    public void exitTemplateParameterDefaultNumber(TemplateParameterDefaultNumberContext ctx)
    {
      if (!isKebabOrLowerCamelCaseName(ctx.parameter = ctx.nameOrKeyword().name))
      {
        syntaxError("parameter name for default numerical value " + KEBAB_LOWER_CAMEL_CASE_MATCH)
            .with(ctx.nameOrKeyword())
            .report();
      }

      ctx.value = new TypedValueNumber(parseLongValue(ctx.NUMBER()));
    }


    /**
     * Builds a default string value for a template parameter.
     *
     * @param ctx  parsed template-parameter default context
     */
    @Override
    public void exitTemplateParameterDefaultString(TemplateParameterDefaultStringContext ctx)
    {
      if (!isKebabOrLowerCamelCaseName(ctx.parameter = ctx.nameOrKeyword().name))
      {
        syntaxError("parameter name for default string value " + KEBAB_LOWER_CAMEL_CASE_MATCH)
            .with(ctx.nameOrKeyword())
            .report();
      }

      ctx.value = new TypedValueString(messageFactory, ctx.quotedString().string);
    }


    /** Collects post-formatter configuration entries keyed by configuration name. */
    final ContextToMapCollector<ConfigDefinitionContext,String,TypedValue<?>> POST_FORMAT_CONFIG_DEFINITION_COLLECTOR =
        new ContextToMapCollector<>(TreeMap::new, (map, context) -> {
          if (map.put(context.name, context.value) != null)
          {
            syntaxError("duplicate config name " + context.name + " for post formatter '" +
                ((PostFormatPartContext)context.parent).postFormatName().name + '\'')
                .with(context)
                .report();
          }
        });


    /**
     * Builds a post-formatter part for a quoted message and its configuration.
     *
     * @param ctx  parsed post-format part context
     */
    @Override
    public void exitPostFormatPart(PostFormatPartContext ctx)
    {
      final var messageConfigValue = ctx
          .configDefinition()
          .stream()
          .filter(cdc -> cdc.value instanceof TypedValue.MessageValue)
          .findFirst();

      messageConfigValue.ifPresent(cdc ->
          syntaxError("post-format config '" + cdc.name + "' cannot be a message value")
              .with(cdc.getChild(2))
              .report());

      ctx.part = new PostFormatterPart(
          ctx.postFormatName().name,
          ctx.quotedMessage().messageWithSpaces,
          isSpaceAtTokenIndex(ctx.getStart().getTokenIndex() - 1),
          isSpaceAtTokenIndex(ctx.getStop().getTokenIndex() + 1),
          new MessagePartConfig(ctx.configDefinition().stream().collect(POST_FORMAT_CONFIG_DEFINITION_COLLECTOR)));
    }


    /**
     * Validates and stores a post-formatter name.
     *
     * @param ctx  parsed post-format name context
     */
    @Override
    public void exitPostFormatName(PostFormatNameContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.nameOrKeyword().name))
        syntaxError("post-format name " + KEBAB_CASE_MATCH).with(ctx).report();
    }


    /**
     * Builds a map entry whose value is a quoted message.
     *
     * @param ctx  parsed map-entry context
     */
    @Override
    public void exitMapEntryMessage(MapEntryMessageContext ctx)
    {
      ctx.keys = ctx.mapKeys().keys;
      ctx.value = new TypedValueMessage(ctx.quotedMessage().messageWithSpaces);
    }


    /**
     * Builds a map entry whose value comes from a simple inline message string.
     *
     * @param ctx  parsed map-entry context
     */
    @Override
    @SuppressWarnings("LanguageMismatch")
    public void exitMapEntryString(MapEntryStringContext ctx)
    {
      ctx.keys = ctx.mapKeys().keys;
      ctx.value = new TypedValueMessage(messageFactory.parseMessage(ctx.simpleString().string));
    }


    /**
     * Builds the default message used when no explicit map key matches.
     *
     * @param ctx  parsed default map-entry context
     */
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


    /**
     * Builds a boolean configuration entry.
     *
     * @param ctx  parsed configuration-definition context
     */
    @Override
    public void exitConfigDefinitionBool(ConfigDefinitionBoolContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.NAME().getText()))
        syntaxError("config name for boolean value " + KEBAB_CASE_MATCH).with(ctx.NAME()).report();

      ctx.value = parseBoolean(ctx.BOOL().getText()) ? TypedValueBool.TRUE : TypedValueBool.FALSE;
    }


    /**
     * Builds a numeric configuration entry.
     *
     * @param ctx  parsed configuration-definition context
     */
    @Override
    public void exitConfigDefinitionNumber(ConfigDefinitionNumberContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.NAME().getText()))
        syntaxError("config name for numerical value " + KEBAB_CASE_MATCH).with(ctx.NAME()).report();

      ctx.value = new TypedValueNumber(parseLongValue(ctx.NUMBER()));
    }


    /**
     * Builds a message-valued configuration entry.
     *
     * @param ctx  parsed configuration-definition context
     */
    @Override
    public void exitConfigDefinitionMessage(ConfigDefinitionMessageContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.NAME().getText()))
        syntaxError("config name for message value " + KEBAB_CASE_MATCH).with(ctx.NAME()).report();

      ctx.value = new TypedValueMessage(ctx.quotedMessage().messageWithSpaces);
    }


    /**
     * Builds a string configuration entry.
     *
     * @param ctx  parsed configuration-definition context
     */
    @Override
    public void exitConfigDefinitionString(ConfigDefinitionStringContext ctx)
    {
      if (!isKebabCaseName(ctx.name = ctx.NAME().getText()))
        syntaxError("config name for string value " + KEBAB_CASE_MATCH).with(ctx.NAME()).report();

      ctx.value = new TypedValueString(messageFactory, ctx.simpleString().string);
    }


    /**
     * Collects the keys defined for a map entry.
     *
     * @param ctx  parsed map-keys context
     */
    @Override
    public void exitMapKeys(MapKeysContext ctx)
    {
      ctx.keys = ctx
          .mapKey()
          .stream()
          .map(configMapKeyContext -> configMapKeyContext.key)
          .toList();
    }


    /**
     * Builds a map key that matches {@code null} values.
     *
     * @param ctx  parsed null-key context
     */
    @Override
    public void exitMapKeyNull(MapKeyNullContext ctx)
    {
      final var equalOperator = ctx.equalOperator();

      ctx.key = equalOperator == null || equalOperator.cmp == CompareType.EQ
          ? MapKeyNull.EQ
          : MapKeyNull.NE;
    }


    /**
     * Builds a map key that matches empty values.
     *
     * @param ctx  parsed empty-key context
     */
    @Override
    public void exitMapKeyEmpty(MapKeyEmptyContext ctx)
    {
      final var equalOperator = ctx.equalOperator();

      ctx.key = equalOperator == null || equalOperator.cmp == CompareType.EQ
          ? MapKeyEmpty.EQ
          : MapKeyEmpty.NE;
    }


    /**
     * Builds a map key that matches a boolean value.
     *
     * @param ctx  parsed boolean-key context
     */
    @Override
    public void exitMapKeyBool(MapKeyBoolContext ctx) {
      ctx.key = parseBoolean(ctx.BOOL().getText()) ? MapKeyBool.TRUE : MapKeyBool.FALSE;
    }


    /**
     * Builds a map key that matches a numeric value.
     *
     * @param ctx  parsed numeric-key context
     */
    @Override
    public void exitMapKeyNumber(MapKeyNumberContext ctx)
    {
      final var relationalOperator = ctx.relationalOperator();

      ctx.key = new MapKeyNumber(
          relationalOperator == null ? CompareType.EQ : relationalOperator.cmp,
          parseLongValue(ctx.NUMBER()));
    }


    /**
     * Builds a map key that matches a string value.
     *
     * @param ctx  parsed string-key context
     */
    @Override
    public void exitMapKeyString(MapKeyStringContext ctx)
    {
      final var relationalOperator = ctx.relationalOperator();

      ctx.key = new MapKeyString(
          relationalOperator == null ? CompareType.EQ : relationalOperator.cmp,
          ctx.quotedString().string);
    }


    /**
     * Resolves a relational operator to the comparison type used by map keys.
     *
     * @param ctx  parsed relational-operator context
     */
    @Override
    public void exitRelationalOperator(RelationalOperatorContext ctx)
    {
      var equalOperator = ctx.equalOperator();
      if (equalOperator != null)
        ctx.cmp = equalOperator.cmp;
      else
        switch(getTerminalToken(ctx, 0).getType())
        {
          case LTE -> ctx.cmp = CompareType.LTE;
          case LT -> ctx.cmp = CompareType.LT;
          case GT -> ctx.cmp = CompareType.GT;
          case GTE -> ctx.cmp = CompareType.GTE;
        }
    }


    /**
     * Resolves an equality operator to the matching comparison type.
     *
     * @param ctx  parsed equality-operator context
     */
    @Override
    public void exitEqualOperator(EqualOperatorContext ctx) {
      ctx.cmp = ctx.EQ() != null ? CompareType.EQ : CompareType.NE;
    }


    /**
     * Stores the textual value of a name-or-keyword token.
     *
     * @param ctx  parsed name-or-keyword context
     */
    @Override
    public void exitNameOrKeyword(NameOrKeywordContext ctx) {
      ctx.name = ctx.getChild(0).getText();
    }


    /**
     * Checks whether the token at the given index begins with a space character.
     *
     * @param i  token index to inspect
     *
     * @return  {@code true} if the indexed token starts with whitespace
     */
    @Contract(pure = true)
    private boolean isSpaceAtTokenIndex(int i)
    {
      if (i >= 0)
      {
        var token = tokenStream.get(i);
        if (token.getType() != EOF)
        {
          final var text = token.getText();
          return !isEmpty(text) && isSpaceChar(text.charAt(0));
        }
      }

      return false;
    }


    /**
     * Parses a numeric literal as a {@code long}.
     *
     * @param numberNode  parse-tree node containing the numeric literal
     *
     * @return  parsed long value
     *
     * @throws MessageParserException  if the literal is outside the supported {@code long} range
     */
    private long parseLongValue(@NotNull TerminalNode numberNode)
    {
      try {
        return new BigInteger(numberNode.getText()).longValueExact();
      } catch(ArithmeticException ex) {
        syntaxError("number value out of range").with(numberNode).report();
        return 0;  // never reached
      }
    }
  }
}
