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

import java.util.ServiceLoader;


/**
 * Formatter service providing formatters for the most common java types.
 *
 * @author Jeroen Gremmen
 * @since 0.4.2
 */
public class DefaultFormatterService extends GenericFormatterService
{
  private static final Object $LOCK = new Object[0];

  private static FormatterService INSTANCE = null;

  /**
   * Classloader to be used to load parameter formatter service classes or {@code null} for
   * the system class loader.
   */
  protected final ClassLoader classLoader;


  /**
   * Returns a shared instance of the default formatter service. This service includes all parameter
   * formatters which are available as a service and accessible by the system class loader.
   *
   * @return  shared instance of the default formatter service, never {@code null}
   */
  public static FormatterService getSharedInstance()
  {
    synchronized($LOCK) {
      if (INSTANCE == null)
        INSTANCE = new DefaultFormatterService();

      return INSTANCE;
    }
  }


  public DefaultFormatterService() {
    this(null, DEFAULT_FORMATTER_CACHE_SIZE);
  }


  public DefaultFormatterService(int formatterCacheSize) {
    this(null, formatterCacheSize);
  }


  @SuppressWarnings("WeakerAccess")
  public DefaultFormatterService(ClassLoader classLoader, int formatterCacheSize)
  {
    super(formatterCacheSize);

    this.classLoader = classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;

    addDefaultFormatters();
  }


  /**
   * Adds the default formatters for this service.
   */
  @SuppressWarnings("WeakerAccess")
  protected void addDefaultFormatters()
  {
    addParameterFormattersFromService();
    addParameterPostFormattersFromService();
  }


  /**
   * Adds all parameter formatters on the classpath which are defined as a service.
   */
  protected void addParameterFormattersFromService()
  {
    ServiceLoader
        .load(ParameterFormatter.class, classLoader)
        .forEach(this::addFormatter);
  }


  /**
   * Adds all post-formatters on the classpath which are defined as a service.
   *
   * @since 0.20.0
   */
  protected void addParameterPostFormattersFromService()
  {
    ServiceLoader
        .load(ParameterPostFormatter.class, classLoader)
        .forEach(this::addParameterPostFormatter);
  }
}
