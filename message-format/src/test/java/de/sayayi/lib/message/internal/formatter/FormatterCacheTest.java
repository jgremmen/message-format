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
package de.sayayi.lib.message.internal.formatter;

import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.runtime.ArrayFormatter;
import de.sayayi.lib.message.util.IsolatingTestClassLoader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static de.sayayi.lib.message.util.AssertionTestSupport.assertGarbageCollected;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link FormatterCache}: formatter lookup, separation of equally named classes from different class loaders, and
 * that class loaders are not retained.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Formatter cache")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class FormatterCacheTest
{
  @Test
  @DisplayName("Least recently used entry is evicted on lookup")
  void lookup()
  {
    var cache = new FormatterCache(8);
    var fn = (Function<Class<?>,ParameterFormatter[]>)t -> new ParameterFormatter[0];

    cache.lookup(boolean.class, fn);
    cache.lookup(int.class, fn);
    cache.lookup(long.class, fn);
    cache.lookup(short.class, fn);
    cache.lookup(String.class, fn);
    cache.lookup(Double.class, fn);
    cache.lookup(Map.class, fn);
    cache.lookup(byte[].class, fn);

    // lookup boolean which marks it as recently used
    // least recently used first: int, long, short, String, Double, Map, byte[], boolean
    assertArrayEquals(new ParameterFormatter[0], cache.lookup(boolean.class, t -> fail()));

    // lookup List, evicts int
    // least recently used first: long, short, String, Double, Map, byte[], boolean, List
    cache.lookup(List.class, fn);

    // lookup int, requires call to buildFormatters and evicts long
    // least recently used first: short, String, Double, Map, byte[], boolean, List, int
    var pf = new ParameterFormatter[] { new ArrayFormatter() };
    assertArrayEquals(pf, cache.lookup(int.class, t -> pf));

    // lookup byte[] which marks it as recently used
    // least recently used first: short, String, Double, Map, boolean, List, int/byte[]
    assertArrayEquals(new ParameterFormatter[0], cache.lookup(byte[].class, t -> fail()));

    // lookup Iterator, requires call to buildFormatters and evicts short
    // least recently used first: String, Double, Map, boolean, List, int/byte[], Iterator
    assertArrayEquals(pf, cache.lookup(Iterator.class, t -> pf));

    // lookup boolean which marks it as recently used
    // least recently used first: String, Double, Map, List, int/byte[], Iterator/boolean
    assertArrayEquals(new ParameterFormatter[0], cache.lookup(boolean.class, t -> fail()));

    // lookup Double which marks it as recently used
    // least recently used first: String, Map, List, int/byte[], Iterator/boolean/Double
    assertArrayEquals(new ParameterFormatter[0], cache.lookup(Double.class, t -> fail()));

    // lookup Set, evicts String, but keeps Double
    cache.lookup(Set.class, fn);
    assertArrayEquals(pf, cache.lookup(String.class, t -> pf));
    assertArrayEquals(new ParameterFormatter[0], cache.lookup(Double.class, t -> fail()));
  }


  @Test
  @DisplayName("Formatters built before clear are not cached")
  void lookupDuringClearIsNotCached()
  {
    var cache = new FormatterCache(8);
    var stale = new ParameterFormatter[] { new ArrayFormatter() };

    // the cache is cleared while the formatters are being built
    assertSame(stale, cache.lookup(String.class, t -> {
      cache.clear();
      return stale;
    }));

    var pf = new ParameterFormatter[0];
    assertSame(pf, cache.lookup(String.class, t -> pf));
    assertSame(pf, cache.lookup(String.class, t -> fail()));
  }


  @Test
  @DisplayName("Same class name loaded by different class loaders")
  void lookupSameClassNameDifferentClassLoaders() throws Exception
  {
    var cache = new FormatterCache(8);
    var type1 = new IsolatingTestClassLoader(SameNameType.class).loadClass(SameNameType.class.getName());
    var type2 = new IsolatingTestClassLoader(SameNameType.class).loadClass(SameNameType.class.getName());
    var type3 = new IsolatingTestClassLoader(SameNameType.class).loadClass(SameNameType.class.getName());
    var pf1 = new ParameterFormatter[] { new ArrayFormatter() };
    var pf2 = new ParameterFormatter[] { new ArrayFormatter() };
    var pf3 = new ParameterFormatter[] { new ArrayFormatter() };

    assertNotSame(type1, type2);
    assertEquals(type1.getName(), type2.getName());

    assertTimeoutPreemptively(ofSeconds(5), () -> {
      cache.lookup(String.class, t -> new ParameterFormatter[0]);
      cache.lookup(type1, t -> pf1);
      cache.lookup(Map.class, t -> new ParameterFormatter[0]);
      assertSame(pf2, cache.lookup(type2, t -> pf2));
      assertSame(pf3, cache.lookup(type3, t -> pf3));

      // cached by identity, not by name
      assertSame(pf1, cache.lookup(type1, t -> fail()));
      assertSame(pf2, cache.lookup(type2, t -> fail()));
      assertSame(pf3, cache.lookup(type3, t -> fail()));
      assertSame(pf2, cache.lookup(SameNameType.class, t -> pf2));

      // force eviction of same named types
      for(var type: List.of(int.class, long.class, short.class, byte.class, char.class, float.class, double.class))
        cache.lookup(type, t -> new ParameterFormatter[0]);

      var pf = new ParameterFormatter[] { new ArrayFormatter() };
      assertSame(pf, cache.lookup(type1, t -> pf));
    });
  }


  @Test
  @DisplayName("Cached types do not prevent their class loader from being unloaded")
  void lookupDoesNotRetainClassLoader() throws Exception
  {
    var cache = new FormatterCache(8);

    assertGarbageCollected(cacheTypeFromIsolatingClassLoader(cache),
        "class loader of a cached type has not been garbage collected");

    // the cache is still usable and a newly loaded type with the same name gets its own entry
    var pf = new ParameterFormatter[] { new ArrayFormatter() };
    assertSame(pf, cache.lookup(new IsolatingTestClassLoader(SameNameType.class).loadClass(SameNameType.class.getName()), t -> pf));
  }


  private static @NotNull WeakReference<ClassLoader> cacheTypeFromIsolatingClassLoader(@NotNull FormatterCache cache)
      throws ClassNotFoundException
  {
    var classLoader = new IsolatingTestClassLoader(SameNameType.class);
    var type = classLoader.loadClass(SameNameType.class.getName());
    var pf = new ParameterFormatter[] { new ArrayFormatter() };

    assertSame(pf, cache.lookup(type, t -> pf));
    assertSame(pf, cache.lookup(type, t -> fail()));

    return new WeakReference<>(classLoader);
  }




  /** Type loaded by multiple class loaders, resulting in different classes with the same name. */
  public static final class SameNameType {
  }
}
