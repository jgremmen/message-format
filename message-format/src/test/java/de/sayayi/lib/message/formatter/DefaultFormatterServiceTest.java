/*
 * Copyright 2026 Jeroen Gremmen
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

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

import static java.io.File.pathSeparator;
import static java.lang.ClassLoader.getPlatformClassLoader;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link DefaultFormatterService}: discovery of formatters through the default class loader and through an
 * isolated class loader.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Default formatter service")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class DefaultFormatterServiceTest
{
  @Test
  @DisplayName("Default class loader is the library class loader")
  void testDefaultClassLoader()
  {
    assertSame(DefaultFormatterService.class.getClassLoader(), new DefaultFormatterService().classLoader);
    assertSame(DefaultFormatterService.class.getClassLoader(), new DefaultFormatterService(null, 16).classLoader);

    val classLoader = new URLClassLoader(new URL[0]);
    assertSame(classLoader, new DefaultFormatterService(classLoader, 16).classLoader);
  }


  @Test
  @DisplayName("Formatters are discovered when the library is not visible to the system class loader")
  void testIsolatedClassLoader() throws Exception
  {
    // class path without the test classes, loaded in isolation from the system class loader
    val urls = new ArrayList<URL>();
    for(val entry: System.getProperty("java.class.path").split(pathSeparator))
      if (!isTestEntry(entry))
        urls.add(toURL(entry));

    try(val isolated = new URLClassLoader(urls.toArray(URL[]::new), getPlatformClassLoader())) {
      val serviceClass = isolated.loadClass(DefaultFormatterService.class.getName());
      assertNotSame(DefaultFormatterService.class, serviceClass);

      val service = serviceClass.getConstructor().newInstance();
      val classLoaderField = serviceClass.getDeclaredField("classLoader");
      classLoaderField.setAccessible(true);

      assertSame(isolated, classLoaderField.get(service));
      assertFalse(((Map<?,?>)serviceClass.getMethod("getPostFormatters").invoke(service)).isEmpty());
      assertTrue(((Iterable<?>)serviceClass.getMethod("getParameterConfigNames").invoke(service))
          .iterator().hasNext());
    }
  }


  private static boolean isTestEntry(String entry)
  {
    val path = Path.of(entry).normalize().toString().replace(File.separatorChar, '/');
    return path.contains("/classes/java/test") || path.contains("/resources/test");
  }


  private static URL toURL(String entry)
  {
    try {
      return Path.of(entry).toUri().toURL();
    } catch(MalformedURLException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
