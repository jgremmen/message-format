/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.adopter;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import de.sayayi.lib.message.exception.MessageAdopterException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static java.util.Locale.getAvailableLocales;
import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class ResourceBundleAdopter extends AbstractMessageAdopter
{
  /**
   * Create a resource bundle adopter for the given {@code configurableMessageSupport}.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  public ResourceBundleAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Create a resource bundle adopter for the given {@code messageFactory} and {@code publisher}.
   *
   * @param messageFactory  message factory, not {@code null}
   * @param publisher       message publisher, not {@code null}
   */
  public ResourceBundleAdopter(@NotNull MessageFactory messageFactory, @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  @Contract(pure = true)
  public void adopt(@NotNull ResourceBundle resourceBundle)
  {
    final Locale locale = resourceBundle.getLocale();

    resourceBundle.keySet().forEach(
        code -> messagePublisher.addMessage(messageFactory
            .parseMessage(code, Map.of(locale, resourceBundle.getString(code)))));
  }


  @Contract(pure = true)
  public void adopt(@NotNull Collection<ResourceBundle> resourceBundles)
  {
    final Map<String,Map<Locale,String>> localizedMessagesByCode = new HashMap<>();

    for(var resourceBundle: resourceBundles)
    {
      final Locale locale = resourceBundle.getLocale();

      for(var code: resourceBundle.keySet())
      {
        localizedMessagesByCode
            .computeIfAbsent(code, k -> new HashMap<>())
            .put(locale, resourceBundle.getString(code));
      }
    }

    localizedMessagesByCode.forEach((code,localizedTexts) ->
        messagePublisher.addMessage(messageFactory.parseMessage(code, localizedTexts)));
  }


  @Contract(pure = true)
  public void adopt(@NotNull String bundleBaseName) {
    adopt(bundleBaseName, null, null, false);
  }


  @Contract(pure = true)
  public void adopt(@NotNull String bundleBaseName, @NotNull ClassLoader classLoader) {
    adopt(bundleBaseName, null, classLoader, false);
  }


  @Contract(pure = true)
  public void adopt(@NotNull String bundleBaseName, @NotNull Set<Locale> locales) {
    adopt(bundleBaseName, locales.toArray(Locale[]::new), null, true);
  }


  @Contract(pure = true)
  public void adopt(@NotNull String bundleBaseName, @NotNull Set<Locale> locales, @NotNull ClassLoader classLoader) {
    adopt(bundleBaseName, locales.toArray(Locale[]::new), classLoader, true);
  }


  @Contract(pure = true)
  protected void adopt(@NotNull String bundleBaseName, Locale[] locales, ClassLoader classLoader,
                       boolean throwOnMissingResourceBundle)
  {
    final Map<String,Map<Locale,String>> localizedMessagesByCode = new HashMap<>();

    if (locales == null)
    {
      locales = getAvailableLocales();
      throwOnMissingResourceBundle = false;
    }

    if (classLoader == null)
      classLoader = getClass().getClassLoader();

    for(var locale: locales)
    {
      try {
        final ResourceBundle resourceBundle = getBundle(bundleBaseName, locale, classLoader);
        final Locale foundLocale = resourceBundle.getLocale();

        for(var code: resourceBundle.keySet())
        {
          localizedMessagesByCode
              .computeIfAbsent(code, k -> new HashMap<>())
              .put(foundLocale, resourceBundle.getString(code));
        }
      } catch(MissingResourceException ex) {
        if (throwOnMissingResourceBundle)
          throw new MessageAdopterException(ex.getLocalizedMessage(), ex);
      }
    }

    localizedMessagesByCode.forEach((code,localizedTexts) ->
        messagePublisher.addMessage(messageFactory.parseMessage(code, localizedTexts)));
  }
}
