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
import de.sayayi.lib.message.parser.MessageCompiler;
import de.sayayi.lib.message.parser.normalizer.MessagePartNormalizer;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import static de.sayayi.lib.message.parser.normalizer.MessagePartNormalizer.PASS_THROUGH;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;


/**
 * Factory class for creating message instances.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
@SuppressWarnings("UnknownLanguage")
public class MessageFactory
{
  /**
   * Shared message factory without any caching capabilities.
   * <p>
   * This message factory will suffice in most cases. However, if you have a large amount of
   * messages, it is better to construct a message factory with an appropriate
   * {@link MessagePartNormalizer}.
   */
  public static final MessageFactory NO_CACHE_INSTANCE = new MessageFactory(PASS_THROUGH);

  private static final Random RANDOM = new Random();
  private static int CODE_ID = 0;

  private final @NotNull MessagePartNormalizer messagePartNormalizer;
  final MessageCompiler messageCompiler;


  /**
   * Construct a new message factory with the given {@code messagePartNormalizer}.
   *
   * @param messagePartNormalizer  message part normalizer instance, never {@code null}
   */
  public MessageFactory(@NotNull MessagePartNormalizer messagePartNormalizer)
  {
    this.messagePartNormalizer = messagePartNormalizer;
    messageCompiler = new MessageCompiler(this);
  }


  /**
   * Returns the message part normalizer instance.
   *
   * @return  message part normalizer, never {@code null}
   */
  public @NotNull MessagePartNormalizer getMessagePartNormalizer() {
    return messagePartNormalizer;
  }


  /**
   * Parse a message format text into a message instance.
   *
   * @param text  message format text, not {@code null}
   *
   * @return  message instance, never {@code null}
   *
   * @throws MessageParserException  in case the message could not be parsed
   */
  @Contract(value = "_ -> new", pure = true)
  public @NotNull Message.WithSpaces parseMessage(@NotNull @Language("MessageFormat") String text) {
    return messageCompiler.compileMessage(text);
  }


  @Contract(value = "_ -> new", pure = true)
  public @NotNull Message parseMessage(@NotNull Map<Locale,String> localizedTexts) {
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
  public @NotNull Message.WithCode parseMessage(@NotNull String code,
                                                @NotNull @Language("MessageFormat") String text)
  {
    try {
      return withCode(code, parseMessage(text));
    } catch(MessageParserException ex) {
      throw ex.withCode(code);
    }
  }


  @Contract(value = "_, _ -> new", pure = true)
  public @NotNull Message.WithCode parseMessage(@NotNull String code,
                                                @NotNull Map<Locale,String> localizedTexts)
  {
    switch(requireNonNull(localizedTexts, "localizedTexts must not be null").size())
    {
      case 0:
        return new EmptyMessageWithCode(code);

      case 1:
        return parseMessage(code, localizedTexts.values().iterator().next());

      default: {
        final Map<Locale,Message> localizedMessages = new HashMap<>();

        localizedTexts.forEach((Locale locale, @Language("MessageFormat") String text) -> {
          try {
            localizedMessages.put(locale, parseMessage(text));
          } catch(MessageParserException ex) {
            throw ex.withCode(code).withLocale(locale);
          }
        });

        return new LocalizedMessageBundleWithCode(code, localizedMessages);
      }
    }
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
  public @NotNull Message parseTemplate(@NotNull @Language("MessageFormat") String text) {
    return messageCompiler.compileTemplate(text);
  }


  @Contract(pure = true)
  public @NotNull Message parseTemplate(@NotNull Map<Locale,String> localizedTexts)
  {
    switch(requireNonNull(localizedTexts, "localizedTexts must not be null").size())
    {
      case 0:
        return EmptyMessage.INSTANCE;

      case 1: {
        final Entry<Locale,String> entry = localizedTexts.entrySet().iterator().next();

        try {
          return parseTemplate(entry.getValue());
        } catch(MessageParserException ex) {
          throw ex.withLocale(entry.getKey());
        }
      }

      default: {
        final Map<Locale,Message> localizedMessages = new HashMap<>();

        localizedTexts.forEach((Locale locale, @Language("MessageFormat") String text) -> {
          try {
            localizedMessages.put(locale, parseTemplate(text));
          } catch(MessageParserException ex) {
            throw ex.withLocale(locale);
          }
        });

        return new LocalizedMessageBundleWithCode(generateCode("TPL"), localizedMessages);
      }
    }
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
      final Message.WithCode messageWithCode = (Message.WithCode)message;

      if (code.equals(messageWithCode.getCode()))
        return messageWithCode;
    }

    if (message instanceof MessageDelegateWithCode)
      message = ((MessageDelegateWithCode)message).getMessage();
    else if (message instanceof LocalizedMessageBundleWithCode)
    {
      return new LocalizedMessageBundleWithCode(code,
          ((LocalizedMessageBundleWithCode)message).getLocalizedMessages());
    }
    else if (message instanceof EmptyMessage || message instanceof EmptyMessageWithCode)
      return new EmptyMessageWithCode(code);

    return new MessageDelegateWithCode(code, message);
  }


  @Contract(pure = true)
  protected @NotNull String generateCode(@NotNull String prefix)
  {
    final byte[] hashBytes = new byte[6];
    RANDOM.nextBytes(hashBytes);

    final String hash = Long.toString(0x1000000000000L |
        ((hashBytes[0] & 0xffL) << 40) |
        ((hashBytes[1] & 0xffL) << 32) |
        ((hashBytes[2] & 0xffL) << 24) |
        ((hashBytes[3] & 0xffL) << 16) |
        ((hashBytes[4] & 0xffL) << 8) |
        (hashBytes[5] & 0xffL), 36);

    return (prefix + '[' + hash + '-' + Integer.toString(++CODE_ID | 0x20000, 36) + ']')
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
}
