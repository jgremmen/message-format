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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.LocaleAware;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.exception.MessageException;
import lombok.Synchronized;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map.Entry;

import static java.util.Collections.*;
import static java.util.Locale.ROOT;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class LocalizedMessageBundleWithCode extends AbstractMessageWithCode implements LocaleAware
{
  private static final long serialVersionUID = 800L;

  private final @NotNull Map<Locale,Message> localizedMessages;

  private Boolean hasParameter;


  public LocalizedMessageBundleWithCode(@NotNull String code, @NotNull Map<Locale,Message> localizedMessages)
  {
    super(code);

    this.localizedMessages = localizedMessages;
  }


  @Override
  @Contract(pure = true)
  public @NotNull String format(@NotNull MessageContext messageContext, @NotNull Parameters parameters) {
    return findMessageByLocale(parameters.getLocale()).format(messageContext, parameters);
  }


  @Contract(pure = true)
  private @NotNull Message findMessageByLocale(@NotNull Locale locale)
  {
    if (localizedMessages.isEmpty())
      throw new MessageException("message bundle with code " + getCode() + " contains no messages");

    final String searchLanguage = locale.getLanguage();
    final String searchCountry = locale.getCountry();

    int match = -1;
    Message message = null;

    for(final Entry<Locale,Message> entry: localizedMessages.entrySet())
    {
      final Locale keyLocale = entry.getKey();

      if (message == null)
        message = entry.getValue();

      if (match == -1 && (keyLocale == null || ROOT.equals(keyLocale)))
      {
        message = entry.getValue();
        match = 0;
      }
      else if (keyLocale.getLanguage().equals(searchLanguage))
      {
        if (keyLocale.getCountry().equals(searchCountry))
          return entry.getValue();
        else if (match < 1)
        {
          message = entry.getValue();
          match = 1;
        }
      }
    }

    return message;
  }


  @Synchronized
  @Override
  @Contract(pure = true)
  public boolean hasParameters()
  {
    if (hasParameter == null)
    {
      hasParameter = Boolean.FALSE;

      for(final Message message: localizedMessages.values())
        if (message.hasParameters())
        {
          hasParameter = Boolean.TRUE;
          break;
        }
    }

    return hasParameter;
  }


  @Override
  public @NotNull SortedSet<String> getParameterNames()
  {
    if (!hasParameters())
      return emptySortedSet();

    final SortedSet<String> parameterNames = new TreeSet<>();

    for(final Message message: localizedMessages.values())
      parameterNames.addAll(message.getParameterNames());

    return parameterNames;
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
}