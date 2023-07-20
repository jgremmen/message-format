/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.util;

import org.jetbrains.annotations.Contract;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.Collections.*;


/**
 * @author Jeroen Gremmen
 * @since 0.8.3
 */
public final class StreamUtil
{
  private StreamUtil() {}


  /**
   * @param <K>  map key type
   * @param <V>  map value type
   *
   * @return  collector finisher function returning an unmodifyable version of the map, never
   *          {@code null}
   *
   * @see Collector#finisher()
   */
  @Contract(pure = true)
  public static <K,V> Function<Map<K,V>,Map<K,V>> unmodifyableMapFinisher()
  {
    return map -> {
      switch(map.size())
      {
        case 0:
          return emptyMap();

        case 1:
          final Entry<K,V> entry = map.entrySet().iterator().next();
          return singletonMap(entry.getKey(), entry.getValue());

        default:
          return unmodifiableMap(map);
      }
    };
  }


  @Contract(pure = true)
  public static <K,V> BinaryOperator<Map<K,V>> foldCombiner() {
    return (map1,map2) -> { map1.putAll(map2); return map1; };
  }
}
