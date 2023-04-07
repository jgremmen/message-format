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

import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import de.sayayi.lib.message.internal.LocalizedMessageBundleWithCode;
import de.sayayi.lib.message.internal.MessageDelegateWithCode;
import de.sayayi.lib.message.internal.part.MessagePart;
import de.sayayi.lib.message.parser.MessageCompiler;
import de.sayayi.lib.message.parser.normalizer.MessagePartNormalizer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;


/**
 * Factory for creating message instances.
 *
 * @author Jeroen Gremmen
 */
public class MessageFactory
{
  /**
   * Shared message factory without any caching capabilities.
   * <p>
   * This message factory will suffice in most cases. However, if you have a large amount of messages,
   * it is better to construct a message factory with an appropriate {@link MessagePartNormalizer}.
   */
  public static final MessageFactory NO_CACHE_INSTANCE = new MessageFactory(new MessagePartNormalizer() {
    @Override public <T extends MessagePart> @NotNull T normalize(@NotNull T part) { return part; }
  });

  private static final AtomicInteger CODE_ID = new AtomicInteger(0);

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
   */
  @Contract(value = "_ -> new", pure = true)
  public @NotNull Message.WithSpaces parseMessage(@NotNull String text) {
    return messageCompiler.compileMessage(text);
  }


  @Contract(value = "_ -> new", pure = true)
  public @NotNull Message parseMessage(@NotNull Map<Locale,String> localizedTexts) {
    return parseMessage("Message::" + CODE_ID.incrementAndGet(), localizedTexts);
  }


  /**
   * Parse the message {@code text} into a {@link Message} instance.
   *
   * @param code  message code
   * @param text  message format
   *
   * @return  message instance
   */
  @Contract(value = "_, _ -> new", pure = true)
  public @NotNull Message.WithCode parseMessage(@NotNull String code, @NotNull String text) {
    return new MessageDelegateWithCode(code, parseMessage(text));
  }


  @Contract(value = "_, _ -> new", pure = true)
  public @NotNull Message.WithCode parseMessage(@NotNull String code,
                                                @NotNull Map<Locale,String> localizedTexts)
  {
    if (requireNonNull(localizedTexts, "localizedTexts must not be null").isEmpty())
      return new EmptyMessageWithCode(code);

    final String message = localizedTexts.get(ROOT);
    if (message != null && localizedTexts.size() == 1)
      return new MessageDelegateWithCode(code, parseMessage(message));

    final Map<Locale,Message> localizedMessages = new LinkedHashMap<>();

    localizedTexts.forEach((locale,text) -> localizedMessages.put(locale, parseMessage(text)));

    return new LocalizedMessageBundleWithCode(code, localizedMessages);
  }


  /**
   * Parse a template format text into a message instance.
   *
   * @param text  template format text, not {@code null}
   *
   * @return  message instance, never {@code null}
   */
  @Contract(value = "_ -> new", pure = true)
  public @NotNull Message parseTemplate(@NotNull String text) {
    return messageCompiler.compileTemplate(text);
  }


  @Contract(pure = true)
  public @NotNull Message parseTemplate(@NotNull Map<Locale,String> localizedTexts)
  {
    if (requireNonNull(localizedTexts, "localizedTexts must not be null").isEmpty())
      return EmptyMessage.INSTANCE;

    final String message = localizedTexts.get(ROOT);
    if (message != null && localizedTexts.size() == 1)
      return parseTemplate(message);

    final Map<Locale,Message> localizedMessages = new LinkedHashMap<>();

    localizedTexts.forEach((locale,text) -> localizedMessages.put(locale, parseMessage(text)));

    return new LocalizedMessageBundleWithCode("Template::" + CODE_ID.incrementAndGet(), localizedMessages);
  }


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
      return new MessageDelegateWithCode(code, ((MessageDelegateWithCode)message).getMessage());

    if (message instanceof EmptyMessage || message instanceof EmptyMessageWithCode)
      return new EmptyMessageWithCode(code);

    return new MessageDelegateWithCode(code, message);
  }
}
