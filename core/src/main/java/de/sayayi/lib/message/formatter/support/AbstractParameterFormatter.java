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
import de.sayayi.lib.message.data.map.MapKey;
import de.sayayi.lib.message.data.map.MapKey.Type;
import de.sayayi.lib.message.data.map.MapValue;
import de.sayayi.lib.message.data.map.MapValueString;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.impl.EmptyMessage;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

import static de.sayayi.lib.message.data.map.MapKey.EMPTY_NULL_TYPE;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractParameterFormatter implements ParameterFormatter
{
  private static final EnumSet<MapKey.Type> NAME_TYPE = EnumSet.of(Type.NAME);


  protected MapValue getConfigValue(@NotNull String name, Data data) {
    return data instanceof DataMap ? ((DataMap)data).find(name, NAME_TYPE, null) : null;
  }


  protected String getConfigValueString(@NotNull String name, Data data)
  {
    if (!(data instanceof DataMap))
      return null;

    MapValueString string = (MapValueString)((DataMap)data)
        .find(name, NAME_TYPE, EnumSet.of(MapValue.Type.STRING));

    return string == null ? null : string.asObject();
  }


  protected String getConfigValueString(@NotNull String name, Data data, String defaultValue)
  {
    String value = getConfigValueString(name, data);
    return value == null ? defaultValue : value;
  }


  protected Message getMessage(Object value, EnumSet<MapKey.Type> keyTypes, Data data, boolean notNull)
  {
    Message message = null;

    if (data instanceof DataMap)
      message = ((DataMap)data).getMessage(value, keyTypes, false);

    if (message == null && notNull)
      message = EmptyMessage.INSTANCE;

    return message;
  }


  protected String getDataString(Data data) {
    return getDataString(data, null);
  }


  protected String getDataString(Data data, String defaultValue) {
    return (data instanceof DataString) ? ((DataString)data).asObject() : defaultValue;
  }


  protected String getDataString(String key, Data data)
  {
    if (data instanceof DataString)
      return ((DataString)data).asObject();

    if (data instanceof DataMap && key != null)
    {
      DataMap map = (DataMap)data;

      if (map.hasMessage(key, null, false))
      {
        Message message = map.getMessage(key, null, false);
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
    return getMessage(null, EMPTY_NULL_TYPE, data, true).format(parameters);
  }


  protected String formatEmpty(@NotNull Parameters parameters, Data data) {
    return getMessage("", EMPTY_NULL_TYPE, data, true).format(parameters);
  }


  protected String formatString(String value, @NotNull Parameters parameters, Data data)
  {
    Message msg = getMessage(value, ChoiceFormatter.KEY_TYPES, data, false);
    return msg == null ? value : msg.format(parameters);
  }


  protected String trimNotNull(String s) {
    return s == null ? "" : s.trim();
  }
}
