/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.internal.formatter;

import de.sayayi.lib.message.internal.BoundedCache;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.Control.FORMAT_PROPERTIES;


/**
 * Localized resources for parameter formatters, stored as property files following the {@link ResourceBundle} naming
 * scheme (e.g. {@code FormatterResources.properties}, {@code FormatterResources_de.properties}).
 * <p>
 * For a locale, the property files of all its {@linkplain ResourceBundle.Control#getCandidateLocales(String, Locale)
 * candidate locales} are merged, so a key missing in a specific variant is taken from a more general variant and
 * finally from the root locale file. Unlike {@link ResourceBundle#getBundle(String, Locale)}, the default locale of the
 * JVM is never used as a fallback, so the result for a locale does not depend on the environment.
 * <p>
 * The merged resources are cached per locale. A lookup for a cached locale acquires no lock and throws no exception for
 * a missing key. Property files are read when a locale is requested for the first time, or again after its entry has
 * been evicted from the cache.
 * <p>
 * This class is thread-safe.
 *
 * @author Jeroen Gremmen
 * @since 0.26.0
 */
@Internal
public final class FormatterResources
{
  /** Maximum number of locales with cached resources. */
  private static final int LOCALE_CACHE_SIZE = 32;

  /** Control providing the candidate locales and bundle names, as used by {@link ResourceBundle}. */
  private static final ResourceBundle.Control CONTROL = ResourceBundle.Control.getControl(FORMAT_PROPERTIES);

  /** Shared resources of the parameter formatters provided by this library. */
  private static final FormatterResources SHARED =
      new FormatterResources(FormatterResources.class, "FormatterResources");

  /** Class used to locate the property files. */
  private final @NotNull Class<?> anchor;

  /** Base name of the property files, relative to the package of {@link #anchor}. */
  private final @NotNull String baseName;

  /** Merged resources by locale. */
  private final @NotNull BoundedCache<Locale,Map<String,String>> resourcesByLocale;

  /** Loader for the resources of a locale, kept in a field so a lookup does not create a method reference. */
  private final @NotNull Function<Locale,Map<String,String>> resourceLoader = this::loadResources;


  /**
   * Creates formatter resources for the property files with the given {@code baseName}.
   *
   * @param anchor    class used to locate the property files, not {@code null}. The files are located in the package
   *                  of this class and must be readable by it, which holds for files in the module of this class
   * @param baseName  base name of the property files without package and extension, not {@code null}
   */
  public FormatterResources(@NotNull Class<?> anchor, @NotNull String baseName)
  {
    this.anchor = requireNonNull(anchor, "anchor must not be null");
    this.baseName = requireNonNull(baseName, "baseName must not be null");

    resourcesByLocale = new BoundedCache<>(LOCALE_CACHE_SIZE);
  }


  /**
   * Returns the shared resources of the parameter formatters provided by this library.
   *
   * @return  shared formatter resources, never {@code null}
   */
  @Contract(pure = true)
  public static @NotNull FormatterResources shared() {
    return SHARED;
  }


  /**
   * Returns the resource with the given {@code key} for the given {@code locale}.
   *
   * @param key     resource key, not {@code null}
   * @param locale  locale, not {@code null}
   *
   * @return  resource value, or {@code null} if no resource with this key exists for {@code locale}
   *
   * @throws UncheckedIOException  if a property file exists but cannot be read
   */
  @Contract(pure = true)
  public String getResource(@NotNull String key, @NotNull Locale locale)
  {
    requireNonNull(key, "key must not be null");

    return resourcesByLocale
        .get(requireNonNull(locale, "locale must not be null"), resourceLoader)
        .get(key);
  }


  /**
   * Reads and merges the property files of all candidate locales of {@code locale}.
   *
   * @param locale  locale, not {@code null}
   *
   * @return  immutable map of merged resources, never {@code null}
   *
   * @throws UncheckedIOException  if a property file exists but cannot be read
   */
  @Contract(pure = true)
  private @NotNull Map<String,String> loadResources(@NotNull Locale locale)
  {
    final var candidateLocales = CONTROL.getCandidateLocales(baseName, locale);
    final var resources = new HashMap<String,String>();

    // most general candidate first, so more specific candidates override its entries
    for(var iterator = candidateLocales.listIterator(candidateLocales.size()); iterator.hasPrevious();)
      loadProperties(CONTROL.toBundleName(baseName, iterator.previous()), resources);

    return Map.copyOf(resources);
  }


  /**
   * Reads the property file for {@code bundleName}, if present, and adds its entries to {@code resources}.
   *
   * @param bundleName  bundle name, not {@code null}
   * @param resources   map receiving the entries, not {@code null}
   *
   * @throws UncheckedIOException  if the property file exists but cannot be read
   */
  @Contract(mutates = "param2")
  private void loadProperties(@NotNull String bundleName, @NotNull Map<String,String> resources)
  {
    final var resourceName = bundleName + ".properties";

    try(var stream = anchor.getResourceAsStream(resourceName)) {
      if (stream != null)
      {
        final var properties = new Properties();
        properties.load(new InputStreamReader(stream, UTF_8));

        for(var name: properties.stringPropertyNames())
          resources.put(name, properties.getProperty(name));
      }
    } catch(IOException ex) {
      throw new UncheckedIOException("failed to read formatter resources '" + resourceName + "'", ex);
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "FormatterResources(baseName=" + baseName + ",locales=" + resourcesByLocale.size() + ')';
  }
}
