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
package de.sayayi.lib.message.log4j;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.util.SupplierDelegate;
import org.apache.logging.log4j.message.AbstractMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.normalizer.MessagePartNormalizer.PASS_THROUGH;


/**
 * A Log4j {@link org.apache.logging.log4j.message.MessageFactory MessageFactory} implementation that uses the
 * message-format library for formatting log messages.
 * <p>
 * Message strings are parsed using the message-format syntax. Parameters passed to
 * {@link #newMessage(String, Object...) newMessage} are made available in the message template as {@code p1},
 * {@code p2}, etc. If the last parameter is a {@link Throwable}, it is additionally propagated as the message's
 * throwable.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 *
 * @see Log4jMessage
 */
public final class Log4jMessageFactory extends AbstractMessageFactory
{
  private final MessageSupport messageSupport;
  private final boolean parameterizedMessageFallback;


  /**
   * Creates a new factory with a default {@link MessageSupport} instance using the shared
   * {@link DefaultFormatterService} and parameterized message fallback enabled.
   */
  public Log4jMessageFactory() {
    this(true);
  }


  /**
   * Creates a new factory with a default {@link MessageSupport} instance using the shared
   * {@link DefaultFormatterService}.
   *
   * @param parameterizedMessageFallback  if {@code true}, messages containing Log4j-style <code>{}</code>
   *                                      placeholders (and no message-format placeholders) are delegated to
   *                                      {@link ParameterizedMessage} for backward compatibility
   */
  public Log4jMessageFactory(boolean parameterizedMessageFallback)
  {
    this(MessageSupportFactory
            .create(
                DefaultFormatterService.getSharedInstance(),
                new MessageFactory(PASS_THROUGH, 256)),
        parameterizedMessageFallback);
  }


  /**
   * Creates a new factory backed by the given {@link MessageSupport} instance.
   *
   * @param messageSupport                the message support to use for formatting, not {@code null}
   * @param parameterizedMessageFallback  if {@code true}, messages containing Log4j-style <code>{}</code>
   *                                      placeholders (and no message-format placeholders) are delegated to
   *                                      {@link ParameterizedMessage} for backward compatibility
   */
  public Log4jMessageFactory(@NotNull MessageSupport messageSupport, boolean parameterizedMessageFallback)
  {
    this.messageSupport = messageSupport;
    this.parameterizedMessageFallback = parameterizedMessageFallback;
  }


  @Override
  public Message newMessage(CharSequence charSequence) {
    return newMessage(charSequence == null ? null : charSequence.toString(), (Object[])null);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Message newMessage(@Language("MessageFormat") String message) {
    return newMessage(message, (Object[])null);
  }


  /**
   * {@inheritDoc}
   * <p>
   * If the message string contains Log4j-style <code>{}</code> placeholders but no message-format placeholders
   * (<code>%{</code>, <code>%[</code> or <code>%(</code>) and the parameterized message fallback is enabled,
   * formatting is delegated to a {@link ParameterizedMessage}
   * for backward compatibility. Otherwise, the message is parsed using message-format syntax and parameters are
   * mapped to variables {@code p1}, {@code p2}, etc.
   * <p>
   * If the last parameter is a {@link Throwable}, it is additionally attached to the returned message.
   * The formatted message string is evaluated lazily when first requested.
   * <p>
   * A {@code null} message string results in a {@link SimpleMessage} with {@code null} content.
   */
  @Override
  public Message newMessage(@Language("MessageFormat") String message, Object... parameters)
  {
    // can this actually happen?
    if (message == null)
      return new SimpleMessage(null);

    // if there are no message format placeholders but there is a log4j parameterized placeholder,
    // use ParameterizedMessage, if enabled.
    if (parameterizedMessageFallback &&
        message.contains("{}") &&
        !(message.contains("\\{}") || message.contains("%{") || message.contains("%[") || message.contains("%(")))
      return new ParameterizedMessage(message, parameters);

    final var builder = messageSupport.message(message);
    Throwable throwable = null;
    int length;

    if (parameters != null && (length = parameters.length) > 0)
    {
      for(var n = 0; n < length; n++)
        builder.with("p" + (n + 1), parameters[n]);

      // check if the last parameter is a throwable
      if (parameters[length - 1] instanceof Throwable throwableParam)
        throwable = throwableParam;
    }

    final var formatSupplier = builder.formatSupplier();

    return new Log4jMessage(
        SupplierDelegate.of(() -> {
          try {
            return formatSupplier.get();
          } catch(Throwable ex) {
            // don't throw exceptions if the message is not valid!!
            return "<internal error formatting: " + message + '>';
          }
        }),
        throwable);
  }
}
