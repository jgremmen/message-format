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
import de.sayayi.lib.message.formatter.ParameterFormatter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractParameterFormatter implements ParameterFormatter
{
  protected boolean hasMessageFor(Object value, Data parameterData)
  {
    return parameterData instanceof DataMap && value instanceof Serializable &&
        ((DataMap)parameterData).hasMessage((Serializable)value);
  }


  protected String getDataString(Data data) {
    return (data instanceof DataString) ? ((DataString)data).asObject() : null;
  }


  protected String getDataString(String key, Data data)
  {
    if (data instanceof DataString)
      return ((DataString)data).asObject();

    if (data instanceof DataMap && key != null)
    {
      DataMap map = (DataMap)data;

      if (map.hasMessage(key))
      {
        Message message = map.getMessage(key);
        if (!message.hasParameters())
          return message.format(Parameters.EMPTY);
      }
    }

    return null;
  }


  protected String getDataString(String key, Data data, String defaultValue)
  {
    String string = getDataString(key, data);
    return string == null ? defaultValue : string;
  }


  protected String formatNull(@NotNull Parameters parameters, Data data) {
    return StringFormatter.format(null, parameters, data);
  }


  protected String formatEmpty(@NotNull Parameters parameters, Data data) {
    return StringFormatter.format("", parameters, data);
  }
}
