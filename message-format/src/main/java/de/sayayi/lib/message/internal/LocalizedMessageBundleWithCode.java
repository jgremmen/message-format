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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.LocaleAware;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.forLanguageTag;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;


/**
 * @author Jeroen Gremmen
 * @since 0.1.0 (renamed in 0.5.0)
 */
public final class LocalizedMessageBundleWithCode extends AbstractMessageWithCode
    implements LocaleAware
{
  /** Localized message map. */
  private final @NotNull Map<Locale,Message> localizedMessages;


  /**
   * Create a localized message bundle with code.
   *
   * @param code               message code, not {@code null} and not empty
   * @param localizedMessages  localized message map, not {@code null}. The map must contain at
   *                           least 2 entries and no mapped value can be {@code null}
   */
  public LocalizedMessageBundleWithCode(@NotNull String code,
                                        @NotNull Map<Locale,Message> localizedMessages)
  {
    super(code);

    if (requireNonNull(localizedMessages, "localizedMessages must not be null").isEmpty())
      throw new IllegalArgumentException("localizedMessages must not be empty");

    this.localizedMessages = new HashMap<>(localizedMessages);
  }


  @Override
  public @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor,
                                    @NotNull Parameters parameters) throws MessageFormatException
  {
    final Locale locale = parameters.getLocale();

    try {
      return findMessageByLocale(locale).formatAsText(messageAccessor, parameters);
    } catch(Exception ex) {
      throw MessageFormatException.of(ex).withCode(code).withLocale(locale);
    }
  }


  @Contract(pure = true)
  private @NotNull Message findMessageByLocale(@NotNull Locale locale)
  {
    final String searchLanguage = locale.getLanguage();
    final String searchCountry = locale.getCountry();

    int match = -1;
    Message message = null;

    for(final Entry<Locale,Message> entry: localizedMessages.entrySet())
    {
      final Locale keyLocale = entry.getKey();
      final Message localizedMessage = entry.getValue();

      if (match == -1 && (keyLocale == null || keyLocale.getLanguage().isEmpty()))
      {
        message = localizedMessage;
        match = 0;  // "default" language match
      }
      else if (keyLocale != null && keyLocale.getLanguage().equals(searchLanguage))
      {
        if (keyLocale.getCountry().equals(searchCountry))
          return localizedMessage;

        if (match < 1)
        {
          message = localizedMessage;
          match = 1;  // same language, different country
        }
      }

      if (message == null)
        message = localizedMessage;  // 1st match
    }

    return requireNonNull(message);
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<Locale> getLocales() {
    return unmodifiableSet(localizedMessages.keySet());
  }


  @Override
  public @NotNull Map<Locale,Message> getLocalizedMessages() {
    return unmodifiableMap(localizedMessages);
  }


  @Override
  public @NotNull MessagePart[] getMessageParts() {
    throw new UnsupportedOperationException("getMessageParts");
  }


  @Override
  public @NotNull Set<String> getTemplateNames()
  {
    return unmodifiableSet(localizedMessages
        .values()
        .stream()
        .flatMap(message -> message.getTemplateNames().stream())
        .collect(toSet()));
  }


  @Override
  @SuppressWarnings("DuplicatedCode")
  public boolean isSame(@NotNull Message message)
  {
    if (message instanceof MessageDelegateWithCode)
      message = ((MessageDelegateWithCode)message).getMessage();

    if (!(message instanceof LocaleAware))
      return false;

    final Map<Locale,Message> lm = ((LocaleAware)message).getLocalizedMessages();

    if (!localizedMessages.keySet().equals(lm.keySet()))
      return false;

    for(final Entry<Locale,Message> entry: localizedMessages.entrySet())
      if (!entry.getValue().isSame(lm.get(entry.getKey())))
        return false;

    return true;
  }


  @Override
  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    else if (!(o instanceof LocalizedMessageBundleWithCode))
      return false;

    final LocalizedMessageBundleWithCode that = (LocalizedMessageBundleWithCode)o;

    return code.equals(that.code) && localizedMessages.equals(that.localizedMessages);
  }


  @Override
  public int hashCode() {
    return (59 + code.hashCode()) * 59 + localizedMessages.hashCode();
  }


  @Override
  public String toString() {
    return "LocalizedMessageBundleWithCode(" + localizedMessages + ')';
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  @SuppressWarnings("JavadocDeclaration")
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeSmallVar(localizedMessages.size());
    packStream.writeString(getCode());

    for(final Entry<Locale,Message> entry: localizedMessages.entrySet())
    {
      packStream.writeString(entry.getKey().toLanguageTag());
      PackHelper.pack(entry.getValue(), packStream);
    }
  }


  /**
   * @param unpack      unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked localized message bundle with code, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  @SuppressWarnings("JavadocDeclaration")
  public static @NotNull Message.WithCode unpack(@NotNull PackHelper unpack,
                                                 @NotNull PackInputStream packStream)
      throws IOException
  {
    final int messageCount = packStream.readSmallVar();
    final String code = requireNonNull(packStream.readString());
    final Map<Locale,Message> messages = new HashMap<>();

    for(int n = 0; n < messageCount; n++)
    {
      messages.put(
          forLanguageTag(requireNonNull(packStream.readString())),
          unpack.unpackMessage(packStream));
    }

    return new LocalizedMessageBundleWithCode(code, messages);
  }
}
