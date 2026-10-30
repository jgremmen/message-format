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
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.time.Duration.ofSeconds;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("Formatter cache")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class FormatterCacheTest
{
  @Test
  @DisplayName("Cache entry reorganization on lookup")
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

    // lookup byte[] which doesn't move it to head (not in lower quarter)
    // cache: int, List, boolean, byte[], Map, Double, String, short
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


  @Test
  @DisplayName("Same class name loaded by different class loaders")
  void lookupSameClassNameDifferentClassLoaders() throws Exception
  {
    val cache = new FormatterCache(8);
    val type1 = new IsolatingClassLoader().loadClass(SameNameType.class.getName());
    val type2 = new IsolatingClassLoader().loadClass(SameNameType.class.getName());
    val type3 = new IsolatingClassLoader().loadClass(SameNameType.class.getName());
    val pf1 = new ParameterFormatter[] { new ArrayFormatter() };
    val pf2 = new ParameterFormatter[] { new ArrayFormatter() };
    val pf3 = new ParameterFormatter[] { new ArrayFormatter() };

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

      val pf = new ParameterFormatter[] { new ArrayFormatter() };
      assertSame(pf, cache.lookup(type1, t -> pf));
    });
  }




  @Test
  @DisplayName("Cached types do not prevent their class loader from being unloaded")
  void lookupDoesNotRetainClassLoader() throws Exception
  {
    val cache = new FormatterCache(8);
    val classLoaderRef = cacheTypeFromIsolatingClassLoader(cache);

    for(var n = 0; n < 50 && classLoaderRef.get() != null; n++)
    {
      System.gc();
      Thread.sleep(10);
    }

    assertNull(classLoaderRef.get(), "class loader of a cached type has not been garbage collected");

    // the cache is still usable and a newly loaded type with the same name gets its own entry
    val pf = new ParameterFormatter[] { new ArrayFormatter() };
    assertSame(pf, cache.lookup(new IsolatingClassLoader().loadClass(SameNameType.class.getName()), t -> pf));
  }


  private static @NotNull WeakReference<ClassLoader> cacheTypeFromIsolatingClassLoader(@NotNull FormatterCache cache)
      throws ClassNotFoundException
  {
    val classLoader = new IsolatingClassLoader();
    val type = classLoader.loadClass(SameNameType.class.getName());
    val pf = new ParameterFormatter[] { new ArrayFormatter() };

    assertSame(pf, cache.lookup(type, t -> pf));
    assertSame(pf, cache.lookup(type, t -> fail()));

    return new WeakReference<>(classLoader);
  }




  /** Type loaded by multiple class loaders, resulting in different classes with the same name. */
  public static final class SameNameType {
  }




  /** Class loader defining its own copy of {@link SameNameType}. */
  private static final class IsolatingClassLoader extends ClassLoader
  {
    private IsolatingClassLoader() {
      super(FormatterCacheTest.class.getClassLoader());
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
      if (!name.equals(SameNameType.class.getName()))
        return super.loadClass(name, resolve);

      synchronized(getClassLoadingLock(name))
      {
        var type = findLoadedClass(name);
        if (type == null)
        {
          try(val in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
            val bytes = requireNonNull(in).readAllBytes();
            type = defineClass(name, bytes, 0, bytes.length);
          } catch(IOException ex) {
            throw new ClassNotFoundException(name, ex);
          }
        }

        return type;
      }
    }
  }
}
