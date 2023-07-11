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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.runtime.ArrayFormatter;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Jeroen Gremmen
 */
class FormatterCacheTest
{
  @Test
  void lookup()
  {
    val cache = new FormatterCache(8);
    val fn = (Function<Class<?>,ParameterFormatter[]>)t -> new ParameterFormatter[0];

    cache.lookup(boolean.class, fn);
    cache.lookup(int.class, fn);
    cache.lookup(long.class, fn);
    cache.lookup(short.class, fn);
    cache.lookup(String.class, fn);
    cache.lookup(Double.class, fn);
    cache.lookup(Map.class, fn);
    cache.lookup(byte[].class, fn);

    // lookup boolean which moves it to head
    // cache: boolean, byte[], Map, Double, String, short, long, int
    assertArrayEquals(new ParameterFormatter[0], cache.lookup(boolean.class, t -> fail()));

    // lookup List, removes int
    // cache: List, boolean, byte[], Map, Double, String, short, long
    cache.lookup(List.class, fn);

    // lookup int, requires call to buildFormatters (moves it to head) and removes long
    // cache: int, List, boolean, byte[], Map, Double, String, short
    val pf = new ParameterFormatter[] { new ArrayFormatter() };
    assertArrayEquals(pf, cache.lookup(int.class, t -> pf));

    // lookup byte[] which moves it to head
    // cache: byte[], int, List, boolean, Map, Double, String, short
    assertArrayEquals(new ParameterFormatter[0], cache.lookup(byte[].class, t -> fail()));

    // lookup Iterator, requires call to buildFormatters (moves it to head) and removes short
    // cache: Iterator, int, List, boolean, byte[], Map, Double, String
    assertArrayEquals(pf, cache.lookup(Iterator.class, t -> pf));

    // lookup boolean which doesn't move it to head (not in lower quarter)
    // cache: Iterator, int, List, boolean, byte[], Map, Double, String
    assertArrayEquals(new ParameterFormatter[0], cache.lookup(boolean.class, t -> fail()));

    // lookup Double which moves it to head (in lower quarter)
    // cache: Double, Iterator, int, List, boolean, byte[], Map, String
    assertArrayEquals(new ParameterFormatter[0], cache.lookup(Double.class, t -> fail()));
  }
}
