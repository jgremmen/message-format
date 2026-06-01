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
package de.sayayi.lib.message.annotation.adopter.provider;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import de.sayayi.lib.message.annotation.adopter.AnnotationAdopter;
import de.sayayi.lib.message.annotation.adopter.AnnotationAdopterProvider;
import de.sayayi.lib.message.exception.MessageAdopterException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import static java.util.Comparator.comparingInt;


/**
 * Factory for creating an {@link AnnotationAdopter} by auto-detecting the best available
 * {@link AnnotationAdopterProvider} on the classpath using the {@link ServiceLoader} mechanism.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
public final class AnnotationAdopterFactory
{
  private AnnotationAdopterFactory() {}


  /**
   * Discovers all available {@link AnnotationAdopterProvider} implementations via {@link ServiceLoader}, selects the
   * one with the lowest {@linkplain AnnotationAdopterProvider#order() order} that is
   * {@linkplain AnnotationAdopterProvider#isAvailable() available}, and uses it to create a new
   * {@link AnnotationAdopter}.
   *
   * @param messageFactory  message factory used for parsing message format strings, not {@code null}
   * @param publisher       message publisher used for publishing parsed messages and templates, not {@code null}
   *
   * @return  a new annotation adopter instance, never {@code null}
   *
   * @throws MessageAdopterException  if no suitable annotation adopter provider is available on the classpath
   */
  @Contract(value = "_, _ -> new", pure = true)
  public static @NotNull AnnotationAdopter getAutoDetected(@NotNull MessageFactory messageFactory,
                                                           @NotNull MessagePublisher publisher)
  {
    return ServiceLoader
        .load(AnnotationAdopterProvider.class)
        .stream()
        .map(Provider::get)
        .filter(AnnotationAdopterProvider::isAvailable)
        .min(comparingInt(AnnotationAdopterProvider::order))
        .orElseThrow(() -> new MessageAdopterException("No available annotation adopter provider found"))
        .createAnnotationAdopter(messageFactory, publisher);
  }
}
