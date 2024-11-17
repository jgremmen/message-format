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
package de.sayayi.lib.message.formatter.runtime.extra;

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class AnnotationFormatter extends AbstractSingleTypeParameterFormatter<Annotation>
{
  private final Map<AnnotationField,Method> annotationFieldMap = new HashMap<>();


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Annotation annotation)
  {
    formatByField: {
      var fieldName = context.getConfigValueString("annotation").orElse("");
      if (fieldName.isEmpty())
        break formatByField;

      var annotationType = annotation.annotationType();
      var key = new AnnotationField(annotationType, fieldName);

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


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Annotation.class);
  }




  private static final class AnnotationField
  {
    private final Class<? extends Annotation> annotationType;
    private final String field;


    public AnnotationField(@NotNull Class<? extends Annotation> annotationType, @NotNull String field)
    {
      this.annotationType = annotationType;
      this.field = field;
    }


    @Override
    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      if (!(o instanceof AnnotationField))
        return false;

      var that = (AnnotationField)o;

      return annotationType == that.annotationType && field.equals(that.field);
    }


    @Override
    public int hashCode() {
      return annotationType.hashCode() * 31 + field.hashCode();
    }
  }
}
