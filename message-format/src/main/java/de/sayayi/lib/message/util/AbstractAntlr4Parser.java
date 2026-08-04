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

import de.sayayi.lib.antlr4.syntax.SyntaxErrorBuilder;
import de.sayayi.lib.antlr4.syntax.SyntaxErrorFormatter;
import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupport.MessageConfigurer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.SyntaxTree;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static java.util.Objects.requireNonNull;


/**
 * Extension of the ANTLR4 base parser that integrates with the message format library for syntax error reporting.
 * <p>
 * This class provides convenience methods to create syntax error messages using the {@link MessageSupport} framework,
 * allowing parsers to produce localized and parameterized error messages.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 *
 * @see SyntaxErrorMessageBuilder
 */
public abstract class AbstractAntlr4Parser extends de.sayayi.lib.antlr4.AbstractAntlr4Parser
{
  /** Message support instance used for formatting syntax error messages. */
  protected final MessageSupport messageSupport;


  /**
   * Creates a new parser with the given message support and default error formatting.
   *
   * @param messageSupport  message support instance for error message formatting, not {@code null}
   */
  protected AbstractAntlr4Parser(@NotNull MessageSupport messageSupport) {
    this.messageSupport = requireNonNull(messageSupport);
  }


  /**
   * Creates a new parser with the given syntax error formatter and message support.
   *
   * @param syntaxErrorFormatter  formatter for syntax error presentation, not {@code null}
   * @param messageSupport        message support instance for error message formatting, not {@code null}
   */
  protected AbstractAntlr4Parser(@NotNull SyntaxErrorFormatter syntaxErrorFormatter,
                                 @NotNull MessageSupport messageSupport)
  {
    super(syntaxErrorFormatter);

    this.messageSupport = requireNonNull(messageSupport);
  }


  /**
   * Creates a new parser with the given syntax error formatter, console error listener setting and message support.
   *
   * @param syntaxErrorFormatter       formatter for syntax error presentation, not {@code null}
   * @param keepConsoleErrorListeners  whether to keep the default ANTLR console error listeners
   * @param messageSupport             message support instance for error message formatting, not {@code null}
   */
  protected AbstractAntlr4Parser(@NotNull SyntaxErrorFormatter syntaxErrorFormatter,
                                 boolean keepConsoleErrorListeners,
                                 @NotNull MessageSupport messageSupport)
  {
    super(syntaxErrorFormatter, keepConsoleErrorListeners);

    this.messageSupport = requireNonNull(messageSupport);
  }


  /**
   * Creates a syntax error message builder using a message code from the message support.
   *
   * @param code  message code to look up, not {@code null}
   *
   * @return a new syntax error message builder, never {@code null}
   */
  @Contract(value = "_ -> new", pure = true)
  protected @NotNull SyntaxErrorMessageBuilder syntaxErrorCode(@NotNull String code) {
    return new SyntaxErrorMessageBuilderImpl(messageSupport.code(code));
  }


  /**
   * Creates a syntax error message builder using an inline message format string.
   *
   * @param message  message format string to parse, not {@code null}
   *
   * @return a new syntax error message builder, never {@code null}
   */
  @Contract(value = "_ -> new", pure = true)
  protected @NotNull SyntaxErrorMessageBuilder syntaxErrorMessage(@NotNull @Language("MessageFormat") String message) {
    return new SyntaxErrorMessageBuilderImpl(messageSupport.message(message));
  }




  /**
   * A builder that combines the message format parameterization capabilities of {@link MessageConfigurer} with the
   * syntax error location tracking of {@link SyntaxErrorBuilder}.
   * <p>
   * Parameters and locale are configured first, then the token position is specified which transitions the builder into
   * a {@link SyntaxErrorBuilder} that can report the error.
   *
   * @since 0.24.0
   */
  protected sealed interface SyntaxErrorMessageBuilder permits SyntaxErrorMessageBuilderImpl
  {
    /**
     * Sets a boolean parameter value for the error message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(String, boolean)
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, boolean value);


    /**
     * Sets a byte parameter value for the error message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(String, byte)
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, byte value);


    /**
     * Sets a char parameter value for the error message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(String, char)
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, char value);


    /**
     * Sets a short parameter value for the error message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(String, short)
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, short value);


    /**
     * Sets an int parameter value for the error message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(String, int)
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, int value);


    /**
     * Sets a long parameter value for the error message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(String, long)
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, long value);


    /**
     * Sets a float parameter value for the error message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(String, float)
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, float value);


    /**
     * Sets a double parameter value for the error message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(String, double)
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, double value);


    /**
     * Sets an object parameter value for the error message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(String, Object)
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, Object value);


    /**
     * Sets multiple parameter values for the error message from a map.
     *
     * @param parameterValues  map of parameter names to values, not {@code null}
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(Map)
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull Map<String,?> parameterValues);


    /**
     * Sets multiple parameter values for the error message from properties.
     *
     * @param properties  properties containing parameter names and values, not {@code null}
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#with(Properties)
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder with(@NotNull Properties properties);


    /**
     * Sets the locale for formatting the error message.
     *
     * @param locale  locale to use, or {@code null} for the default locale
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#locale(Locale)
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder locale(Locale locale);


    /**
     * Sets the locale for formatting the error message using a language tag.
     *
     * @param locale  locale language tag, or {@code null} for the default locale
     *
     * @return this builder instance, never {@code null}
     *
     * @see MessageConfigurer#locale(String)
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull SyntaxErrorMessageBuilder locale(String locale);


    /**
     * Formats the error message and creates a syntax error builder with start position at the given token.
     *
     * @param token  token marking the start of the error location, not {@code null}
     *
     * @return a syntax error builder for further position and reporting configuration, never {@code null}
     *
     * @see SyntaxErrorBuilder#withStart(Token)
     */
    @Contract(value = "_ -> new", pure = true)
    @NotNull SyntaxErrorBuilder withStart(@NotNull Token token);


    /**
     * Formats the error message and creates a syntax error builder with start position at the given syntax tree node.
     *
     * @param syntaxTree  syntax tree node marking the start of the error location, not {@code null}
     *
     * @return a syntax error builder for further position and reporting configuration, never {@code null}
     *
     * @see SyntaxErrorBuilder#withStart(SyntaxTree)
     */
    @Contract(value = "_ -> new", pure = true)
    @NotNull SyntaxErrorBuilder withStart(@NotNull SyntaxTree syntaxTree);


    /**
     * Formats the error message and creates a syntax error builder with stop position at the given token.
     *
     * @param token  token marking the end of the error location, not {@code null}
     *
     * @return a syntax error builder for further position and reporting configuration, never {@code null}
     *
     * @see SyntaxErrorBuilder#withStop(Token)
     */
    @Contract(value = "_ -> new", pure = true)
    @NotNull SyntaxErrorBuilder withStop(@NotNull Token token);


    /**
     * Formats the error message and creates a syntax error builder with stop position at the given syntax tree node.
     *
     * @param syntaxTree  syntax tree node marking the end of the error location, not {@code null}
     *
     * @return a syntax error builder for further position and reporting configuration, never {@code null}
     *
     * @see SyntaxErrorBuilder#withStop(SyntaxTree)
     */
    @Contract(value = "_ -> new", pure = true)
    @NotNull SyntaxErrorBuilder withStop(@NotNull SyntaxTree syntaxTree);


    /**
     * Formats the error message and creates a syntax error builder spanning the entire given token.
     *
     * @param token  token where the syntax error occurred, not {@code null}
     *
     * @return a syntax error builder for further position and reporting configuration, never {@code null}
     *
     * @see SyntaxErrorBuilder#with(Token)
     *
     * @since 0.24.1
     */
    @Contract(value = "_ -> new", pure = true)
    @NotNull SyntaxErrorBuilder with(@NotNull Token token);


    /**
     * Formats the error message and creates a syntax error builder spanning the entire given syntax tree node.
     *
     * @param syntaxTree  syntax tree node defining the full error location, not {@code null}
     *
     * @return a syntax error builder for further position and reporting configuration, never {@code null}
     *
     * @see SyntaxErrorBuilder#with(SyntaxTree)
     */
    @Contract(value = "_ -> new", pure = true)
    @NotNull SyntaxErrorBuilder with(@NotNull SyntaxTree syntaxTree);


    /**
     * Formats the error message and creates a syntax error builder with the given cause exception.
     *
     * @param cause  exception that caused the syntax error, or {@code null}
     *
     * @return a syntax error builder for further position and reporting configuration, never {@code null}
     *
     * @see SyntaxErrorBuilder#withCause(Exception)
     */
    @Contract(value = "_ -> new", pure = true)
    @NotNull SyntaxErrorBuilder withCause(Exception cause);
  }




  /**
   * Implementation of {@link SyntaxErrorMessageBuilder} that delegates parameter configuration to a
   * {@link MessageConfigurer} and creates a {@link SyntaxErrorBuilder} when token position methods are invoked.
   */
  private final class SyntaxErrorMessageBuilderImpl implements SyntaxErrorMessageBuilder
  {
    private final MessageConfigurer<? extends Message> messageConfigurer;


    /**
     * Creates a new builder backed by the given message configurer.
     *
     * @param messageConfigurer  message configurer providing parameter and locale support, not {@code null}
     */
    private SyntaxErrorMessageBuilderImpl(@NotNull MessageConfigurer<? extends Message> messageConfigurer) {
      this.messageConfigurer = messageConfigurer;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, boolean value)
    {
      messageConfigurer.with(parameter, value);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, byte value)
    {
      messageConfigurer.with(parameter, value);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, char value)
    {
      messageConfigurer.with(parameter, value);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, short value)
    {
      messageConfigurer.with(parameter, value);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, int value)
    {
      messageConfigurer.with(parameter, value);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, long value)
    {
      messageConfigurer.with(parameter, value);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, float value)
    {
      messageConfigurer.with(parameter, value);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, double value)
    {
      messageConfigurer.with(parameter, value);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull String parameter, Object value)
    {
      messageConfigurer.with(parameter, value);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull Map<String,?> parameterValues)
    {
      messageConfigurer.with(parameterValues);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder with(@NotNull Properties properties)
    {
      messageConfigurer.with(properties);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder locale(Locale locale)
    {
      messageConfigurer.locale(locale);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorMessageBuilder locale(String locale)
    {
      messageConfigurer.locale(locale);
      return this;
    }


    @Override
    public @NotNull SyntaxErrorBuilder withStart(@NotNull Token token) {
      return createSyntaxErrorBuilder().withStart(token);
    }


    @Override
    public @NotNull SyntaxErrorBuilder withStart(@NotNull SyntaxTree syntaxTree) {
      return createSyntaxErrorBuilder().withStart(syntaxTree);
    }


    @Override
    public @NotNull SyntaxErrorBuilder withStop(@NotNull Token token) {
      return createSyntaxErrorBuilder().withStop(token);
    }


    @Override
    public @NotNull SyntaxErrorBuilder withStop(@NotNull SyntaxTree syntaxTree) {
      return createSyntaxErrorBuilder().withStop(syntaxTree);
    }


    @Override
    public @NotNull SyntaxErrorBuilder with(@NotNull Token token) {
      return createSyntaxErrorBuilder().with(token);
    }


    @Override
    public @NotNull SyntaxErrorBuilder with(@NotNull SyntaxTree syntaxTree) {
      return createSyntaxErrorBuilder().with(syntaxTree);
    }


    @Override
    public @NotNull SyntaxErrorBuilder withCause(Exception cause) {
      return createSyntaxErrorBuilder().withCause(cause);
    }


    /**
     * Formats the configured message and creates a new syntax error builder for position specification.
     *
     * @return a new syntax error builder, never {@code null}
     */
    @Contract(value = "-> new", pure = true)
    private @NotNull SyntaxErrorBuilder createSyntaxErrorBuilder() {
      return AbstractAntlr4Parser.this.syntaxError(messageConfigurer.format());
    }
  }
}
