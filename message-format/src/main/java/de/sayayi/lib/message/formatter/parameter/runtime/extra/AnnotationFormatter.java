/*
 * Copyright 2024 Jeroen Gremmen
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

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Parameter formatter for {@link Annotation} values.
 * <p>
 * This formatter extracts and formats a single annotation element value. The {@code annotation} configuration key
 * specifies the name of the annotation element (method) whose value should be formatted.
 * <p>
 * If the configuration key is absent, empty, or refers to a non-existent element, formatting is delegated to the next
 * available formatter.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class AnnotationFormatter extends AbstractSingleTypeParameterFormatter<Annotation>
{
  private final Map<AnnotationField,Method> annotationFieldMap = new HashMap<>();


  @Override
  protected boolean updateTypedClassifiers(@NotNull ClassifierContext context, @NotNull Annotation value)
  {
    context.addClassifier("annotation");

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Extracts the annotation element value specified by the {@code annotation} configuration key and formats it. If
   * the element does not exist, formatting is delegated to the next formatter.
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Annotation annotation)
  {
    formatByField: {
      var fieldName = context.getConfigValueString("annotation").orElse("");
      if (fieldName.isEmpty())
        break formatByField;

      final var annotationType = annotation.annotationType();
      final var key = new AnnotationField(annotationType, fieldName);

      Method method;

      synchronized(annotationFieldMap) {
        if ((method = annotationFieldMap.get(key)) == null)
        {
          try {
            if ((method = annotationType.getDeclaredMethod(fieldName)).getReturnType() == void.class)
              break formatByField;

            annotationFieldMap.put(key, method);
          } catch(ReflectiveOperationException ex) {
            break formatByField;
          }
        }
      }

      try {
        return context.format(method.invoke(annotation), method.getReturnType());
      } catch(ReflectiveOperationException ignored) {
      }
    }

    return context.delegateToNextFormatter();
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link Annotation}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Annotation.class);
  }

  
  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "annotation"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("annotation");
  }




  private record AnnotationField(@NotNull Class<? extends Annotation> annotationType, @NotNull String field) {
  }
}
