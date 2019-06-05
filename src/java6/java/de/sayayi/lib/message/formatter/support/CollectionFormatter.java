/**
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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public class CollectionFormatter implements ParameterFormatter
{
  @Override
  public String format(Object collection, String format, Parameters parameters, ParameterData data)
  {
    if (collection == null)
      return null;

    final ResourceBundle bundle = getBundle("Formatter", parameters.getLocale());
    final StringBuilder s = new StringBuilder();

    for(Object value: (Collection)collection)
    {
      if (s.length() > 0)
        s.append(", ");

      if (value == collection)
        s.append(bundle.getString("thisCollection"));
      else if (value != null)
        s.append(parameters.getFormatter(format, value.getClass()).format(value, format, parameters, data));
    }

    return s.toString();
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Collection.class);
  }
}
