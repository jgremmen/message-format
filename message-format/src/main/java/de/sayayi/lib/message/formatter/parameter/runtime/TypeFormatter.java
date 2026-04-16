/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.stream.Collectors.joining;


/**
 * Parameter formatter for {@link Type} values (including {@link Class}).
 * <p>
 * This formatter renders a Java type as a human-readable string. The output is controlled by the {@code type}
 * configuration key, which accepts a combination of the following format flags:
 * <ul>
 *   <li>{@code c} &ndash; use the simple class name only</li>
 *   <li>{@code j} &ndash; omit the {@code java.lang.} package prefix</li>
 *   <li>{@code u} &ndash; omit the {@code java.util.} package prefix</li>
 *   <li>{@code v} &ndash; expand type variable bounds</li>
 * </ul>
 * <p>
 * The default format is {@code "ju"}, which strips both the {@code java.lang.} and {@code java.util.} prefixes.
 * The formatter handles generic types, parameterized types, type variables, wildcard types and arrays.
 *
 * @author Jeroen Gremmen
 */
public final class TypeFormatter extends AbstractSingleTypeParameterFormatter<Type>
{
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier("type");

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formats the type using the format flags specified by the {@code type} configuration key.
   */
  @Override
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Type type) {
    return noSpaceText(toString(type, context.getConfigValueString("type").orElse("ju")));
  }


  /**
   * Converts a Java {@link Type} to a formatted string representation using the given format flags.
   *
   * @param type        the type to format, not {@code null}
   * @param typeFormat  a string of format flags ({@code c}, {@code j}, {@code u}, {@code v}), not {@code null}
   *
   * @return  formatted type string, never {@code null}
   */
  @Contract(pure = true)
  public static @NotNull String toString(@NotNull Type type, @NotNull String typeFormat)
  {
    // c = short class
    // j = no java.lang. prefix
    // u = no java.util. prefix

    return switch(type) {
      case Class<?> clazz -> toString_class(clazz, typeFormat);
      case GenericArrayType genericArrayType -> toString(genericArrayType.getGenericComponentType(), typeFormat) + "[]";
      case ParameterizedType parameterizedType -> toString_parameterized(parameterizedType, typeFormat);
      case TypeVariable<?> typeVariable -> toString_typeVariable(typeVariable, typeFormat);
      case WildcardType wildcardType -> toString_wildcard(wildcardType, typeFormat);

      default -> type.toString();
    };
  }


  private static String toString_class(@NotNull Class<?> type, @NotNull String typeFormat)
  {
    final var arraySuffix = new StringBuilder();

    while(type.isArray())
    {
      arraySuffix.append("[]");
      type = type.getComponentType();
    }

    if (typeFormat.indexOf('c') >= 0 || type.isPrimitive())
      return type.getSimpleName() + arraySuffix;

    final var name = type.getName();
    final var formattedClass =
        (typeFormat.indexOf('j') >= 0 && name.startsWith("java.lang.")) ||
        (typeFormat.indexOf('u') >= 0 && name.startsWith("java.util."))
            ? name.substring(10)
            : name;

    return formattedClass.replaceAll("\\$[0-9]*", ".") + arraySuffix;
  }


  private static String toString_parameterized(@NotNull ParameterizedType parameterizedType,
                                               @NotNull String typeFormat)
  {
    final var formattedType = new StringBuilder();
    final var ownerType = parameterizedType.getOwnerType();
    final var rawType = parameterizedType.getRawType();

    withOwnerType: {
      if (ownerType != null)
      {
        formattedType.append(toString(ownerType, typeFormat)).append(".");

        if (ownerType instanceof ParameterizedType && rawType instanceof Class<?> clazz)
        {
          final var ownerRawType = (Class<?>)((ParameterizedType)ownerType).getRawType();
          formattedType.append(clazz.getName().replace(ownerRawType.getName() + "$", ""));
          break withOwnerType;
        }
      }

      formattedType.append(toString(rawType, typeFormat));
    }

    var actualTypeArguments = parameterizedType.getActualTypeArguments();
    if (actualTypeArguments.length > 0)
    {
      formattedType.append(Arrays.stream(actualTypeArguments)
          .map(t -> toString(t, typeFormat))
          .collect(joining(", ", "<", ">")));
    }

    return formattedType.toString();
  }


  private static String toString_typeVariable(@NotNull TypeVariable<?> typeVariable, @NotNull String typeFormat)
  {
    if (typeFormat.indexOf('v') < 0)
      return typeVariable.getName();

    final var formattedTypeVariable = new StringBuilder("<").append(typeVariable.getName());
    final var bounds = typeVariable.getBounds();

    if (bounds.length > 0)
    {
      final var typeFormat0 = typeFormat.replace("T", "");

      formattedTypeVariable.append(" extends ").append(Arrays.stream(bounds)
          .map(t -> toString(t, typeFormat0))
          .collect(joining(" & ")));
    }

    return formattedTypeVariable.append('>').toString();
  }


  private static String toString_wildcard(@NotNull WildcardType wildcardType, @NotNull String typeFormat)
  {
    final var formattedWildcardType = new StringBuilder();
    final var lowerBounds = wildcardType.getLowerBounds();
    var bounds = lowerBounds;

    if (lowerBounds.length > 0)
      formattedWildcardType.append("? super ");
    else
    {
      var upperBounds = wildcardType.getUpperBounds();
      if (upperBounds.length == 0 || upperBounds[0].equals(Object.class))
        return "?";

      bounds = upperBounds;
      formattedWildcardType.append("? extends ");
    }

    formattedWildcardType.append(Arrays.stream(bounds)
        .map(t -> toString(t, typeFormat))
        .collect(joining(" & ")));

    return formattedWildcardType.toString();
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link Type}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Type.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "type"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("type");
  }
}
