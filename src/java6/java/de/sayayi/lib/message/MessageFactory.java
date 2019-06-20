/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message;

import de.sayayi.lib.message.annotation.Messages;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.exception.MessageLocaleParseException;
import de.sayayi.lib.message.impl.EmptyMessageWithCode;
import de.sayayi.lib.message.impl.MessageDelegateWithCode;
import de.sayayi.lib.message.impl.MultipartLocalizedMessageBundleWithCode;
import de.sayayi.lib.message.parser.MessageParser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Jeroen Gremmen
 */
public final class MessageFactory
{
  private static final AtomicInteger CODE_ID = new AtomicInteger(0);


  @Contract("_ -> new")
  @NotNull
  public static Message parse(@NotNull String text) {
    return new MessageParser(text).parseMessage();
  }


  @Contract("_ -> new")
  @NotNull
  public static Message parse(@NotNull Map<Locale,String> localizedTexts) {
    return parse("Generated::" + CODE_ID.incrementAndGet(), localizedTexts);
  }


  /**
   * Parse the message {@code text} into a {@link Message} instance.
   *
   * @param code  message code
   * @param text  message format
   *
   * @return  message instance
   */
  @NotNull
  public static MessageWithCode parse(@NotNull String code, @NotNull String text) {
    return new MessageDelegateWithCode(code, new MessageParser(text).parseMessage());
  }


  @NotNull
  public static MessageWithCode parse(@NotNull String code, @NotNull Map<Locale,String> localizedTexts)
  {
    if (localizedTexts.isEmpty())
      return new EmptyMessageWithCode(code);

    final Map<Locale,Message> localizedParts = new LinkedHashMap<Locale,Message>();

    for(final Entry<Locale,String> localizedText: localizedTexts.entrySet())
      localizedParts.put(localizedText.getKey(), new MessageParser(localizedText.getValue()).parseMessage());

    return new MultipartLocalizedMessageBundleWithCode(code, localizedParts);
  }


  @SuppressWarnings("WeakerAccess")
  @NotNull
  public static Set<MessageWithCode> parseAnnotations(@NotNull AnnotatedElement element)
  {
    Set<MessageWithCode> messageBundle = new HashSet<MessageWithCode>();

    de.sayayi.lib.message.annotation.Message annotation =
        element.getAnnotation(de.sayayi.lib.message.annotation.Message.class);
    if (annotation != null)
      messageBundle.add(parse(annotation));

    Messages messagesAnnotation = element.getAnnotation(Messages.class);
    if (messagesAnnotation != null)
      for(de.sayayi.lib.message.annotation.Message message: messagesAnnotation.messages())
      {
        MessageWithCode mwc = parse(message);

        if (!messageBundle.add(mwc))
          throw new IllegalArgumentException("duplicate message code " + mwc.getCode() + " found");

        messageBundle.add(mwc);
      }

    return messageBundle;
  }


  @NotNull
  public static MessageWithCode parse(@NotNull de.sayayi.lib.message.annotation.Message annotation)
  {
    final Text[] texts = annotation.texts();
    if (texts.length == 0)
      return new EmptyMessageWithCode(annotation.code());

    final Map<Locale,String> localizedTexts = new LinkedHashMap<Locale,String>();

    for(final Text text: texts)
    {
      final Locale locale = forLanguageTag(text.locale());
      if (!localizedTexts.containsKey(locale))
        localizedTexts.put(locale, text.text());
    }

    return parse(annotation.code(), localizedTexts);
  }


  @NotNull
  static Locale forLanguageTag(@NotNull String locale)
  {
    if (locale.isEmpty())
      return Locale.ROOT;

    final int length = locale.length();
    if (length < 2)
      throw new MessageLocaleParseException("missing language code for locale " + locale);

    if (!Character.isLowerCase(locale.charAt(0)) || !Character.isLowerCase(locale.charAt(1)))
        throw new MessageLocaleParseException("invalid language code for locale " + locale);

    if (length == 2)
      return new Locale(locale);

    if (length != 5)
      throw new MessageLocaleParseException("unexpected length " + length + " for locale " + locale);

    if (locale.charAt(2) != '-' && locale.charAt(2) != '_')
      throw new MessageLocaleParseException("missing separator '-' between language and country code for locale " + locale);

    if (!Character.isUpperCase(locale.charAt(3)) || !Character.isUpperCase(locale.charAt(4)))
      throw new MessageLocaleParseException("invalid country code for locale " + locale);

    return new Locale(locale.substring(0,  2), locale.substring(3, 5));
  }
}
