package de.sayayi.lib.message.formatter.runtime.extra;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;


@DisplayName("Annotation formatter")
class AnnotationFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormattableTypes() {
    assertFormatterForType(new AnnotationFormatter(), Annotation.class);
  }


  @Test
  @MyAnnotation(value = 56, name = "Annotation", chars = { 'A', 'b', 'C' })
  void testAnnotation() throws ReflectiveOperationException
  {
    val context = MessageSupportFactory.create(
        createFormatterService(new AnnotationFormatter()), NO_CACHE_INSTANCE);

    val annotation = AnnotationFormatterTest.class
        .getDeclaredMethod("testAnnotation").getAnnotation(MyAnnotation.class);

    assertEquals('@' + getClass().getName() +
        "$MyAnnotation(value=56, name=\"Annotation\", chars={'A', 'b', 'C'})", context
        .message("%{ann}")
        .with("ann", annotation)
        .format());

    assertEquals("AbC", context
        .message("%{ann,annotation:chars}")
        .with("ann", annotation)
        .format());
  }




  @Target(METHOD)
  @Retention(RUNTIME)
  public @interface MyAnnotation
  {
    int value();
    String name();
    char[] chars();
  }
}