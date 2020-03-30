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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.DataString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public final class MapFormatter extends AbstractParameterFormatter
{
  @Override
  @Contract(pure = true)
  public String format(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    final Map<?,?> map = (Map<?,?>)value;
    if (map == null)
      return formatNull(parameters, data);
    if (map.isEmpty())
      return formatEmpty(parameters, data);

    final ResourceBundle bundle = getBundle(getClass().getPackage().getName() + ".Formatter",
        parameters.getLocale());
    final String separator = getSeparator(data);
    final StringBuilder s = new StringBuilder();
    final String nullKey = getDataString("null-key", data, "(null)");
    final String nullValue = getDataString("null-value", data, "(null)");

    for(Entry<?,?> entry: map.entrySet())
    {
      if (s.length() > 0)
        s.append(", ");

      Object key = entry.getKey();
      String keyString;

      if (key == value)
        keyString = bundle.getString("thisMap");
      else if (key != null)
        keyString = trimNotNull(parameters.getFormatter(key.getClass()).format(key, null, parameters, null));
      else
        keyString = nullKey;

      Object val = entry.getValue();
      String valueString;

      if (val == value)
        valueString = bundle.getString("thisMap");
      else if (val != null)
        valueString = trimNotNull(parameters.getFormatter(val.getClass()).format(val, null, parameters, null));
      else
        valueString = nullValue;

      s.append((keyString + separator + valueString).trim());
    }

    return s.toString();
  }


  private String getSeparator(Data data)
  {
    if (hasMessageFor("sep", data))
    {
      Message msg = ((DataMap)data).getMessage("sep", null);

      if (!msg.hasParameters())
      {
        String text = msg.format(Parameters.EMPTY);

        if (text == null)
          text = "";

        if (text.isEmpty() && (msg.isSpaceBefore() || msg.isSpaceAfter()))
          text = " ";
        else if (!text.isEmpty())
        {
          if (msg.isSpaceBefore())
            text = " " + text;
          if (msg.isSpaceAfter())
            text += " ";
        }

        return text;
      }
    }

    if (data instanceof DataString)
      return ((DataString)data).asObject();

    return "=";
  }


  private String trimNotNull(String s) {
    return s == null ? "" : s.trim();
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(Map.class);
  }
}
