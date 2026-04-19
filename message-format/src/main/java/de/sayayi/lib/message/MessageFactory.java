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
package de.sayayi.lib.message;

import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import de.sayayi.lib.message.internal.LocalizedMessageBundleWithCode;
import de.sayayi.lib.message.internal.MessageDelegateWithCode;
import de.sayayi.lib.message.internal.parser.MessageCompiler;
import de.sayayi.lib.message.part.normalizer.MessagePartNormalizer;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static de.sayayi.lib.message.part.normalizer.MessagePartNormalizer.PASS_THROUGH;
import static java.lang.Math.clamp;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;


/**
 * Factory class for creating {@link Message} instances by parsing message format strings and localized message maps.
 * <p>
 * The factory supports parsing both messages and templates, each in two flavours:
 * <ul>
 *   <li>A single format string (e.g. {@link #parseMessage(String)}, {@link #parseTemplate(String)}).</li>
 *   <li>
 *     A locale-keyed map of format strings for locale-aware messages (e.g. {@link #parseMessage(Map)},
 *     {@link #parseTemplate(Map)}).
 *   </li>
 * </ul>
 * An explicit message code can be provided via {@link #parseMessage(String, String)} and
 * {@link #parseMessage(String, Map)}.
 * <p>
 * Each factory instance is configured with a {@link MessagePartNormalizer} that is applied to parsed message parts,
 * enabling deduplication or caching strategies for large message sets. For simple use cases the
 * {@link #NO_CACHE_INSTANCE shared no-cache instance} is sufficient.
 * <p>
 * Optionally, a message cache can be enabled via {@link #MessageFactory(MessagePartNormalizer, int)} to avoid
 * repeated parsing of the same message format string. When the cache reaches its configured maximum size, the least
 * recently used entry is evicted. See {@link #parseMessage(String)} for details.
 * <p>
 * This class is <strong>thread-safe</strong>. All public methods can be called concurrently from multiple threads.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public class MessageFactory
{
  /**
   * Shared message factory without any caching capabilities.
   * <p>
   * This message factory will suffice in most cases. However, if you have a large number of messages, it is better
   * to construct a message factory with an appropriate {@link MessagePartNormalizer}.
   */
  public static final MessageFactory NO_CACHE_INSTANCE = new MessageFactory(PASS_THROUGH);

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final AtomicInteger CODE_ID = new AtomicInteger();

  private final @NotNull MessagePartNormalizer messagePartNormalizer;
  final MessageCompiler messageCompiler;

  private final @Nullable Map<String,Message.WithSpaces> messageCache;
  private final @Nullable Lock cacheLock;


  /**
   * Construct a new message factory with the given {@code messagePartNormalizer} and no message cache.
   *
   * @param messagePartNormalizer  message part normalizer instance, never {@code null}
   */
  public MessageFactory(@NotNull MessagePartNormalizer messagePartNormalizer) {
    this(messagePartNormalizer, 0);
  }


  /**
   * Construct a new message factory with the given {@code messagePartNormalizer} and a message cache of the given
   * {@code messageCacheSize}.
   * <p>
   * When the cache is full, the least recently used message is evicted before adding a new one.
   * A {@code messageCacheSize} of {@code 0} or less disables caching.
   *
   * @param messagePartNormalizer  message part normalizer instance, never {@code null}
   * @param messageCacheSize       maximum number of cached messages, or {@code 0} to disable caching
   *
   * @since 0.21.0
   */
  public MessageFactory(@NotNull MessagePartNormalizer messagePartNormalizer, int messageCacheSize)
  {
    this.messagePartNormalizer = messagePartNormalizer;
    messageCompiler = new MessageCompiler(this);

    if (messageCacheSize > 0)
    {
      messageCache = new LinkedHashMap<>(clamp(messageCacheSize, 16, 64), 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String,Message.WithSpaces> eldest) {
          return size() > messageCacheSize;
        }
      };
      cacheLock = new ReentrantLock();
    }
    else
    {
      messageCache = null;
      cacheLock = null;
    }
  }


  /**
   * Returns the message part normalizer instance.
   *
   * @return  message part normalizer, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull MessagePartNormalizer getMessagePartNormalizer() {
    return messagePartNormalizer;
  }


  /**
   * Creates a new {@link MessageBuilder} bound to this factory instance.
   * <p>
   * The returned builder is <strong>not thread-safe</strong> and must not be reused after calling
   * {@link MessageBuilder#build()} or {@link MessageBuilder#buildWithCode(String)}.
   *
   * @return  new message builder, never {@code null}
   *
   * @since 0.21.0
   */
  @Contract("-> new")
  public @NotNull MessageBuilder messageBuilder() {
    return MessageBuilder.create(this);
  }


  /**
   * Parse a message format text into a message instance.
   * <p>
   * If this factory was created with a message cache (see {@link #MessageFactory(MessagePartNormalizer, int)}),
   * previously parsed messages are returned from the cache. When the cache is full, the least recently used entry
   * is evicted.
   *
   * @param text  message format text, not {@code null}
   *
   * @return  message instance, never {@code null}
   *
   * @throws MessageParserException  in case the message could not be parsed
   */
  @Contract(pure = true)
  public @NotNull Message.WithSpaces parseMessage(@NotNull @Language("MessageFormat") String text)
  {
    if (cacheLock == null)
      return messageCompiler.compileMessage(text);

    cacheLock.lock();
    try {
      //noinspection DataFlowIssue
      return messageCache.computeIfAbsent(text, messageCompiler::compileMessage);
    }
    finally {
      cacheLock.unlock();
    }
  }


  /**
   * Parse localized message format texts into a {@link Message} instance with an automatically generated message code.
   *
   * @param localizedTexts  a map containing message formats, keyed by locale, not {@code null}
   *
   * @return  message instance with a generated code, never {@code null}
   *
   * @throws MessageParserException  in case one of the messages could not be parsed
   *
   * @see #parseMessage(String, Map)
   */
  @Contract(value = "_ -> new", pure = true)
  public @NotNull Message.WithCode parseMessage(@NotNull Map<Locale,String> localizedTexts) {
    return parseMessage(generateCode("MSG"), localizedTexts);
  }


  /**
   * Parse the message {@code text} into a {@link Message} instance.
   *
   * @param code  message code, not {@code null}
   * @param text  message format, not {@code null}
   *
   * @return  message instance, never {@code null}
   *
   * @throws MessageParserException  in case the message could not be parsed
   */
  @Contract(value = "_, _ -> new", pure = true)
  public @NotNull Message.WithCode parseMessage(@NotNull String code, @NotNull @Language("MessageFormat") String text)
  {
    try {
      return withCode(code, parseMessage(text));
    } catch(MessageParserException ex) {
      throw ex.withCode(code);
    }
  }


  /**
   * Parse the localized messages {@code localizedTexts} into a {@link Message} instance.
   *
   * @param code            message code, not {@code null}
   * @param localizedTexts  a map containing message formats, keyed by locale, not {@code null}
   *
   * @return  message with code instance, never {@code null}
   *
   * @throws MessageParserException  in case one of the messages could not be parsed
   */
  @Contract(value = "_, _ -> new", pure = true)
  public @NotNull Message.WithCode parseMessage(@NotNull String code, @NotNull Map<Locale,String> localizedTexts)
  {
    return switch(requireNonNull(localizedTexts, "localizedTexts must not be null").size()) {
      case 0 -> new EmptyMessageWithCode(code);

      case 1 -> {
        var entry = localizedTexts.entrySet().iterator().next();

        try {
          yield parseMessage(code, entry.getValue());
        } catch(MessageParserException ex) {
          throw ex.withLocale(entry.getKey());
        }
      }

      default -> {
        final var localizedMessages = new HashMap<Locale,Message>();

        localizedTexts.forEach((Locale locale, @Language("MessageFormat") String text) -> {
          try {
            localizedMessages.put(locale, parseMessage(text));
          } catch(MessageParserException ex) {
            throw ex.withCode(code).withLocale(locale);
          }
        });

        yield new LocalizedMessageBundleWithCode(code, localizedMessages);
      }
    };
  }


  /**
   * Parse a template format text into a message instance.
   *
   * @param text  template format text, not {@code null}
   *
   * @return  message instance, never {@code null}
   *
   * @throws MessageParserException  in case the template could not be parsed
   */
  @Contract(value = "_ -> new", pure = true)
  public @NotNull Message.WithSpaces parseTemplate(@NotNull @Language("MessageFormat") String text) {
    return messageCompiler.compileTemplate(text);
  }


  /**
   * Parse the localized template messages {@code localizedTexts} into a {@link Message} instance.
   *
   * @param localizedTexts  a map containing template message formats, keyed by locale, not {@code null}
   *
   * @return  template message instance, never {@code null}
   *
   * @throws MessageParserException  in case one of the template messages could not be parsed
   */
  @Contract(pure = true)
  public @NotNull Message parseTemplate(@NotNull Map<Locale,String> localizedTexts)
  {
    return switch(requireNonNull(localizedTexts, "localizedTexts must not be null").size())
    {
      case 0 -> EmptyMessage.INSTANCE;

      case 1 -> {
        final var entry = localizedTexts.entrySet().iterator().next();

        try {
          yield parseTemplate(entry.getValue());
        } catch(MessageParserException ex) {
          throw ex.withLocale(entry.getKey());
        }
      }

      default -> {
        final var localizedMessages = new HashMap<Locale,Message>();

        localizedTexts.forEach((Locale locale, @Language("MessageFormat") String text) -> {
          try {
            localizedMessages.put(locale, parseTemplate(text));
          } catch(MessageParserException ex) {
            throw ex.withLocale(locale);
          }
        });

        yield new LocalizedMessageBundleWithCode(generateCode("TPL"), localizedMessages);
      }
    };
  }


  /**
   * Modifies {@code message} so that it has the given {@code code}.
   *
   * @param code     (new) message code, not {@code null} and not empty
   * @param message  message, not {@code null}
   *
   * @return  message with code, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Message.WithCode withCode(@NotNull String code, @NotNull Message message)
  {
    if (requireNonNull(message, "message must not be null") instanceof Message.WithCode)
    {
      final var messageWithCode = (Message.WithCode)message;

      if (code.equals(messageWithCode.getCode()))
        return messageWithCode;
    }

    // unwrap message
    while(message instanceof MessageDelegateWithCode)
      message = ((MessageDelegateWithCode)message).getMessage();

    if (message instanceof Message.LocaleAware)
      return new LocalizedMessageBundleWithCode(code, ((Message.LocaleAware)message).getLocalizedMessages());
    else if (message instanceof EmptyMessage || message instanceof EmptyMessageWithCode)
      return new EmptyMessageWithCode(code);

    return new MessageDelegateWithCode(code, message);
  }


  /**
   * Generates a unique message or template code by combining the given {@code prefix} with a
   * random base-36 hash and a monotonically increasing counter. The resulting code is
   * upper-cased and has the form <code>PREFIX[HASH-COUNTER]</code> (e.g.
   * {@code MSG[1A2B3C4D5E-XXXXX]}).
   *
   * @param prefix  code prefix (e.g. {@code "MSG"} or {@code "TPL"}), not {@code null}
   *
   * @return  generated unique code, never {@code null}
   *
   * @see #isGeneratedCode(String)
   */
  @Contract(pure = true)
  protected @NotNull String generateCode(@NotNull String prefix)
  {
    final var hashBytes = new byte[6];
    RANDOM.nextBytes(hashBytes);

    final var hash = Long.toString(0x1000000000000L |
        ((hashBytes[0] & 0xffL) << 40) |
        ((hashBytes[1] & 0xffL) << 32) |
        ((hashBytes[2] & 0xffL) << 24) |
        ((hashBytes[3] & 0xffL) << 16) |
        ((hashBytes[4] & 0xffL) << 8) |
         (hashBytes[5] & 0xffL), 36);

    return (prefix + '[' + hash + '-' + Integer.toString(CODE_ID.incrementAndGet() | 0x20000, 36) + ']')
        .toUpperCase(ROOT);
  }


  /**
   * Checks whether {@code code} is a generated message or template code.
   *
   * @param code  message or template code
   *
   * @return  {@code true} if the given code is a generated message or template code,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  public static boolean isGeneratedCode(String code) {
    return code != null && code.matches("(MSG|TPL)\\[[0-9A-Z-]+]");
  }


  /**
   * Checks whether messages {@code m1} and {@code m2} are the same. Messages are considered "the same" when the
   * message parts of both messages are identical.
   * <p>
   * {@link Message.LocaleAware LocaleAware} messages are "the same" when both locale and associated message are
   * identical for all locales provided by this message.
   * <p>
   * Identical messages with different codes ({@link Message.WithCode WithCode}) are still considered the same as only
   * the message part is compared.
   *
   * @param m1  message 1 to compare, not {@code null}
   * @param m2  message 2 to compare, not {@code null}
   *
   * @return  {@code true} if both messages are identical, {@code false} otherwise
   *
   * @see Message#isSame(Message)
   *
   * @since 0.20.0
   */
  @Contract(pure = true)
  public static boolean isSame(@NotNull Message m1, @NotNull Message m2)
  {
    // unwrap m1
    while(m1 instanceof MessageDelegateWithCode)
      m1 = ((MessageDelegateWithCode)m1).getMessage();

    // unwrap m2
    while(m2 instanceof MessageDelegateWithCode)
      m2 = ((MessageDelegateWithCode)m2).getMessage();

    if (m1 == m2)
      return true;

    final var la1 = m1 instanceof Message.LocaleAware;
    final var la2 = m2 instanceof Message.LocaleAware;

    return la1 && la2
        ? isSame((Message.LocaleAware)m1, (Message.LocaleAware)m2)
        : !la1 && !la2 && Arrays.equals(m1.getMessageParts(), m2.getMessageParts());
  }


  @Contract(pure = true)
  private static boolean isSame(@NotNull Message.LocaleAware m1, @NotNull Message.LocaleAware m2)
  {
    if (m1 != m2)
    {
      final var lm1 = m1.getLocalizedMessages();
      final var lm2 = m2.getLocalizedMessages();

      if (!lm1.keySet().equals(lm2.keySet()))
        return false;

      for(var entry: lm1.entrySet())
        if (!entry.getValue().isSame(lm2.get(entry.getKey())))
          return false;
    }

    return true;
  }
}
