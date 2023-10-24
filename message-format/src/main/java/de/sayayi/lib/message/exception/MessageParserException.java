/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.exception;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;


/**
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public class MessageParserException extends MessageException
{
  /** Error message describing the error that occurred durcing parsing. */
  private final String errorMessage;

  /** A visual representation of the syntax error and where it occurred. */
  private final String syntaxError;

  /** Code of the message being formatted as the exception occurs or {@code null}. */
  private final String code;

  /** Name of the template being formatted as the exception occurs or {@code null}. */
  private final String template;

  /** Locale in effect during message/template formatting as the exception occurs or {@code null}. */
  private final Locale locale;


  public MessageParserException(@NotNull String errorMessage, @NotNull String syntaxError,
                                Exception cause) {
    this(errorMessage, syntaxError, null, null, null, cause);
  }


  /**
   * @since 0.9.1
   */
  public MessageParserException(@NotNull String errorMessage, @NotNull String syntaxError,
                                String code, String template, Locale locale, Exception cause)
  {
    super(errorMessage + '\n' + syntaxError, cause);

    this.errorMessage = errorMessage;
    this.syntaxError = syntaxError;
    this.code = code;
    this.template = template;
    this.locale = locale;
  }


  /**
   * Returns the error message describing what went wrong during parsing.
   *
   * @return  error message, never {@code null}
   *
   * @since 0.8.4
   */
  @Contract(pure = true)
  public @NotNull String getErrorMessage() {
    return errorMessage;
  }


  /**
   * Returns a visual representation of the location where the syntax error occurred during
   * parsing.
   *
   * @return  syntax error, never {@code null}
   *
   * @since 0.8.4
   */
  @Contract(pure = true)
  public @NotNull String getSyntaxError() {
    return syntaxError;
  }


  /**
   * Returns the message code, if available.
   *
   * @return  message code, or {@code null}
   *
   * @since 0.9.1
   */
  @Contract(pure = true)
  public String getCode() {
    return code;
  }


  /**
   * Returns the template name, if available.
   *
   * @return  template name, or {@code null}
   *
   * @since 0.9.1
   */
  @Contract(pure = true)
  public String getTemplate() {
    return template;
  }


  /**
   * Returns the message locale, if available.
   *
   * @return  message locale, or {@code null}
   *
   * @since 0.9.1
   */
  @Contract(pure = true)
  public Locale getLocale() {
    return locale;
  }


  /**
   * Returns a copy of this exception where the code value has been replaced with
   * the given {@code code}.
   *
   * @param code  new code, not {@code null}
   *
   * @return  new exception based on the current exception with modified code value,
   *          never {@code null}
   *
   * @since 0.9.1
   */
  @Contract("_ -> new")
  public @NotNull MessageParserException withCode(@NotNull String code)
  {
    return new MessageParserException(errorMessage, syntaxError, code, null, locale,
        (Exception)getCause());
  }


  /**
   * Returns a copy of this exception where the template value has been replaced with
   * the given {@code template}.
   *
   * @param template  new template, not {@code null}
   *
   * @return  new exception based on the current exception with modified template value,
   *          never {@code null}
   *
   * @since 0.9.1
   */
  @Contract("_ -> new")
  public @NotNull MessageParserException withTemplate(@NotNull String template)
  {
    return new MessageParserException(errorMessage, syntaxError, null, template, locale,
        (Exception)getCause());
  }


  /**
   * Returns a copy of this exception where the locale value has been replaced with
   * the given {@code locale}.
   *
   * @param locale  new locale, not {@code null}
   *
   * @return  new exception based on the current exception with modified locale value,
   *          never {@code null}
   *
   * @since 0.9.1
   */
  @Contract("_ -> new")
  public @NotNull MessageParserException withLocale(@NotNull Locale locale)
  {
    return new MessageParserException(errorMessage, syntaxError, code, template, locale,
        (Exception)getCause());
  }
}
