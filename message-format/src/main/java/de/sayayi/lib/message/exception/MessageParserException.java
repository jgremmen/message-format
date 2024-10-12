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

import static de.sayayi.lib.message.MessageFactory.isGeneratedCode;
import static java.util.Locale.ROOT;
import static java.util.Locale.UK;


/**
 * A message parser exception provides detailed information about what went wrong during message or
 * template parsing and provides a visual representation showing exactly where the parsing error
 * occurred.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public class MessageParserException extends MessageException
{
  /** Exception type. */
  private final Type type;

  /** Error message describing the error that occurred durcing parsing. */
  private final String errorMessage;

  /** A visual representation of the syntax error and where it occurred. */
  private final String syntaxError;

  /** Code of the message being parsed as the exception occurs or {@code null}. */
  private final String code;

  /** Name of the template being parsed as the exception occurs or {@code null}. */
  private final String template;

  /** Locale in effect during message/template parsing as the exception occurs or {@code null}. */
  private final Locale locale;


  /**
   * Create a new message parser exception.
   *
   * @param errorMessage  error message describing what went wrong during parsing, not {@code null}
   * @param syntaxError   a visual representation showing where the parsing error occurred, not {@code null}
   * @param cause         the exception that caused this exception
   *
   * @since 0.9.1
   */
  public MessageParserException(@NotNull String errorMessage, @NotNull String syntaxError, Exception cause) {
    this(null, errorMessage, syntaxError, null, null, null, cause);
  }


  /**
   * Create a new message parser exception.
   *
   * @param type          parse type
   * @param errorMessage  error message describing what went wrong during parsing, not {@code null}
   * @param syntaxError   a visual representation showing where the parsing error occurred, not {@code null}
   * @param code          message code associated with the message being parsed
   * @param template      template name associated with the template being parsed
   * @param locale        locale associated with the message or template being parsed
   * @param cause         the exception that caused this exception
   *
   * @since 0.9.1
   */
  public MessageParserException(Type type, @NotNull String errorMessage, @NotNull String syntaxError,
                                String code, String template, Locale locale, Exception cause)
  {
    super(cause);

    this.type = type;
    this.errorMessage = errorMessage;
    this.syntaxError = syntaxError;
    this.code = code;
    this.template = template;
    this.locale = locale;
  }


  @Override
  public String getMessage()
  {
    /*
      c | tpl | l | t |  msg
      0 |  0  | 0 | - |
      0 |  0  | 0 | M |  failed to parse message
      0 |  0  | 0 | T |  failed to parse template
      0 |  0  | 1 | - |  failed to parse message/template for locale 'l'
      0 |  0  | 1 | M |  failed to parse message for locale 'l'
      0 |  0  | 1 | T |  failed to parse template for locale 'l'
      0 |  1  | 0 | T |  failed to parse template 't'
      0 |  1  | 1 | T |  failed to parse template 't' for locale 'l'
      1 |  0  | 0 | M |  failed to parse message with code 'c'
      1 |  0  | 1 | M |  failed to parse message with code 'c' for locale 'l'
    */

    final StringBuilder msg = new StringBuilder();
    final int n =
        (code != null && !isGeneratedCode(code) ? 0b100 : 0b000) +
        (template != null && !isGeneratedCode(template) ? 0b010 : 0b000) +
        (locale != null ? 0b001 : 0b000);

    if (n != 0 || type != null)
    {
      msg.append("failed to parse ");

      if (type == Type.MESSAGE)
      {
        msg.append("message");
        if ((n & 0b100) != 0)
          msg.append(" with code '").append(code).append('\'');
      }
      else if (type == Type.TEMPLATE)
      {
        msg.append("template");
        if ((n & 0b010) != 0)
          msg.append(" '").append(template).append('\'');
      }
      else if (n == 1)
        msg.append("message/template");

      if ((n & 0b001) != 0)
        msg.append(" for locale ").append(ROOT.equals(locale) ? "ROOT" : locale.getDisplayName(UK));

      msg.append(": ");
    }

    return msg.append(errorMessage).append('\n').append(syntaxError).toString();
  }


  /**
   * Returns the exception type, if available.
   *
   * @return  exception type, or {@code null}
   *
   * @since 0.9.1
   */
  @Contract(pure = true)
  public Type getType() {
    return type;
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
    return new MessageParserException(Type.MESSAGE, errorMessage, syntaxError, code, null, locale,
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
    return new MessageParserException(Type.TEMPLATE, errorMessage, syntaxError, null, template,
        locale, (Exception)getCause());
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
  public @NotNull MessageParserException withLocale(@NotNull Locale locale) {
    return new MessageParserException(type, errorMessage, syntaxError, code, template, locale, (Exception)getCause());
  }


  /**
   * Returns a copy of this exception where the type has been replaced with the given {@code type}.
   *
   * @param type  type, not {@code null}
   *
   * @return  new exception based on the current exception with modified locale value,
   *          never {@code null}
   *
   * @since 0.9.1
   */
  @Contract("_ -> new")
  public @NotNull MessageParserException withType(@NotNull Type type)
  {
    return new MessageParserException(type, errorMessage, syntaxError,
        type == Type.MESSAGE ? code : null, type == Type.TEMPLATE ? template : null, locale,
        (Exception)getCause());
  }




  public enum Type
  {
    MESSAGE,
    TEMPLATE
  }
}
