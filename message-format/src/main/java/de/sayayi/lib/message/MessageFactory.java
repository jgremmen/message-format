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
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import de.sayayi.lib.message.internal.LocalizedMessageBundleWithCode;
import de.sayayi.lib.message.internal.MessageDelegateWithCode;
import de.sayayi.lib.message.internal.part.MessagePart;
import de.sayayi.lib.message.parser.MessageCompiler;
import de.sayayi.lib.message.parser.normalizer.MessagePartNormalizer;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonMap;
import static java.util.Locale.ROOT;


/**
 * Factory implementation for creating message instances from various sources.
 *
 * @author Jeroen Gremmen
 */
public class MessageFactory
{
  public static final MessageFactory NO_CACHE_INSTANCE = new MessageFactory(new MessagePartNormalizer() {
    @Override public <T extends MessagePart> @NotNull T normalize(@NotNull T part) { return part; }
  });

  private static final AtomicInteger CODE_ID = new AtomicInteger(0);

  @Getter private final MessagePartNormalizer messagePartNormalizer;
  private final MessageCompiler messageCompiler;


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
   * Parse a message format text into a message instance.
   *
   * @param text  message format text, not {@code null}
   *
   * @return  message instance, never {@code null}
   */
  @Contract(value = "_ -> new", pure = true)
  public @NotNull Message.WithSpaces parse(@NotNull String text) {
    return messageCompiler.compileMessage(text);
  }


  @Contract(value = "_ -> new", pure = true)
  public @NotNull Message parse(@NotNull Map<Locale,String> localizedTexts) {
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
  @Contract(value = "_, _ -> new", pure = true)
  public @NotNull Message.WithCode parse(@NotNull String code, @NotNull String text) {
    return new MessageDelegateWithCode(code, parse(text));
  }


  @Contract(value = "_, _ -> new", pure = true)
  public @NotNull Message.WithCode parse(@NotNull String code, @NotNull Map<Locale,String> localizedTexts)
  {
    if (localizedTexts.isEmpty())
      return new EmptyMessageWithCode(code);

    final String message = localizedTexts.get(ROOT);
    if (message != null && localizedTexts.size() == 1)
      return new MessageDelegateWithCode(code, parse(message));

    final Map<Locale,Message> localizedParts = new LinkedHashMap<>();

    localizedTexts.forEach((locale,text) -> localizedParts.put(locale, parse(text)));

    return new LocalizedMessageBundleWithCode(code, localizedParts);
  }


  @SuppressWarnings("WeakerAccess")
  @Contract(value = "_ -> new", pure = true)
  public @NotNull Set<Message.WithCode> parseAnnotations(@NotNull AnnotatedElement element)
  {
    final Set<Message.WithCode> messages = new HashSet<>();

    MessageDef annotation = element.getAnnotation(MessageDef.class);
    if (annotation != null)
      messages.add(parse(annotation));

    MessageDefs messageDefsAnnotation = element.getAnnotation(MessageDefs.class);
    if (messageDefsAnnotation != null)
      for(MessageDef messageDef: messageDefsAnnotation.value())
      {
        Message.WithCode mwc = parse(messageDef);

        if (!messages.add(mwc))
          throw new MessageException("duplicate message code " + mwc.getCode() + " found");

        messages.add(mwc);
      }

    return messages;
  }


  @Contract(value = "_ -> new", pure = true)
  public @NotNull Message.WithCode parse(@NotNull MessageDef annotation)
  {
    final Text[] texts = annotation.texts();
    final String code = annotation.code();

    if (texts.length == 0)
    {
      String text = annotation.text();
      return text.isEmpty() ? new EmptyMessageWithCode(code) : parse(code, text);
    }

    final Map<Locale,String> localizedTexts = new LinkedHashMap<>();

    for(final Text text: texts)
    {
      final Locale locale = forLanguageTag(text.locale());
      final String value = text.locale().isEmpty() && text.text().isEmpty() ? text.value() : text.text();

      if (!localizedTexts.containsKey(locale))
        localizedTexts.put(locale, value);
    }

    return parse(code, localizedTexts);
  }


  @Contract(pure = true)
  public @NotNull Message.WithCode withCode(@NotNull String code, @NotNull Message message)
  {
    if (message instanceof MessageDelegateWithCode)
      return new MessageDelegateWithCode(code, ((MessageDelegateWithCode)message).getMessage());

    if (message instanceof Message.WithCode)
    {
      final Message.WithCode messageWithCode = (Message.WithCode)message;

      if (code.equals(messageWithCode.getCode()))
        return messageWithCode;
    }

    if (message instanceof EmptyMessage || message instanceof EmptyMessageWithCode)
      return new EmptyMessageWithCode(code);

    return new MessageDelegateWithCode(code, message);
  }


  @Contract(pure = true)
  public @NotNull MessageBundle bundle(@NotNull Properties properties)
  {
    final MessageBundle bundle = new MessageBundle(this);

    properties.forEach((k,v) -> bundle.add(parse(k.toString(), String.valueOf(v))));

    return bundle;
  }


  @Contract(pure = true)
  public @NotNull MessageBundle bundle(@NotNull ResourceBundle resourceBundle)
  {
    final MessageBundle bundle = new MessageBundle(this);
    final Locale locale = resourceBundle.getLocale();

    resourceBundle.keySet().forEach(
        code -> bundle.add(parse(code, singletonMap(locale, resourceBundle.getString(code)))));

    return bundle;
  }


  @Contract(pure = true)
  public @NotNull MessageBundle bundle(@NotNull Map<Locale,Properties> properties)
  {
    final Map<String,Map<Locale,Message>> localizedMessagesByCode = new HashMap<>();

    for(Entry<Locale,Properties> entry: properties.entrySet())
      for(Entry<Object,Object> localizedProperty: entry.getValue().entrySet())
      {
        localizedMessagesByCode.computeIfAbsent(localizedProperty.getKey().toString(), k -> new HashMap<>())
            .put(entry.getKey(), parse(String.valueOf(localizedProperty.getValue())));
      }

    return new MessageBundle(this, localizedMessagesByCode);
  }


  @Contract(pure = true)
  public @NotNull MessageBundle bundle(@NotNull Collection<ResourceBundle> properties)
  {
    final Map<String,Map<Locale,Message>> localizedMessagesByCode = new HashMap<>();

    for(ResourceBundle resourceBundle: properties)
      for(String code: resourceBundle.keySet())
      {
        localizedMessagesByCode.computeIfAbsent(code, k -> new HashMap<>())
            .put(resourceBundle.getLocale(), parse(resourceBundle.getString(code)));
      }

    return new MessageBundle(this, localizedMessagesByCode);
  }


  @Contract(pure = true)
  static @NotNull Locale forLanguageTag(@NotNull String locale)
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
