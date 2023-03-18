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

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;


/**
 * Formatter service providing formatters for the most common java types.
 *
 * @author Jeroen Gremmen
 */
public class DefaultFormatterService extends GenericFormatterService
{
  private static final FormatterService INSTANCE = new DefaultFormatterService();

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
  public static FormatterService getSharedInstance() {
    return INSTANCE;
  }


  public DefaultFormatterService() {
    this(null);
  }


  @SuppressWarnings("WeakerAccess")
  public DefaultFormatterService(ClassLoader classLoader)
  {
    this.classLoader = classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;

    addDefaultFormatters();
  }


  /**
   * Adds the default formatters for this service.
   */
  @SuppressWarnings("WeakerAccess")
  protected void addDefaultFormatters() {
    addFormattersFromService();
  }


  /**
   * Adds all parameter formatters on the classpath which are defined as a service.
   */
  protected void addFormattersFromService()
  {
    ServiceLoader.load(ParameterFormatter.class, classLoader).forEach(parameterFormatter -> {
      try {
        addFormatter(parameterFormatter);
      } catch(ServiceConfigurationError ignore) {
      }
    });
  }
}
