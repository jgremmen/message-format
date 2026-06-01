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
package de.sayayi.lib.message.annotation.adopter;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


/**
 * Service provider interface for creating {@link AnnotationAdopter} instances backed by a specific bytecode analysis
 * library. Implementations are discovered via {@link java.util.ServiceLoader} and the provider with the lowest
 * {@linkplain #order() order} whose required classes are {@linkplain #isAvailable() available} on the classpath is
 * selected.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 *
 * @see AnnotationAdopter#getAutoDetected(de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport)
 */
public interface AnnotationAdopterProvider
{
  /**
   * Returns the priority order of this provider. When multiple providers are available, the one with the lowest
   * order value is selected.
   *
   * @return  the priority order of this provider (lower values have higher priority)
   */
  @Contract(pure = true)
  int order();


  /**
   * Returns the set of fully qualified class names that must be present on the classpath for this provider to be
   * usable. These are typically classes from the bytecode analysis library that this provider depends on.
   *
   * @return  set of required class names, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Set<String> getRequiredClassNames();


  /**
   * Checks whether all {@linkplain #getRequiredClassNames() required classes} are available on the classpath.
   *
   * @return  {@code true} if all required classes can be loaded, {@code false} otherwise
   */
  @Contract(pure = true)
  default boolean isAvailable()
  {
    final var classloader = Thread.currentThread().getContextClassLoader();

    try {
      for(final var className: getRequiredClassNames())
        Class.forName(className, false, classloader);

      return true;
    } catch(Exception ignored) {
    }

    return false;
  }


  /**
   * Creates a new {@link AnnotationAdopter} instance using the given message factory and publisher.
   *
   * @param messageFactory  message factory used for parsing message format strings, not {@code null}
   * @param publisher       message publisher used for publishing parsed messages and templates, not {@code null}
   *
   * @return  a new annotation adopter instance, never {@code null}
   */
  @Contract(value = "_, _ -> new", pure = true)
  @NotNull AnnotationAdopter createAnnotationAdopter(@NotNull MessageFactory messageFactory,
                                                     @NotNull MessagePublisher publisher);
}
