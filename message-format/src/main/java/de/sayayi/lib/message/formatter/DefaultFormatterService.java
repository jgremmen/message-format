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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.post.PostFormatter;

import java.util.ServiceLoader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Formatter service that automatically discovers and registers parameter formatters and post formatters available on
 * the classpath via the Java {@link ServiceLoader} mechanism.
 * <p>
 * This service extends {@link GenericFormatterService} by loading all {@link ParameterFormatter} and
 * {@link PostFormatter} implementations that are declared as services. A shared singleton instance is available via
 * {@link #getSharedInstance()}.
 * <p>
 * Subclasses can override {@link #addDefaultFormatters()} or the individual
 * {@link #addParameterFormattersFromService()} and {@link #addPostFormattersFromService()} methods to customize which
 * formatters are loaded.
 *
 * @author Jeroen Gremmen
 * @since 0.4.2
 */
public class DefaultFormatterService extends GenericFormatterService
{
  private static final Lock $LOCK = new ReentrantLock();

  private static FormatterService INSTANCE = null;

  /**
   * Classloader to be used to load parameter formatter service classes or {@code null} for the system class loader.
   */
  protected final ClassLoader classLoader;


  /**
   * Returns a shared instance of the default formatter service. This service includes all parameter formatters which
   * are available as a service and accessible by the system class loader.
   *
   * @return  shared instance of the default formatter service, never {@code null}
   */
  public static FormatterService getSharedInstance()
  {
    $LOCK.lock();
    try {
      if (INSTANCE == null)
        INSTANCE = new DefaultFormatterService();
    } finally {
      $LOCK.unlock();
    }

    return INSTANCE;
  }


  /**
   * Creates a default formatter service using the system class loader and the
   * {@link #DEFAULT_FORMATTER_CACHE_SIZE default} cache size.
   */
  public DefaultFormatterService() {
    this(null, DEFAULT_FORMATTER_CACHE_SIZE);
  }


  /**
   * Creates a default formatter service using the system class loader and the given cache size.
   *
   * @param formatterCacheSize  maximum number of type-to-formatter mappings to cache
   */
  public DefaultFormatterService(int formatterCacheSize) {
    this(null, formatterCacheSize);
  }


  /**
   * Creates a default formatter service using the given class loader and cache size.
   *
   * @param classLoader         class loader used to discover service-provided formatters, or {@code null} for
   *                            the system class loader
   * @param formatterCacheSize  maximum number of type-to-formatter mappings to cache
   */
  @SuppressWarnings("WeakerAccess")
  public DefaultFormatterService(ClassLoader classLoader, int formatterCacheSize)
  {
    super(formatterCacheSize);

    this.classLoader = classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;

    addDefaultFormatters();
  }


  /**
   * Adds the default formatters for this service. This method is called during construction and loads both parameter
   * formatters and post formatters via the {@link ServiceLoader}.
   * <p>
   * Subclasses can override this method to customize which formatters are registered.
   *
   * @see #addParameterFormattersFromService()
   * @see #addPostFormattersFromService()
   */
  @SuppressWarnings("WeakerAccess")
  protected void addDefaultFormatters()
  {
    addParameterFormattersFromService();
    addPostFormattersFromService();
  }


  /**
   * Discovers and registers all {@link ParameterFormatter} implementations available on the classpath via the
   * {@link ServiceLoader} mechanism.
   */
  protected void addParameterFormattersFromService()
  {
    ServiceLoader
        .load(ParameterFormatter.class, classLoader)
        .forEach(this::addFormatter);
  }


  /**
   * Discovers and registers all {@link PostFormatter} implementations available on the classpath via the
   * {@link ServiceLoader} mechanism.
   *
   * @since 0.20.0
   */
  protected void addPostFormattersFromService()
  {
    ServiceLoader
        .load(PostFormatter.class, classLoader)
        .forEach(this::addPostFormatter);
  }
}
