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
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static java.util.Locale.getAvailableLocales;
import static java.util.ResourceBundle.getBundle;


/**
 * Message adopter that reads messages and templates from {@link ResourceBundle ResourceBundles}. Resource bundle keys
 * are used as message codes or template names and their values are parsed as message or template format strings.
 * <p>
 * Several {@code adopt} overloads are provided:
 * <ul>
 *   <li>
 *     A single {@link ResourceBundle} or a collection of bundles can be adopted directly as messages or templates.
 *   </li>
 *   <li>
 *     A bundle base name can be provided, in which case the adopter resolves bundles for all
 *     {@linkplain java.util.Locale#getAvailableLocales() available locales} or a specified set of locales, optionally
 *     using a custom {@link ClassLoader}.
 *   </li>
 * </ul>
 * When the same message code or template name appears in multiple bundles (or multiple locales), the localized values
 * are combined into a single locale-aware message or template.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@SuppressWarnings("DuplicatedCode")
public class ResourceBundleAdopter extends AbstractMessageAdopter
{
  /**
   * Creates a resource bundle adopter for the given {@code configurableMessageSupport}. The message factory and message
   * publisher are both obtained from the configurable message support instance.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  public ResourceBundleAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Creates a resource bundle adopter for the given {@code messageFactory} and {@code publisher}. This constructor
   * allows the message factory and message publisher to be provided independently.
   *
   * @param messageFactory  message factory used for parsing message format strings, not {@code null}
   * @param publisher       message publisher used for publishing parsed messages and templates, not {@code null}
   */
  public ResourceBundleAdopter(@NotNull MessageFactory messageFactory, @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  /**
   * Adopts messages from a single resource bundle. Each key in the bundle is used as the message code and its value is
   * parsed as a message format string. As only a single locale is involved, the resulting messages are independent of
   * the locale; the bundle's {@linkplain ResourceBundle#getLocale() locale} is only used for reporting parser errors.
   * The resulting messages are published to the {@linkplain MessagePublisher message publisher}.
   *
   * @param resourceBundle  resource bundle to adopt, not {@code null}
   *
   * @since 0.11.0
   *
   * @see #adopt(Collection)
   * @see #adopt(String)
   */
  public void adopt(@NotNull ResourceBundle resourceBundle)
  {
    final var locale = resourceBundle.getLocale();

    resourceBundle.keySet().forEach(
        code -> messagePublisher.addMessage(messageFactory
            .parseMessage(code, Map.of(locale, resourceBundle.getString(code)))));
  }


  /**
   * Adopts templates from a single resource bundle. Each key in the bundle is used as the template name and its value
   * is parsed as a template format string. As only a single locale is involved, the resulting templates are independent
   * of the locale; the bundle's {@linkplain ResourceBundle#getLocale() locale} is only used for reporting parser
   * errors. The resulting templates are published to the {@linkplain MessagePublisher message publisher}.
   *
   * @param resourceBundle  resource bundle to adopt, not {@code null}
   *
   * @see #adoptTemplates(Collection)
   *
   * @since 0.25.0
   */
  public void adoptTemplates(@NotNull ResourceBundle resourceBundle)
  {
    final var locale = resourceBundle.getLocale();

    resourceBundle.keySet().forEach(
        name -> messagePublisher.addTemplate(name, messageFactory
            .parseTemplate(Map.of(locale, resourceBundle.getString(name)))));
  }


  /**
   * Adopts messages from a collection of resource bundles. If the same message code appears in multiple bundles, the
   * localized values are combined into a single locale-aware message. The resulting messages are published to the
   * {@linkplain MessagePublisher message publisher}.
   *
   * @param resourceBundles  resource bundles to adopt, not {@code null}
   *
   * @see #adopt(ResourceBundle)
   */
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
   * Adopts templates from a collection of resource bundles. If the same template name appears in multiple bundles, the
   * localized values are combined into a single locale-aware template. The resulting templates are published to the
   * {@linkplain MessagePublisher message publisher}.
   *
   * @param resourceBundles  resource bundles to adopt, not {@code null}
   *
   * @see #adoptTemplates(ResourceBundle)
   *
   * @since 0.25.0
   */
  public void adoptTemplates(@NotNull Collection<ResourceBundle> resourceBundles)
  {
    final var localizedTemplatesByName = new HashMap<String,Map<Locale,String>>();

    for(var resourceBundle: resourceBundles)
    {
      final var locale = resourceBundle.getLocale();

      for(var name: resourceBundle.keySet())
      {
        localizedTemplatesByName
            .computeIfAbsent(name, k -> new HashMap<>())
            .put(locale, resourceBundle.getString(name));
      }
    }

    localizedTemplatesByName.forEach((name,localizedTexts) ->
        messagePublisher.addTemplate(name, messageFactory.parseTemplate(localizedTexts)));
  }


  /**
   * Adopts messages from resource bundles resolved by the given base name. Bundles are looked up for all
   * {@linkplain java.util.Locale#getAvailableLocales() available locales} using the adopter's class loader. A bundle
   * is only adopted for the locale it has been provided for; missing bundles are silently ignored.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   *
   * @see #adopt(String, ClassLoader)
   * @see #adopt(String, Set)
   */
  public void adopt(@NotNull String bundleBaseName) {
    adopt(bundleBaseName, null, null, false);
  }


  /**
   * Adopts templates from resource bundles resolved by the given base name. Bundles are looked up for all
   * {@linkplain java.util.Locale#getAvailableLocales() available locales} using the adopter's class loader. A bundle is
   * only adopted for the locale it has been provided for; missing bundles are silently ignored.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   *
   * @see #adoptTemplates(String, ClassLoader)
   * @see #adoptTemplates(String, Set)
   *
   * @since 0.25.0
   */
  public void adoptTemplates(@NotNull String bundleBaseName) {
    adoptTemplates(bundleBaseName, null, null, false);
  }


  /**
   * Adopts messages from resource bundles resolved by the given base name and class loader. Bundles are looked up for
   * all {@linkplain java.util.Locale#getAvailableLocales() available locales}. A bundle is only adopted for the locale
   * it has been provided for; missing bundles are silently ignored.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   * @param classLoader     class loader used for loading the resource bundles, not {@code null}
   *
   * @see #adopt(String)
   * @see #adopt(String, Set, ClassLoader)
   */
  public void adopt(@NotNull String bundleBaseName, @NotNull ClassLoader classLoader) {
    adopt(bundleBaseName, null, classLoader, false);
  }


  /**
   * Adopts templates from resource bundles resolved by the given base name and class loader. Bundles are looked up for
   * all {@linkplain java.util.Locale#getAvailableLocales() available locales}. A bundle is only adopted for the locale
   * it has been provided for; missing bundles are silently ignored.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   * @param classLoader     class loader used for loading the resource bundles, not {@code null}
   *
   * @see #adoptTemplates(String)
   * @see #adoptTemplates(String, Set, ClassLoader)
   *
   * @since 0.25.0
   */
  public void adoptTemplates(@NotNull String bundleBaseName, @NotNull ClassLoader classLoader) {
    adoptTemplates(bundleBaseName, null, classLoader, false);
  }


  /**
   * Adopts messages from resource bundles resolved by the given base name for the specified set of locales. The
   * adopter's class loader is used for loading the bundles. If no bundle exists for exactly a requested locale, a
   * {@link MessageAdopterException} is thrown; a bundle for a parent locale, the default locale or the base bundle does
   * not count as a bundle for the requested locale. Keys inherited from parent bundles are adopted for the requested
   * locale as well.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   * @param locales         set of locales to resolve bundles for, not {@code null}
   *
   * @throws MessageAdopterException  if the resource bundle for a requested locale is missing
   *
   * @see #adopt(String)
   * @see #adopt(String, Set, ClassLoader)
   */
  public void adopt(@NotNull String bundleBaseName, @NotNull Set<Locale> locales) {
    adopt(bundleBaseName, locales.toArray(Locale[]::new), null, true);
  }


  /**
   * Adopts templates from resource bundles resolved by the given base name for the specified set of locales. The
   * adopter's class loader is used for loading the bundles. If no bundle exists for exactly a requested locale, a
   * {@link MessageAdopterException} is thrown; a bundle for a parent locale, the default locale or the base bundle does
   * not count as a bundle for the requested locale. Keys inherited from parent bundles are adopted for the requested
   * locale as well.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   * @param locales         set of locales to resolve, not {@code null}
   *
   * @throws MessageAdopterException  if the resource bundle for a requested locale is missing
   *
   * @see #adoptTemplates(String)
   * @see #adoptTemplates(String, Set, ClassLoader)
   *
   * @since 0.25.0
   */
  public void adoptTemplates(@NotNull String bundleBaseName, @NotNull Set<Locale> locales) {
    adoptTemplates(bundleBaseName, locales.toArray(Locale[]::new), null, true);
  }


  /**
   * Adopts messages from resource bundles resolved by the given base name for the specified set of locales, using the
   * provided class loader. If no bundle exists for exactly a requested locale, a {@link MessageAdopterException} is
   * thrown; a bundle for a parent locale, the default locale or the base bundle does not count as a bundle for the
   * requested locale. Keys inherited from parent bundles are adopted for the requested locale as well.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   * @param locales         set of locales to resolve bundles for, not {@code null}
   * @param classLoader     class loader used for loading the resource bundles, not {@code null}
   *
   * @throws MessageAdopterException  if the resource bundle for a requested locale is missing
   *
   * @see #adopt(String, ClassLoader)
   * @see #adopt(String, Set)
   */
  public void adopt(@NotNull String bundleBaseName, @NotNull Set<Locale> locales, @NotNull ClassLoader classLoader) {
    adopt(bundleBaseName, locales.toArray(Locale[]::new), classLoader, true);
  }


  /**
   * Adopts templates from resource bundles resolved by the given base name for the specified set of locales, using the
   * provided class loader. If no bundle exists for exactly a requested locale, a {@link MessageAdopterException} is
   * thrown; a bundle for a parent locale, the default locale or the base bundle does not count as a bundle for the
   * requested locale. Keys inherited from parent bundles are adopted for the requested locale as well.
   *
   * @param bundleBaseName  resource bundle base name, not {@code null}
   * @param locales         set of locales to resolve, not {@code null}
   * @param classLoader     class loader used for loading the bundles, not {@code null}
   *
   * @throws MessageAdopterException  if the resource bundle for a requested locale is missing
   *
   * @see #adoptTemplates(String, ClassLoader)
   * @see #adoptTemplates(String, Set)
   *
   * @since 0.25.0
   */
  public void adoptTemplates(@NotNull String bundleBaseName, @NotNull Set<Locale> locales,
                             @NotNull ClassLoader classLoader) {
    adoptTemplates(bundleBaseName, locales.toArray(Locale[]::new), classLoader, true);
  }


  /**
   * Internal adopt implementation that resolves resource bundles for the given base name and locales. If
   * {@code locales} is {@code null}, all {@linkplain java.util.Locale#getAvailableLocales() available locales} are used
   * and missing bundles are silently ignored. If {@code classLoader} is {@code null}, the adopter's own class loader is
   * used. A bundle found for a different locale than the requested one is treated as missing. When the same message
   * code appears across multiple locales, the localized values are combined into a single locale-aware message.
   *
   * @param bundleBaseName                resource bundle base name, not {@code null}
   * @param locales                       locales to resolve, or {@code null} for all available locales
   * @param classLoader                   class loader for bundle loading, or {@code null} to use the adopter's class
   *                                      loader
   * @param throwOnMissingResourceBundle  whether to throw a {@link MessageAdopterException} when a bundle cannot be
   *                                      found
   *
   * @throws MessageAdopterException  if {@code throwOnMissingResourceBundle} is {@code true} and a resource bundle is
   *                                  missing
   */
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
      final var resourceBundle =
          getResourceBundle(bundleBaseName, locale, classLoader, throwOnMissingResourceBundle);

      if (resourceBundle != null)
      {
        for(var code: resourceBundle.keySet())
        {
          localizedMessagesByCode
              .computeIfAbsent(code, k -> new HashMap<>())
              .put(locale, resourceBundle.getString(code));
        }
      }
    }

    localizedMessagesByCode.forEach((code,localizedTexts) ->
        messagePublisher.addMessage(messageFactory.parseMessage(code, localizedTexts)));
  }


  /**
   * Internal template adoption implementation that resolves resource bundles for the given base name and locales. If
   * {@code locales} is {@code null}, all {@linkplain java.util.Locale#getAvailableLocales() available locales} are used
   * and missing bundles are silently ignored. If {@code classLoader} is {@code null}, the adopter's own class loader is
   * used. A bundle found for a different locale than the requested one is treated as missing.
   *
   * @param bundleBaseName                resource bundle base name, not {@code null}
   * @param locales                       locales to resolve, or {@code null} for all available locales
   * @param classLoader                   class loader for bundle loading, or {@code null} to use the adopter's class
   *                                      loader
   * @param throwOnMissingResourceBundle  whether to throw a {@link MessageAdopterException} when a bundle cannot be
   *                                      found
   *
   * @throws MessageAdopterException  if {@code throwOnMissingResourceBundle} is {@code true} and a resource bundle
   *                                  is missing
   *
   * @since 0.25.0
   */
  private void adoptTemplates(@NotNull String bundleBaseName, Locale[] locales, ClassLoader classLoader,
                              boolean throwOnMissingResourceBundle)
  {
    final var localizedTemplatesByName = new HashMap<String,Map<Locale,String>>();

    if (locales == null)
    {
      locales = getAvailableLocales();
      throwOnMissingResourceBundle = false;
    }

    if (classLoader == null)
      classLoader = getClass().getClassLoader();

    for(var locale: locales)
    {
      final var resourceBundle =
          getResourceBundle(bundleBaseName, locale, classLoader, throwOnMissingResourceBundle);

      if (resourceBundle != null)
        for(var name: resourceBundle.keySet())
        {
          localizedTemplatesByName
              .computeIfAbsent(name, k -> new HashMap<>())
              .put(locale, resourceBundle.getString(name));
        }
    }

    localizedTemplatesByName.forEach((name,localizedTexts) ->
        messagePublisher.addTemplate(name, messageFactory.parseTemplate(localizedTexts)));
  }


  /**
   * Returns the resource bundle for the given base name and exactly the given {@code locale}. A bundle found by
   * {@link ResourceBundle#getBundle(String, Locale, ClassLoader)} for a different locale, i.e. a bundle for a parent
   * locale, the {@linkplain Locale#getDefault() default locale} or the base bundle, is treated as missing. The keys of
   * the returned bundle include the keys inherited from its parent bundles.
   *
   * @param bundleBaseName                resource bundle base name, not {@code null}
   * @param locale                        locale to resolve, not {@code null}
   * @param classLoader                   class loader for bundle loading, not {@code null}
   * @param throwOnMissingResourceBundle  whether to throw a {@link MessageAdopterException} when the bundle is missing
   *
   * @return  resource bundle for {@code locale} or {@code null} if the bundle is missing and
   *          {@code throwOnMissingResourceBundle} is {@code false}
   *
   * @throws MessageAdopterException  if {@code throwOnMissingResourceBundle} is {@code true} and the resource bundle is
   *                                  missing
   */
  private static ResourceBundle getResourceBundle(@NotNull String bundleBaseName, @NotNull Locale locale,
                                                  @NotNull ClassLoader classLoader,
                                                  boolean throwOnMissingResourceBundle)
  {
    try {
      final var resourceBundle = getBundle(bundleBaseName, locale, classLoader);
      if (resourceBundle.getLocale().equals(locale))
        return resourceBundle;
    } catch(MissingResourceException ex) {
      if (throwOnMissingResourceBundle)
        throw new MessageAdopterException(ex.getLocalizedMessage(), ex);

      return null;
    }

    if (throwOnMissingResourceBundle)
    {
      throw new MessageAdopterException("missing resource bundle '" + bundleBaseName + "' for locale '" + locale +
          '\'');
    }

    return null;
  }
}
