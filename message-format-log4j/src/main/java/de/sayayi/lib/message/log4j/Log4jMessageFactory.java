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
import org.apache.logging.log4j.message.AbstractMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

import static de.sayayi.lib.message.part.normalizer.MessagePartNormalizer.PASS_THROUGH;
import static java.util.Objects.requireNonNull;


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
  /**
   * Precomputed parameter names {@code p1}, {@code p2}, etc. for the most common parameter counts.
   *
   * @since 0.26.0
   */
  private static final String[] PARAMETER_NAMES = new String[16];


  static
  {
    for(var n = 0; n < PARAMETER_NAMES.length; n++)
      PARAMETER_NAMES[n] = "p" + (n + 1);
  }


  /**
   * Message support used to parse and format the log messages. For the default message support this holds {@code null}
   * until the first message is created.
   */
  private final AtomicReference<MessageSupport> messageSupport;

  /** {@code true} if messages with Log4j-style placeholders only are delegated to {@link ParameterizedMessage}. */
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
   * <p>
   * The default message support is created when the first message is created, not by this constructor. So creating a
   * logger backed by this factory does not access the shared formatter service. This matters if a logger is created
   * while the shared formatter service is being initialized, e.g. in the static initializer of a formatter discovered
   * with the {@link java.util.ServiceLoader} mechanism.
   *
   * @param parameterizedMessageFallback  if {@code true}, messages containing Log4j-style <code>{}</code> placeholders
   *                                      (and no message-format placeholders) are delegated to
   *                                      {@link ParameterizedMessage} for backward compatibility
   */
  public Log4jMessageFactory(boolean parameterizedMessageFallback)
  {
    this.messageSupport = new AtomicReference<>();
    this.parameterizedMessageFallback = parameterizedMessageFallback;
  }


  /**
   * Creates a new factory backed by the given {@link MessageSupport} instance.
   *
   * @param messageSupport                the message support to use for formatting, not {@code null}
   * @param parameterizedMessageFallback  if {@code true}, messages containing Log4j-style <code>{}</code> placeholders
   *                                      (and no message-format placeholders) are delegated to
   *                                      {@link ParameterizedMessage} for backward compatibility
   */
  public Log4jMessageFactory(@NotNull MessageSupport messageSupport, boolean parameterizedMessageFallback)
  {
    this.messageSupport = new AtomicReference<>(requireNonNull(messageSupport, "messageSupport must not be null"));
    this.parameterizedMessageFallback = parameterizedMessageFallback;
  }


  /**
   * {@inheritDoc}
   * <p>
   * The character sequence is converted into a string and handled like {@link #newMessage(String)}.
   */
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
   * for backward compatibility. Otherwise, the message is parsed using message-format syntax and parameters are mapped
   * to variables {@code p1}, {@code p2}, etc.
   * <p>
   * If the last parameter is a {@link Throwable}, it is additionally attached to the returned message. The formatted
   * message string is evaluated lazily when first requested and then cached. Runtime exceptions occurring while
   * formatting are not thrown; instead, they are reported as a warning through Log4j's
   * {@link StatusLogger StatusLogger} and a placeholder text containing the message format string is returned.
   * {@link Error Errors} are propagated to the caller.
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

    final var builder = getMessageSupport().message(message);
    Throwable throwable = null;
    int length;

    if (parameters != null && (length = parameters.length) > 0)
    {
      for(var n = 0; n < length; n++)
        builder.with(n < PARAMETER_NAMES.length ? PARAMETER_NAMES[n] : "p" + (n + 1), parameters[n]);

      // check if the last parameter is a throwable
      if (parameters[length - 1] instanceof Throwable throwableParam)
        throwable = throwableParam;
    }

    // the builder is not used otherwise, so it can format the message lazily; Log4jMessage caches the result
    return new Log4jMessage(
        message,
        () -> {
          try {
            return builder.format();
          } catch(RuntimeException ex) {
            // don't throw exceptions if the message is not valid; report the cause via the status logger
            StatusLogger.getLogger().warn("failed to format message: {}", message, ex);
            return "<internal error formatting: " + message + '>';
          }
        },
        throwable);
  }


  /**
   * Returns the message support used to parse and format the log messages. The default message support is created on
   * the first call.
   * <p>
   * The default message support is created without holding a lock, as creating it may create the shared formatter
   * service. Concurrent first calls may each create a message support, but all of them return the one published first,
   * so all messages share its message cache.
   *
   * @return  message support, never {@code null}
   *
   * @since 0.26.0
   */
  @NotNull MessageSupport getMessageSupport()
  {
    var support = messageSupport.get();
    if (support == null)
    {
      final var created = MessageSupportFactory.create(
          DefaultFormatterService.getSharedInstance(),
          new MessageFactory(PASS_THROUGH, 256));

      if ((support = messageSupport.compareAndExchange(null, created)) == null)
        support = created;
    }

    return support;
  }
}
