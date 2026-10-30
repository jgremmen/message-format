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
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;


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
  @Test
  void testFormattableTypes() {
    assertFormatterForType(new AnnotationFormatter(), Annotation.class);
  }


  @Test
  @MyAnnotation(value = 56, name = "Annotation", chars = { 'A', 'b', 'C' })
  void testAnnotation() throws ReflectiveOperationException
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new AnnotationFormatter()));

    val annotation = AnnotationFormatterTest.class
        .getDeclaredMethod("testAnnotation").getAnnotation(MyAnnotation.class);

    assertEquals('@' + getClass().getName() +
        ".MyAnnotation(value=56, name=\"Annotation\", chars={'A', 'b', 'C'})", context
        .message("%{ann}")
        .with("ann", annotation)
        .format());

    assertEquals("AbC", context
        .message("%{ann,annotation:chars}")
        .with("ann", annotation)
        .format());
  }


  @Test
  @DisplayName("Unknown annotation element delegates to the next formatter")
  @MyAnnotation(value = 7, name = "Unknown", chars = {})
  void testUnknownElement() throws ReflectiveOperationException
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new AnnotationFormatter()));

    val annotation = AnnotationFormatterTest.class
        .getDeclaredMethod("testUnknownElement").getAnnotation(MyAnnotation.class);

    // unknown elements are not cached, so the second lookup must give the same result
    for(var n = 0; n < 2; n++)
    {
      assertEquals(annotation.toString(), context
          .message("%{ann,annotation:unknown}")
          .with("ann", annotation)
          .format());
    }

    // methods inherited from Annotation are not annotation elements
    assertEquals(annotation.toString(), context
        .message("%{ann,annotation:hashCode}")
        .with("ann", annotation)
        .format());

    assertEquals("Unknown", context
        .message("%{ann,annotation:name}")
        .with("ann", annotation)
        .format());
  }


  @Test
  @DisplayName("Concurrent formatting of annotation elements")
  @MyAnnotation(value = 42, name = "Concurrent", chars = { 'x', 'y' })
  void testConcurrentFormatting() throws Exception
  {
    val context = MessageSupportFactory
        .create(createFormatterService(new AnnotationFormatter()));

    val annotation = AnnotationFormatterTest.class
        .getDeclaredMethod("testConcurrentFormatting").getAnnotation(MyAnnotation.class);
    val threads = 8;
    val start = new CountDownLatch(1);
    val tasks = new ArrayList<Callable<Void>>();

    for(var t = 0; t < threads; t++)
    {
      tasks.add(() -> {
        start.await();

        for(var n = 0; n < 500; n++)
        {
          assertEquals("Concurrent 42 xy", context
              .message("%{ann,annotation:name} %{ann,annotation:value} %{ann,annotation:chars}")
              .with("ann", annotation)
              .format());
        }

        return null;
      });
    }

    try(val executor = newFixedThreadPool(threads)) {
      val futures = tasks.stream().map(executor::submit).toList();
      start.countDown();

      for(val future: futures)
        future.get(30, SECONDS);
    }
  }


  @Test
  @DisplayName("Formatted annotations do not prevent their class loader from being unloaded")
  @SuppressWarnings("BusyWait")
  void testAnnotationTypeNotRetained() throws Exception
  {
    val context = MessageSupportFactory.create(createFormatterService(new AnnotationFormatter()));
    val classLoaderRef = formatAnnotationFromIsolatingClassLoader(context);

    for(var n = 0; n < 50 && classLoaderRef.get() != null; n++)
    {
      System.gc();
      Thread.sleep(10);
    }

    assertNull(classLoaderRef.get(), "class loader of a formatted annotation has not been garbage collected");
  }


  private static @NotNull WeakReference<ClassLoader> formatAnnotationFromIsolatingClassLoader(
      @NotNull MessageSupport context) throws ClassNotFoundException
  {
    val classLoader = new IsolatingClassLoader();
    val annotatedType = classLoader.loadClass(IsolatedAnnotated.class.getName());
    val annotation = annotatedType.getDeclaredAnnotations()[0];

    assertNotSame(IsolatedAnnotation.class, annotation.annotationType());
    assertEquals("isolated", context
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




  /** Class loader defining its own copies of {@link IsolatedAnnotation} and {@link IsolatedAnnotated}. */
  private static final class IsolatingClassLoader extends ClassLoader
  {
    private static final Set<String> ISOLATED_NAMES =
        Set.of(IsolatedAnnotation.class.getName(), IsolatedAnnotated.class.getName());


    private IsolatingClassLoader() {
      super(AnnotationFormatterTest.class.getClassLoader());
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
      if (!ISOLATED_NAMES.contains(name))
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
