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

import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.MessageDefs;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.exception.MessageException;
import de.sayayi.lib.message.exception.MessageLocaleParseException;
import de.sayayi.lib.message.impl.EmptyMessage;
import de.sayayi.lib.message.impl.EmptyMessageWithCode;
import de.sayayi.lib.message.impl.MessageDelegateWithCode;
import de.sayayi.lib.message.impl.MultipartLocalizedMessageBundleWithCode;
import de.sayayi.lib.message.parser.MessageParserSupport;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Locale.ROOT;


/**
 * @author Jeroen Gremmen
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageFactory
{
  private static final AtomicInteger CODE_ID = new AtomicInteger(0);


  @NotNull
  @Contract(value = "_ -> new", pure = true)
  public static Message parse(@NotNull String text) {
    return MessageParserSupport.parse(text);
  }


  @NotNull
  @Contract(value = "_ -> new", pure = true)
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
  @Contract(value = "_, _ -> new", pure = true)
  public static Message.WithCode parse(@NotNull String code, @NotNull String text) {
    return new MessageDelegateWithCode(code, parse(text));
  }


  @NotNull
  @Contract(value = "_, _ -> new", pure = true)
  public static Message.WithCode parse(@NotNull String code, @NotNull Map<Locale,String> localizedTexts)
  {
    if (localizedTexts.isEmpty())
      return new EmptyMessageWithCode(code);

    String message = localizedTexts.get(ROOT);
    if (message != null && localizedTexts.size() == 1)
      return new MessageDelegateWithCode(code, parse(message));

    final Map<Locale,Message> localizedParts = new LinkedHashMap<Locale,Message>();

    for(final Entry<Locale,String> localizedText: localizedTexts.entrySet())
      localizedParts.put(localizedText.getKey(), parse(localizedText.getValue()));

    return new MultipartLocalizedMessageBundleWithCode(code, localizedParts);
  }


  @SuppressWarnings("WeakerAccess")
  @NotNull
  @Contract(value = "_ -> new", pure = true)
  public static Set<Message.WithCode> parseAnnotations(@NotNull AnnotatedElement element)
  {
    Set<Message.WithCode> messageBundle = new HashSet<Message.WithCode>();

    MessageDef annotation = element.getAnnotation(MessageDef.class);
    if (annotation != null)
      messageBundle.add(parse(annotation));

    MessageDefs messageDefsAnnotation = element.getAnnotation(MessageDefs.class);
    if (messageDefsAnnotation != null)
      for(MessageDef messageDef: messageDefsAnnotation.value())
      {
        Message.WithCode mwc = parse(messageDef);

        if (!messageBundle.add(mwc))
          throw new MessageException("duplicate message code " + mwc.getCode() + " found");

        messageBundle.add(mwc);
      }

    return messageBundle;
  }


  @NotNull
  @Contract(value = "_ -> new", pure = true)
  public static Message.WithCode parse(@NotNull MessageDef annotation)
  {
    final Text[] texts = annotation.texts();
    if (texts.length == 0)
      return new EmptyMessageWithCode(annotation.code());

    final Map<Locale,String> localizedTexts = new LinkedHashMap<Locale,String>();

    for(final Text text: texts)
    {
      final Locale locale = forLanguageTag(text.locale());
      final String value = text.locale().isEmpty() && text.text().isEmpty() ? text.value() : text.text();

      if (!localizedTexts.containsKey(locale))
        localizedTexts.put(locale, value);
    }

    return parse(annotation.code(), localizedTexts);
  }


  @NotNull
  @Contract(pure = true)
  public static Message.WithCode withCode(@NotNull String code, @NotNull Message message)
  {
    if (message instanceof MessageDelegateWithCode)
      return new MessageDelegateWithCode(code, ((MessageDelegateWithCode)message).getMessage());

    if (message instanceof Message.WithCode)
    {
      Message.WithCode messageWithCode = (Message.WithCode)message;

      if (code.equals(messageWithCode.getCode()))
        return messageWithCode;
    }

    if (message instanceof EmptyMessage || message instanceof EmptyMessageWithCode)
      return new EmptyMessageWithCode(code);

    return new MessageDelegateWithCode(code, message);
  }


  @NotNull
  @Contract(pure = true)
  public static MessageBundle bundle(@NotNull Properties properties)
  {
    MessageBundle bundle = new MessageBundle();

    for(Entry<Object,Object> entry: properties.entrySet())
      bundle.add(parse(entry.getKey().toString(), String.valueOf(entry.getValue())));

    return bundle;
  }


  @NotNull
  @Contract(pure = true)
  public static MessageBundle bundle(@NotNull ResourceBundle resourceBundle)
  {
    MessageBundle bundle = new MessageBundle();
    Locale locale = resourceBundle.getLocale();

    for(String code: resourceBundle.keySet())
      bundle.add(parse(code, Collections.singletonMap(locale, resourceBundle.getString(code))));

    return bundle;
  }


  @NotNull
  @Contract(pure = true)
  public static MessageBundle bundle(@NotNull Map<Locale,Properties> properties)
  {
    Map<String,Map<Locale,Message>> localizedMessagesByCode = new HashMap<String,Map<Locale,Message>>();

    for(Entry<Locale,Properties> entry: properties.entrySet())
      for(Entry<Object,Object> localizedProperty: entry.getValue().entrySet())
      {
        String code = localizedProperty.getKey().toString();
        Message message = parse(String.valueOf(localizedProperty.getValue()));

        Map<Locale,Message> localizedMessages = localizedMessagesByCode.get(code);
        if (localizedMessages == null)
        {
          localizedMessages = new HashMap<Locale,Message>();
          localizedMessagesByCode.put(code, localizedMessages);
        }

        localizedMessages.put(entry.getKey(), message);
      }

    return new MessageBundle(localizedMessagesByCode);
  }


  @NotNull
  @Contract(pure = true)
  public static MessageBundle bundle(@NotNull Collection<ResourceBundle> properties)
  {
    Map<String,Map<Locale,Message>> localizedMessagesByCode = new HashMap<String,Map<Locale,Message>>();

    for(ResourceBundle resourceBundle: properties)
      for(String code: resourceBundle.keySet())
      {
        Map<Locale,Message> localizedMessages = localizedMessagesByCode.get(code);
        if (localizedMessages == null)
        {
          localizedMessages = new HashMap<Locale,Message>();
          localizedMessagesByCode.put(code, localizedMessages);
        }

        localizedMessages.put(resourceBundle.getLocale(), parse(resourceBundle.getString(code)));
      }

    return new MessageBundle(localizedMessagesByCode);
  }


  @NotNull
  @Contract(pure = true)
  static Locale forLanguageTag(@NotNull String locale)
  {
    if (locale.isEmpty())
      return ROOT;

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
