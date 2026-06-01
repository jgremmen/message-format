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
import de.sayayi.lib.message.annotation.adopter.lib.SpringAnnotationAdopter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


/**
 * {@link AnnotationAdopterProvider} implementation that creates a {@link SpringAnnotationAdopter} backed by the
 * ASM library bundled with <a href="https://spring.io/projects/spring-framework">Spring Framework</a>.
 * <p>
 * This provider requires the Spring Framework on the classpath. It has an order of {@code 100},
 * making it the preferred provider in Spring-based applications.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
public final class SpringAnnotationAdopterProvider implements AnnotationAdopterProvider
{
  @Override
  public int order() {
    return 100;
  }


  @Override
  public @NotNull Set<String> getRequiredClassNames()
  {
    return Set.of(
        "org.springframework.asm.ClassVisitor",
        "org.springframework.asm.AnnotationVisitor",
        "org.springframework.asm.MethodVisitor",
        "org.springframework.asm.SpringAsmInfo");
  }


  @Override
  public @NotNull AnnotationAdopter createAnnotationAdopter(@NotNull MessageFactory messageFactory,
                                                            @NotNull MessagePublisher publisher) {
    return new SpringAnnotationAdopter(messageFactory, publisher);
  }
}
