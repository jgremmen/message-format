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
package de.sayayi.lib.message.internal.message;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.LocaleAware;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.internal.pack.PackSupport.packSmallCount;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.ROOT;
import static java.util.Locale.forLanguageTag;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableSet;


/**
 * {@link Message.WithCode} implementation that holds multiple locale-specific messages and selects the best
 * matching message at format time based on the requested locale.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0 (renamed in 0.5.0)
 */
@ApiStatus.Internal
public final class LocalizedMessageBundleWithCode extends AbstractMessageWithCode implements LocaleAware
{
  /** Localized message map. */
  private final @NotNull Map<@NotNull Locale,@NotNull Message> localizedMessages;


  /**
   * Creates a localized message bundle with code.
   *
   * @param code               message code, not {@code null} and not empty
   * @param localizedMessages  localized message map, not {@code null}. The map must contain at least 2 entries and no
   *                           mapped value can be {@code null}
   */
  public LocalizedMessageBundleWithCode(@NotNull String code, @NotNull Map<Locale,@NotNull Message> localizedMessages)
  {
    super(code);

    var size = requireNonNull(localizedMessages, "localizedMessages must not be null").size();
    if (size == 0)
      throw new IllegalArgumentException("localizedMessages must not be empty");

    this.localizedMessages = new LinkedHashMap<>(size);

    localizedMessages.forEach((locale, message) -> {
      //noinspection ConstantValue
      if (message == null)
        throw new IllegalArgumentException("localizedMessages must not contain null values");

      this.localizedMessages.put(locale == null ? ROOT : locale, message);
    });
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
  {
    var locale = parameters.getLocale();

    try {
      return findMessageByLocale(locale).formatAsText(messageAccessor, parameters);
    } catch(Exception ex) {
      throw MessageFormatException.of(ex).withCode(code).withLocale(locale);
    }
  }


  /**
   * Finds the message that best matches {@code locale}.
   * <p>
   * A message for the same language is always preferred over a message for a locale without language (e.g.
   * {@link Locale#ROOT}), which in turn is preferred over a message for any other language. Among the messages for the
   * same language, the script is compared first and the country second:
   * <ol>
   *   <li>
   *     If {@code locale} has a script, a message with the same script is preferred over a message without script,
   *     which is preferred over a message with a different script. If {@code locale} has no script, the script of the
   *     messages is not considered.
   *   </li>
   *   <li>
   *     A message for the same country is preferred over a message without country, which is preferred over a message
   *     for a different country.
   *   </li>
   * </ol>
   * The variant is not considered. If several messages match equally well, the first one in iteration order is
   * selected.
   *
   * @param locale  locale to find a message for, not {@code null}
   *
   * @return  best matching message, never {@code null}
   */
  @Contract(pure = true)
  private @NotNull Message findMessageByLocale(@NotNull Locale locale)
  {
    final var searchLanguage = locale.getLanguage();
    final var searchScript = locale.getScript();
    final var searchCountry = locale.getCountry();

    var bestMatch = -1;
    Message message = null;

    for(var entry: localizedMessages.entrySet())
    {
      final var keyLocale = entry.getKey();
      final var keyLanguage = keyLocale.getLanguage();
      final int match;

      if (keyLanguage.equals(searchLanguage))
      {
        // same script (or no script requested) / without script / different script
        final var keyScript = keyLocale.getScript();
        final var scriptMatch = searchScript.isEmpty() || keyScript.equals(searchScript)
            ? 2 : keyScript.isEmpty() ? 1 : 0;

        // same country / without country / different country
        final var keyCountry = keyLocale.getCountry();
        final var countryMatch = keyCountry.equals(searchCountry)
            ? 2 : keyCountry.isEmpty() ? 1 : 0;

        if (scriptMatch == 2 && countryMatch == 2)
          return entry.getValue();  // same language, script and country

        // same language: 2..9, the script is more significant than the country
        match = 2 + scriptMatch * 3 + countryMatch;
      }
      else
        match = keyLanguage.isEmpty() ? 1 : 0;  // "default" language / any other language

      if (match > bestMatch)
      {
        message = entry.getValue();
        bestMatch = match;
      }
    }

    return requireNonNull(message);
  }


  /** {@inheritDoc} */
  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<Locale> getLocales() {
    return unmodifiableSet(localizedMessages.keySet());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Map<Locale,Message> getLocalizedMessages() {
    return unmodifiableMap(localizedMessages);
  }


  /** {@inheritDoc} */
  @Override
  @Unmodifiable
  public @NotNull Set<String> getTemplateNames()
  {
    return localizedMessages
        .values()
        .stream()
        .flatMap(message -> message.getTemplateNames().stream())
        .collect(toUnmodifiableSet());
  }


  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o)
  {
    return o instanceof LocalizedMessageBundleWithCode that &&
        code.equals(that.code) &&
        localizedMessages.equals(that.localizedMessages);
  }


  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return (59 + code.hashCode()) * 59 + localizedMessages.hashCode();
  }


  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "LocalizedMessageBundleWithCode(" + localizedMessages + ')';
  }


  /**
   * Writes this localized message bundle with code to the given pack output stream.
   * <p>
   * A localized message bundle with more than 255 locales cannot be packed.
   *
   * @param packStream  data output pack target, not {@code null}
   *
   * @throws IOException  if the bundle has more than 255 locales or if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packSmallCount(localizedMessages.size(), "localized message bundle", "locales", packStream);
    packStream.writeString(getCode());

    for(var entry: localizedMessages.entrySet())
    {
      packStream.writeString(entry.getKey().toLanguageTag());
      PackSupport.pack(entry.getValue(), packStream);
    }
  }


  /**
   * Reads a localized message bundle with code from the given pack input stream.
   *
   * @param unpack      unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked localized message bundle with code, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull Message.WithCode unpack(@NotNull PackSupport unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    final var messageCount = packStream.readSmallVar();
    final var code = requireNonNull(packStream.readString());
    final var messages = new LinkedHashMap<Locale,Message>();

    for(var n = 0; n < messageCount; n++)
    {
      messages.put(
          forLanguageTag(requireNonNull(packStream.readString())),
          unpack.unpackMessage(packStream));
    }

    return new LocalizedMessageBundleWithCode(code, messages);
  }
}
