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
 * @author Jeroen Gremmen
 */
public class MessageFormatException extends MessageException
{
  private final String code;
  private final String template;
  private final Locale locale;
  private final String parameter;


  public MessageFormatException(Throwable cause) {
    this(null, null, null, null, cause);
  }


  protected MessageFormatException(String code, String template, Locale locale, String parameter,
                                   Throwable cause)
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
    final StringBuilder msg = new StringBuilder();

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

    final int n = (code != null && !isGeneratedCode(code) ? 8 : 0) +
        (template != null ? 4 : 0) + (locale != null ? 2 : 0) + (parameter != null ? 1 : 0);

    msg.append("failed to format");

    if (n < 0b0100)
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
      msg.append((n & 0b1000) != 0 ? " and" : " for").append(" locale ");
      msg.append(ROOT.equals(locale) ? "ROOT" : locale.getDisplayName(UK));
    }

    return msg.toString();
  }


  @Contract("_ -> new")
  public @NotNull MessageFormatException withCode(@NotNull String code) {
    return new MessageFormatException(code, template, locale, parameter, getCause());
  }


  @Contract("_ -> new")
  public MessageFormatException withTemplate(@NotNull String template) {
    return new MessageFormatException(code, template, locale, parameter, getCause());
  }


  @Contract("_ -> new")
  public MessageFormatException withLocale(@NotNull Locale locale) {
    return new MessageFormatException(code, template, locale, parameter, getCause());
  }


  @Contract("_ -> new")
  public MessageFormatException withParameter(@NotNull String parameter) {
    return new MessageFormatException(code, template, locale, parameter, getCause());
  }
}
