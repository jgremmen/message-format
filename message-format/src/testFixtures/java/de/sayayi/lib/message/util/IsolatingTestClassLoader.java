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
package de.sayayi.lib.message.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableSet;


/**
 * Class loader defining its own copies of selected classes from the class files of its parent class loader.
 * <p>
 * The copies have the same names as the original classes, but are different classes. All other classes are loaded by
 * the parent class loader.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
public final class IsolatingTestClassLoader extends ClassLoader
{
  private final Set<String> isolatedNames;


  /**
   * Creates a class loader defining its own copies of {@code isolatedTypes}. The parent class loader is the class
   * loader of the first type.
   *
   * @param isolatedTypes  types to define copies of, not empty
   */
  public IsolatingTestClassLoader(@NotNull Class<?>... isolatedTypes)
  {
    super(isolatedTypes[0].getClassLoader());

    isolatedNames = Set
        .of(isolatedTypes)
        .stream()
        .map(Class::getName)
        .collect(toUnmodifiableSet());
  }


  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
  {
    if (!isolatedNames.contains(name))
      return super.loadClass(name, resolve);

    synchronized(getClassLoadingLock(name)) {
      var type = findLoadedClass(name);
      if (type == null)
      {
        try(var in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
          var bytes = requireNonNull(in).readAllBytes();
          type = defineClass(name, bytes, 0, bytes.length);
        } catch(IOException ex) {
          throw new ClassNotFoundException(name, ex);
        }
      }

      return type;
    }
  }
}
