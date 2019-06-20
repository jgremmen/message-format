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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.ParameterData;
import de.sayayi.lib.message.data.ParameterString;
import de.sayayi.lib.message.formatter.ParameterFormatter;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public class MapFormatter implements ParameterFormatter
{
  @Override
  public String format(Object value, String format, Parameters parameters, ParameterData data)
  {
    final Map<?,?> map = (Map<?,?>)value;
    if (map == null || map.isEmpty())
      return null;

    final ResourceBundle bundle = getBundle("Formatter", parameters.getLocale());
    final String separator = (data instanceof ParameterString) ? ((ParameterString)data).getValue() : "=";
    final StringBuilder s = new StringBuilder();

    for(Entry<?,?> entry: map.entrySet())
    {
      if (s.length() > 0)
        s.append(", ");

      Object key = entry.getKey();
      if (key == value)
        s.append(bundle.getString("thisMap"));
      else if (key != null)
        s.append(parameters.getFormatter(null, key.getClass()).format(key, null, parameters, null));
      else
        s.append("(null)");

      s.append(separator);

      Object val = entry.getValue();
      if (val == value)
        s.append(bundle.getString("thisMap"));
      else if (val != null)
        s.append(parameters.getFormatter(null, val.getClass()).format(val, null, parameters, null));
      else
        s.append("(null)");
    }

    return s.toString();
  }


  @Override
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Map.class);
  }
}
