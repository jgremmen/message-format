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

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.annotation.adopter.AnnotationAdopter;
import de.sayayi.lib.message.annotation.adopter.lib.AsmAnnotationAdopter;
import de.sayayi.lib.message.annotation.adopter.lib.ByteBuddyAnnotationAdopter;
import de.sayayi.lib.message.annotation.adopter.lib.SpringAnnotationAdopter;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;


/**
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("AnnotationAdopterFactory")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AnnotationAdopterFactoryTest
{
  private static MessageSupport.ConfigurableMessageSupport messageSupport;


  @BeforeAll
  static void setUp() {
    messageSupport = MessageSupportFactory.create(new DefaultFormatterService());
  }


  @Test
  @DisplayName("Auto-detect Spring adopter")
  void testAutoDetectedSpring() throws Throwable
  {
    withFilteredClassLoader(
        Set.of(
            "org.objectweb.asm.ClassVisitor",
            "net.bytebuddy.jar.asm.ClassVisitor"),
        () -> assertInstanceOf(SpringAnnotationAdopter.class, AnnotationAdopter.getAutoDetected(messageSupport)));
  }


  @Test
  @DisplayName("Auto-detect ASM adopter")
  void testAutoDetectedAsm() throws Throwable
  {
    withFilteredClassLoader(
        Set.of(
            "org.springframework.asm.SpringAsmInfo",
            "net.bytebuddy.jar.asm.ClassVisitor"),
        () -> assertInstanceOf(AsmAnnotationAdopter.class, AnnotationAdopter.getAutoDetected(messageSupport)));
  }


  @Test
  @DisplayName("Auto-detect Byte-Buddy adopter")
  void testAutoDetectedByteBuddy() throws Throwable
  {
    withFilteredClassLoader(
        Set.of(
            "org.springframework.asm.SpringAsmInfo",
            "org.objectweb.asm.ClassVisitor"),
        () -> assertInstanceOf(ByteBuddyAnnotationAdopter.class, AnnotationAdopter.getAutoDetected(messageSupport)));
  }


  private static void withFilteredClassLoader(@NotNull Set<String> hiddenClasses, @NotNull Executable action)
      throws Throwable
  {
    val currentThread = Thread.currentThread();
    val original = currentThread.getContextClassLoader();

    currentThread.setContextClassLoader(new FilteringClassLoader(original, hiddenClasses));
    try {
      action.execute();
    } finally {
      currentThread.setContextClassLoader(original);
    }
  }




  private static final class FilteringClassLoader extends ClassLoader
  {
    private final Set<String> hiddenClasses;


    FilteringClassLoader(@NotNull ClassLoader parent, @NotNull Set<String> hiddenClasses)
    {
      super(parent);

      this.hiddenClasses = hiddenClasses;
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
      if (hiddenClasses.contains(name))
        throw new ClassNotFoundException(name);

      return super.loadClass(name, resolve);
    }
  }
}
