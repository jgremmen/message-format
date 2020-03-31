/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.data;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.data.map.MapKey;
import de.sayayi.lib.message.data.map.MapValue;
import de.sayayi.lib.message.data.map.MapValue.Type;
import de.sayayi.lib.message.data.map.MapValueString;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
public final class DataMap implements Data
{
  private static final long serialVersionUID = 400L;

  @Getter private final Map<MapKey,MapValue> map;


  @Override
  @Contract(pure = true)
  public String toString() {
    return map.toString();
  }


  @NotNull
  @Override
  @Contract(pure = true)
  public Map<MapKey,MapValue> asObject() {
    return Collections.unmodifiableMap(map);
  }


  @Contract(pure = true)
  public MapValue find(Object key, Set<MapKey.Type> keyTypes, Set<MapValue.Type> valueTypes)
  {
    MapValue found = null;

    for(Entry<MapKey,MapValue> entry: map.entrySet())
    {
      final MapKey mapKey = entry.getKey();
      if (mapKey == null)
        continue;

      final MapValue mapValue = entry.getValue();

      if ((keyTypes == null || keyTypes.contains(mapKey.getType()) &&
          (valueTypes == null || valueTypes.contains(mapValue.getType()))))
        switch(mapKey.match(Locale.ROOT, key))
        {
          case LENIENT:
            found = entry.getValue();
            break;

          case EXACT:
            return entry.getValue();
        }
    }

    return found;
  }


  @Contract(pure = true)
  public Message getMessage(Object key, Set<MapKey.Type> keyTypes, boolean includeDefault)
  {
    MapValue mapValue = find(key, keyTypes, MapValue.STRING_MESSAGE_TYPE);

    if (mapValue == null)
    {
      if (!includeDefault)
        return null;

      mapValue = map.get(null);
      return mapValue == null ? null : (Message)map.get(null).asObject();
    }

    if (mapValue.getType() == Type.STRING)
      return ((MapValueString)mapValue).asMessage();

    return (Message)mapValue.asObject();
  }
}
