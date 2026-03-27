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
 * Message adopter that reads messages from {@link ResourceBundle ResourceBundles}. Resource bundle
 * keys are used as message codes and their values are parsed as message format strings.
 * <p>
 * Several {@code adopt} overloads are provided:
 * <ul>
 *   <li>A single {@link ResourceBundle} or a collection of bundles can be adopted directly.</li>
 *   <li>A bundle base name can be provided, in which case the adopter resolves bundles for all
 *       {@linkplain java.util.Locale#getAvailableLocales() available locales} or a specified set
 *       of locales, optionally using a custom {@link ClassLoader}.</li>
 * </ul>
 * When the same message code appears in multiple bundles (or multiple locales), the localized
 * values are combined into a single locale-aware message.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class ResourceBundleAdopter extends AbstractMessageAdopter
{
  /**
   * Create a resource bundle adopter for the given {@code configurableMessageSupport}. The message
   * factory and message publisher are both obtained from the configurable message support instance.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  public ResourceBundleAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Create a resource bundle adopter for the given {@code messageFactory} and {@code publisher}.
   * This constructor allows the message factory and message publisher to be provided
   * independently.
   *
   * @param messageFactory  message factory used for parsing message format strings, not {@code null}
   * @param publisher       message publisher used for publishing parsed messages and templates, not {@code null}
   */
  public ResourceBundleAdopter(@NotNull MessageFactory messageFactory, @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  /**
   * Adopt messages from a single resource bundle. Each key in the bundle is used as the message code and its value
   * is parsed as a message format string, associated with the bundle's {@linkplain ResourceBundle#getLocale() locale}.
   * The resulting messages are published to the {@linkplain MessagePublisher message publisher}.
   *
   * @param resourceBundle  resource bundle to adopt, not {@code null}
   *
   * @see #adopt(Collection)
   * @see #adopt(String)
   */
  @Contract(pure = true)
  public void adopt(@NotNull ResourceBundle resourceBundle)
  {
    final var locale = resourceBundle.getLocale();

    resourceBundle.keySet().forEach(
        code -> messagePublisher.addMessage(messageFactory
            .parseMessage(code, Map.of(locale, resourceBundle.getString(code)))));
  }


  /**
   * Adopt messages from a collection of resource bundles. If the same message code appears in multiple bundles, the
   * localized values are combined into a single locale-aware message. The resulting messages are published to the
   * {@linkplain MessagePublisher message publisher}.
   *
   * @param resourceBundles  resource bundles to adopt, not {@code null}
   *
   * @see #adopt(ResourceBundle)
   */
  @Contract(pure = true)
  public void adopt(@NotNull Collection<ResourceBundle> resourceBundles)
  {
    final var localizedMessagesByCode = new HashMap<String,Map<Locale,String>>();

    for(var resourceBundle: resourceBundles)
    {
      final var locale = resourceBundle.getLocale();

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


  /**
   * Adopt messages from resource bundles resolved by the given base name. Bundles are looked up for all
   * {@linkplain java.util.Locale#getAvailableLocales() available locales} using the adopter's class loader. Missing
   * bundles are silently ignored.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   *
   * @see #adopt(String, ClassLoader)
   * @see #adopt(String, Set)
   */
  @Contract(pure = true)
  public void adopt(@NotNull String bundleBaseName) {
    adopt(bundleBaseName, null, null, false);
  }


  /**
   * Adopt messages from resource bundles resolved by the given base name and class loader. Bundles are looked up for
   * all {@linkplain java.util.Locale#getAvailableLocales() available locales}. Missing bundles are silently ignored.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   * @param classLoader     class loader used for loading the resource bundles, not {@code null}
   *
   * @see #adopt(String)
   * @see #adopt(String, Set, ClassLoader)
   */
  @Contract(pure = true)
  public void adopt(@NotNull String bundleBaseName, @NotNull ClassLoader classLoader) {
    adopt(bundleBaseName, null, classLoader, false);
  }


  /**
   * Adopt messages from resource bundles resolved by the given base name for the specified set of locales.
   * The adopter's class loader is used for loading the bundles. If a bundle for a requested locale cannot be found,
   * a {@link MessageAdopterException} is thrown.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   * @param locales         set of locales to resolve bundles for, not {@code null}
   *
   * @throws MessageAdopterException  if a resource bundle for a requested locale is missing
   *
   * @see #adopt(String)
   * @see #adopt(String, Set, ClassLoader)
   */
  @Contract(pure = true)
  public void adopt(@NotNull String bundleBaseName, @NotNull Set<Locale> locales) {
    adopt(bundleBaseName, locales.toArray(Locale[]::new), null, true);
  }


  /**
   * Adopt messages from resource bundles resolved by the given base name for the specified set of locales, using
   * the provided class loader. If a bundle for a requested locale cannot be found, a {@link MessageAdopterException}
   * is thrown.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   * @param locales         set of locales to resolve bundles for, not {@code null}
   * @param classLoader     class loader used for loading the resource bundles, not {@code null}
   *
   * @throws MessageAdopterException  if a resource bundle for a requested locale is missing
   *
   * @see #adopt(String, ClassLoader)
   * @see #adopt(String, Set)
   */
  @Contract(pure = true)
  public void adopt(@NotNull String bundleBaseName, @NotNull Set<Locale> locales, @NotNull ClassLoader classLoader) {
    adopt(bundleBaseName, locales.toArray(Locale[]::new), classLoader, true);
  }


  /**
   * Internal adopt implementation that resolves resource bundles for the given base name and locales. If
   * {@code locales} is {@code null}, all {@linkplain java.util.Locale#getAvailableLocales() available locales} are
   * used and missing bundles are silently ignored. If {@code classLoader} is {@code null}, the adopter's own class
   * loader is used. When the same message code appears across multiple locales, the localized values are combined
   * into a single locale-aware message.
   *
   * @param bundleBaseName                  resource bundle base name, not {@code null}
   * @param locales                         locales to resolve, or {@code null} for all available locales
   * @param classLoader                     class loader for bundle loading, or {@code null} to use the adopter's
   *                                        class loader
   * @param throwOnMissingResourceBundle    whether to throw a {@link MessageAdopterException} when a bundle cannot
   *                                        be found
   *
   * @throws MessageAdopterException  if {@code throwOnMissingResourceBundle} is {@code true} and a resource bundle
   *                                  is missing
   */
  @Contract(pure = true)
  protected void adopt(@NotNull String bundleBaseName, Locale[] locales, ClassLoader classLoader,
                       boolean throwOnMissingResourceBundle)
  {
    final var localizedMessagesByCode = new HashMap<String,Map<Locale,String>>();

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
        final var resourceBundle = getBundle(bundleBaseName, locale, classLoader);
        final var foundLocale = resourceBundle.getLocale();

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
