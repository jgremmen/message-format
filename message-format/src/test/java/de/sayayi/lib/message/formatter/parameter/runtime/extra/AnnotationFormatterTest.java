/*
 * Copyright 2025 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.util.IsolatingTestClassLoader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;

import static de.sayayi.lib.message.util.AssertionTestSupport.assertGarbageCollected;
import static de.sayayi.lib.message.util.ConcurrentTestSupport.runConcurrently;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;


/**
 * Tests {@link AnnotationFormatter}: formattable types, formatting of annotation instances and unknown elements,
 * concurrent use, and that annotation types are not retained.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
@DisplayName("Annotation formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class AnnotationFormatterTest extends AbstractFormatterTest
{
  private final MessageSupport messageSupport = createMessageSupport(new AnnotationFormatter());


  @Test
  @DisplayName("Formattable types include Annotation")
  void formattableTypes() {
    assertFormatterForType(new AnnotationFormatter(), Annotation.class);
  }


  @Test
  @MyAnnotation(value = 56, name = "Annotation", chars = { 'A', 'b', 'C' })
  @DisplayName("An annotation is formatted as a whole or by the value of a single element")
  void annotation() throws ReflectiveOperationException
  {
    var annotation = AnnotationFormatterTest.class
        .getDeclaredMethod("annotation").getAnnotation(MyAnnotation.class);

    assertEquals(
        '@' + getClass().getName() + ".MyAnnotation(value=56, name=\"Annotation\", chars={'A', 'b', 'C'})",
        messageSupport
            .message("%{ann}")
            .with("ann", annotation)
            .format());

    assertEquals(
        "AbC",
        messageSupport
            .message("%{ann,annotation:chars}")
            .with("ann", annotation)
            .format());
  }


  @Test
  @DisplayName("Unknown annotation element delegates to the next formatter")
  @MyAnnotation(value = 7, name = "Unknown", chars = {})
  void unknownElement() throws ReflectiveOperationException
  {
    var annotation = AnnotationFormatterTest.class
        .getDeclaredMethod("unknownElement")
        .getAnnotation(MyAnnotation.class);

    // unknown elements are not cached, so the second lookup must give the same result
    for(var n = 0; n < 2; n++)
    {
      assertEquals(
          annotation.toString(),
          messageSupport
              .message("%{ann,annotation:unknown}")
              .with("ann", annotation)
              .format());
    }

    // methods inherited from Annotation are not annotation elements
    assertEquals(
        annotation.toString(),
        messageSupport
            .message("%{ann,annotation:hashCode}")
            .with("ann", annotation)
            .format());

    assertEquals(
        "Unknown",
        messageSupport
            .message("%{ann,annotation:name}")
            .with("ann", annotation)
            .format());
  }


  @Test
  @DisplayName("Concurrent formatting of annotation elements")
  @MyAnnotation(value = 42, name = "Concurrent", chars = { 'x', 'y' })
  void concurrentFormatting() throws Exception
  {
    var annotation = AnnotationFormatterTest.class
        .getDeclaredMethod("concurrentFormatting")
        .getAnnotation(MyAnnotation.class);

    runConcurrently(8, thread -> {
      for(var n = 0; n < 500; n++)
      {
        assertEquals(
            "Concurrent 42 xy",
            messageSupport
                .message("%{ann,annotation:name} %{ann,annotation:value} %{ann,annotation:chars}")
                .with("ann", annotation)
                .format());
      }
    });
  }


  @Test
  @DisplayName("Formatted annotations do not prevent their class loader from being unloaded")
  void annotationTypeNotRetained() throws Exception
  {
    assertGarbageCollected(formatAnnotationFromIsolatingClassLoader(messageSupport),
        "class loader of a formatted annotation has not been garbage collected");
  }


  private static @NotNull WeakReference<ClassLoader> formatAnnotationFromIsolatingClassLoader(
      @NotNull MessageSupport context) throws ClassNotFoundException
  {
    var classLoader = new IsolatingTestClassLoader(IsolatedAnnotation.class, IsolatedAnnotated.class);
    var annotatedType = classLoader.loadClass(IsolatedAnnotated.class.getName());
    var annotation = annotatedType.getDeclaredAnnotations()[0];

    assertNotSame(IsolatedAnnotation.class, annotation.annotationType());
    assertEquals(
        "isolated",
        context
            .message("%{ann,annotation:name}")
            .with("ann", annotation)
            .format());

    return new WeakReference<>(classLoader);
  }




  @Target(METHOD)
  @Retention(RUNTIME)
  public @interface MyAnnotation
  {
    int value();
    String name();
    char[] chars();
  }




  /** Annotation loaded by an isolating class loader. */
  @Target(TYPE)
  @Retention(RUNTIME)
  public @interface IsolatedAnnotation {
    String name();
  }




  /** Type annotated with {@link IsolatedAnnotation}, loaded by an isolating class loader. */
  @IsolatedAnnotation(name = "isolated")
  public static final class IsolatedAnnotated {
  }
}
