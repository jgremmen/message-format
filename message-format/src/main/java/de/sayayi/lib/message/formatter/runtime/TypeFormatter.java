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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
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
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Type type) {
    return noSpaceText(toString(type, context.getConfigValueString("type").orElse("ju")));
  }


  @Contract(pure = true)
  public static @NotNull String toString(@NotNull Type type, @NotNull String typeFormat)
  {
    // c = short class
    // j = no java.lang. prefix
    // u = no java.util. prefix

    if (type instanceof Class)
      return toString_class((Class<?>)type, typeFormat);

    if (type instanceof GenericArrayType)
      return toString(((GenericArrayType)type).getGenericComponentType(), typeFormat) + "[]";

    if (type instanceof ParameterizedType)
      return toString_parameterized((ParameterizedType)type, typeFormat);

    if (type instanceof TypeVariable)
      return toString_typeVariable((TypeVariable<?>)type, typeFormat);

    if (type instanceof WildcardType)
      return toString_wildcard((WildcardType)type, typeFormat);

    return type.toString();
  }


  private static String toString_class(@NotNull Class<?> type, @NotNull String typeFormat)
  {
    var arraySuffix = new StringBuilder();

    while(type.isArray())
    {
      arraySuffix.append("[]");
      type = type.getComponentType();
    }

    if (typeFormat.indexOf('c') >= 0 || type.isPrimitive())
      return type.getSimpleName() + arraySuffix;

    var name = type.getName();
    var formattedClass =
        (typeFormat.indexOf('j') >= 0 && name.startsWith("java.lang.")) ||
        (typeFormat.indexOf('u') >= 0 && name.startsWith("java.util."))
            ? name.substring(10)
            : name;

    return formattedClass.replaceAll("\\$[0-9]*", ".") + arraySuffix;
  }


  private static String toString_parameterized(@NotNull ParameterizedType parameterizedType,
                                               @NotNull String typeFormat)
  {
    var formattedType = new StringBuilder();
    var ownerType = parameterizedType.getOwnerType();
    var rawType = parameterizedType.getRawType();

    withOwnerType: {
      if (ownerType != null)
      {
        formattedType.append(toString(ownerType, typeFormat)).append(".");

        if (ownerType instanceof ParameterizedType && rawType instanceof Class)
        {
          var ownerRawType = (Class<?>)((ParameterizedType)ownerType).getRawType();
          formattedType.append(((Class<?>)rawType).getName()
              .replace(ownerRawType.getName() + "$", ""));
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

    var formattedTypeVariable = new StringBuilder();
    formattedTypeVariable.append('<').append(typeVariable.getName());

    var bounds = typeVariable.getBounds();
    if (bounds.length > 0)
    {
      var typeFormat0 = typeFormat.replace("T", "");

      formattedTypeVariable.append(" extends ").append(Arrays.stream(bounds)
          .map(t -> toString(t, typeFormat0))
          .collect(joining(" & ")));
    }

    return formattedTypeVariable.append('>').toString();
  }


  private static String toString_wildcard(@NotNull WildcardType wildcardType, @NotNull String typeFormat)
  {
    var formattedWildcardType = new StringBuilder();
    var lowerBounds = wildcardType.getLowerBounds();
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
