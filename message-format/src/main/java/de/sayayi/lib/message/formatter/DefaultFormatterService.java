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
package de.sayayi.lib.message.formatter;

import java.util.Iterator;
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


  public static FormatterService getSharedInstance() {
    return INSTANCE;
  }


  @SuppressWarnings("WeakerAccess")
  public DefaultFormatterService() {
    addDefaultFormatters();
  }


  @SuppressWarnings("WeakerAccess")
  protected void addDefaultFormatters() {
    addFormattersFromService();
  }


  @SuppressWarnings("java:S108")
  protected void addFormattersFromService()
  {
    final ServiceLoader<ParameterFormatter> serviceLoader = ServiceLoader.load(ParameterFormatter.class);
    final Iterator<ParameterFormatter> iterator = serviceLoader.iterator();

    while(iterator.hasNext())
    {
      try {
        addFormatter(iterator.next());
      } catch(ServiceConfigurationError ignore) {
      }
    }
  }
}
