/*
 * Copyright 2023 Jeroen Gremmen
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
 * Exception providing detailed information about what went wrong during message/template
 * formatting.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class MessageFormatException extends MessageException
{
  /** Code of the message being formatted as the exception occurs or {@code null}. */
  private final String code;

  /** Name of the template being formatted as the exception occurs or {@code null}. */
  private final String template;

  /** Locale in effect during message/template formatting as the exception occurs or {@code null}. */
  private final Locale locale;

  /** Name of the parameter being formatted as the exception occurs or {@code null}. */
  private final String parameter;


  /**
   * Constructs a new message exception with the specified cause.
   * <p>
   * Note that the detail message associated with {@code cause} is <i>not</i>
   * incorporated in this runtime exception's detail message.
   *
   * @param cause    the cause (which is saved for later retrieval by the {@link #getCause()}
   *                 method). (A {@code null} value is permitted, and indicates that the cause is
   *                 nonexistent or unknown.)
   */
  protected MessageFormatException(Throwable cause) {
    this(null, null, null, null, cause);
  }


  /**
   * All arguments constructor for internal use.
   *
   * @param code       message code
   * @param template   template name
   * @param locale     message locale
   * @param parameter  message parameter name
   * @param cause      cause
   */
  protected MessageFormatException(String code, String template, Locale locale, String parameter, Throwable cause)
  {
    super(cause);

    this.code = code;
    this.template = template;
    this.locale = locale;
    this.parameter = parameter;
  }


  @Override
  public String getMessage()
  {
    /*
      c | t | l | p | msg
      0 | 0 | 0 | 0 | failed to format message
      0 | 0 | 0 | 1 | failed to format message parameter 'p'
      0 | 0 | 1 | 0 | failed to format message for locale 'l'
      0 | 0 | 1 | 1 | failed to format message parameter 'p' for locale 'l'
      0 | 1 | 0 | 0 | failed to format template 't'
      0 | 1 | 0 | 1 | failed to format parameter 'p' in template 't'
      0 | 1 | 1 | 0 | failed to format template 't' for locale 'l'
      0 | 1 | 1 | 1 | failed to format parameter 'p' in template 't' for locale 'l'
      1 | 0 | 0 | 0 | failed to format message with code 'c'
      1 | 0 | 0 | 1 | failed to format parameter 'p' for message with code 'c'
      1 | 0 | 1 | 0 | failed to format message with code 'c' and locale 'l'
      1 | 0 | 1 | 1 | failed to format parameter 'p' for message with code 'c' and locale 'l'
      1 | 1 | 0 | 0 | failed to format template 't' for message with code 'c'
      1 | 1 | 0 | 1 | failed to format parameter 'p' in template 't' for message with code 'c'
      1 | 1 | 1 | 0 | failed to format template 't' for message with code 'c' and locale 'l'
      1 | 1 | 1 | 1 | failed to format parameter 'p' in template 't' for message with code 'c' and locale 'l'
     */

    var msg = new StringBuilder("failed to format");
    var n =
        (code != null && !isGeneratedCode(code) ? 0b1000 : 0b0000) +
        (template != null && !isGeneratedCode(template) ? 0b0100 : 0b0000) +
        (locale != null ? 0b0010 : 0b0000) +
        (parameter != null ? 0b0001 : 0b0000);

    if ((n & 0b1100) == 0)
      msg.append(" message");

    if ((n & 0b0001) != 0)  // parameter
      msg.append(" parameter '").append(parameter).append('\'');

    if ((n & 0b0100) != 0)  // template
    {
      if ((n & 0b0001) != 0)
        msg.append(" in");

      msg.append(" template '").append(template).append('\'');
    }

    if ((n & 0b1000) != 0)  // code
    {
      if (n != 0b1010 && n != 0b1000)
        msg.append(" for");

      msg.append(" message with code '").append(code).append('\'');
    }

    if ((n & 0b0010) != 0)  // locale
    {
      msg.append((n & 0b1000) != 0 ? " and" : " for").append(" locale ")
         .append(ROOT.equals(locale) ? "ROOT" : locale.getDisplayName(UK));
    }

    return msg.toString();
  }


  /**
   * Returns the message code, if available.
   *
   * @return  message code, or {@code null}
   */
  @Contract(pure = true)
  public String getCode() {
    return code;
  }


  /**
   * Returns the template name, if available.
   *
   * @return  template name, or {@code null}
   */
  @Contract(pure = true)
  public String getTemplate() {
    return template;
  }


  /**
   * Returns the message locale, if available.
   *
   * @return  message locale, or {@code null}
   */
  @Contract(pure = true)
  public Locale getLocale() {
    return locale;
  }


  /**
   * Returns the message parameter name, if available.
   *
   * @return  message parameter name, or {@code null}
   */
  @Contract(pure = true)
  public String getParameter() {
    return parameter;
  }


  /**
   * Returns a copy of this exception where the code value has been replaced with
   * the given {@code code}.
   *
   * @param code  new code, not {@code null}
   *
   * @return  new exception based on the current exception with modified code value,
   *          never {@code null}
   */
  @Contract("_ -> new")
  public @NotNull MessageFormatException withCode(@NotNull String code) {
    return new MessageFormatException(code, template, locale, parameter, getCause());
  }


  /**
   * Returns a copy of this exception where the template value has been replaced with
   * the given {@code template}.
   *
   * @param template  new template, not {@code null}
   *
   * @return  new exception based on the current exception with modified template value,
   *          never {@code null}
   */
  @Contract("_ -> new")
  public @NotNull MessageFormatException withTemplate(@NotNull String template) {
    return new MessageFormatException(code, template, locale, parameter, getCause());
  }


  /**
   * Returns a copy of this exception where the locale value has been replaced with
   * the given {@code locale}.
   *
   * @param locale  new locale, not {@code null}
   *
   * @return  new exception based on the current exception with modified locale value,
   *          never {@code null}
   */
  @Contract("_ -> new")
  public @NotNull MessageFormatException withLocale(@NotNull Locale locale) {
    return new MessageFormatException(code, template, locale, parameter, getCause());
  }


  /**
   * Returns a copy of this exception where the parameter value has been replaced with
   * the given {@code parameter}.
   *
   * @param parameter  new parameter, not {@code null}
   *
   * @return  new exception based on the current exception with modified parameter value,
   *          never {@code null}
   */
  @Contract("_ -> new")
  public @NotNull MessageFormatException withParameter(@NotNull String parameter) {
    return new MessageFormatException(code, template, locale, parameter, getCause());
  }


  /**
   * Cast to or create a new MessageFormatException instance based of {@code ex}.
   *
   * @param ex  exception, or {@code null}
   *
   * @return  {@code MessageFormatException} instance, never {@code null}
   */
  @Contract(pure = true)
  public static @NotNull MessageFormatException of(Exception ex)
  {
    return ex instanceof MessageFormatException
        ? (MessageFormatException)ex
        : new MessageFormatException(ex);
  }
}
