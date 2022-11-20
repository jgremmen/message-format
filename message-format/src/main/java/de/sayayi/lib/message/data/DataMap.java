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
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.map.MapKey;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.data.map.MapValue;
import de.sayayi.lib.message.data.map.MapValue.Type;
import de.sayayi.lib.message.data.map.MapValueMessage;
import de.sayayi.lib.message.data.map.MapValueString;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.EXACT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.MISMATCH;
import static de.sayayi.lib.message.data.map.MapValue.STRING_MESSAGE_TYPE;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toCollection;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
@EqualsAndHashCode(doNotUseGetters = true)
public final class DataMap implements Data
{
  private static final long serialVersionUID = 800L;

  private final @NotNull Map<MapKey,MapValue> map;


  @Contract(pure = true)
  public boolean isEmpty() {
    return map.isEmpty();
  }


  @Override
  @Contract(pure = true)
  public String toString() {
    return map.toString();
  }


  @Override
  @Contract(pure = true)
  public @NotNull Map<MapKey,MapValue> asObject() {
    return unmodifiableMap(map);
  }


  @Contract(pure = true)
  public MapValue find(@NotNull MessageContext messageContext, Object key,
                       @NotNull Parameters parameters, @NotNull Set<MapKey.Type> keyTypes,
                       Set<MapValue.Type> valueTypes)
  {
    MatchResult bestMatchResult = MISMATCH;
    MapValue bestMatch = null;

    for(Entry<MapKey,MapValue> entry: map.entrySet())
    {
      final MapKey mapKey = entry.getKey();
      if (mapKey == null)
        continue;

      final MapValue mapValue = entry.getValue();

      if (keyTypes.contains(mapKey.getType()) && (valueTypes == null ||
          valueTypes.contains(mapValue.getType())))
      {
        final MatchResult matchResult = mapKey.match(messageContext, parameters, key);

        if (matchResult == EXACT)
          return entry.getValue();

        if (matchResult.compareTo(bestMatchResult) > 0)
        {
          bestMatchResult = matchResult;
          bestMatch = entry.getValue();
        }
      }
    }

    return bestMatch;
  }


  @Contract(pure = true)
  public Message.WithSpaces getMessage(@NotNull MessageContext messageContext, Object key,
                                       @NotNull Parameters parameters, @NotNull Set<MapKey.Type> keyTypes,
                                       boolean includeDefault)
  {
    MapValue mapValue = find(messageContext, key, parameters, keyTypes, STRING_MESSAGE_TYPE);

    if (mapValue == null)
    {
      if (includeDefault)
        mapValue = map.get(null);

      if (mapValue == null)
        return null;
    }

    return mapValue.getType() == Type.STRING
        ? ((MapValueString)mapValue).asMessage(messageContext.getMessageFactory())
        : (Message.WithSpaces)mapValue.asObject();
  }


  /**
   * Returns all parameter names occurring in messages in this map.
   *
   * @return  all parameter names, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Set<String> getParameterNames()
  {
    return map.values().stream()
        .filter(mapValue -> mapValue instanceof MapValueMessage)
        .flatMap(mapValue -> ((MapValueMessage)mapValue).asObject().getParameterNames().stream())
        .collect(toCollection(TreeSet::new));
  }
}