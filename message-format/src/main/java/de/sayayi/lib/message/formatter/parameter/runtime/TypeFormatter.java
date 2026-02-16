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
 * @author Jeroen Gremmen
 */
public final class TypeFormatter extends AbstractSingleTypeParameterFormatter<Type>
{
  @Override
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Type type) {
    return noSpaceText(toString(type, context.getConfigValueString("type").orElse("ju")));
  }


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


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Type.class);
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("type");
  }
}
