/*
 * Copyright 2021 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;


/**
 * @author Jeroen Gremmen
 */
public final class TypeFormatter extends AbstractParameterFormatter
{
  @Override
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return nullText();

    return noSpaceText(toString((Type)value,
        getConfigValueString(messageContext, "type", parameters, data, true, "Cju")));
  }


  static String toString(@NotNull Type type, @NotNull String typeFormat)
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
    final StringBuilder arraySuffix = new StringBuilder();

    while(type.isArray())
    {
      arraySuffix.append("[]");
      type = type.getComponentType();
    }

    if (typeFormat.indexOf('c') >= 0 || type.isPrimitive())
      return type.getSimpleName() + arraySuffix;

    final String name = type.getName();
    final String formattedClass = (typeFormat.indexOf('j') >= 0 && name.startsWith("java.lang.")) ||
                                  (typeFormat.indexOf('u') >= 0 && name.startsWith("java.util."))
        ? name.substring(10)
        : name;

    return formattedClass.replaceAll("\\$[0-9]*", ".") + arraySuffix;
  }


  private static String toString_parameterized(@NotNull ParameterizedType parameterizedType, @NotNull String typeFormat)
  {
    final StringBuilder formattedType = new StringBuilder();
    final Type ownerType = parameterizedType.getOwnerType();
    final Type rawType = parameterizedType.getRawType();

    withOwnerType: {
      if (ownerType != null)
      {
        formattedType.append(toString(ownerType, typeFormat)).append(".");

        if (ownerType instanceof ParameterizedType && rawType instanceof Class)
        {
          final Class<?> ownerRawType = (Class<?>)((ParameterizedType)ownerType).getRawType();
          formattedType.append(((Class<?>)rawType).getName().replace(ownerRawType.getName() + "$", ""));
          break withOwnerType;
        }
      }

      formattedType.append(toString(rawType, typeFormat));
    }

    final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    if (actualTypeArguments != null && actualTypeArguments.length > 0)
    {
      formattedType.append(Arrays.stream(actualTypeArguments)
          .map(t -> toString(t, typeFormat))
          .collect(joining(", ", "<", ">")));
    }

    return formattedType.toString();
  }


  private static String toString_typeVariable(@NotNull TypeVariable<?> typeVariable, @NotNull String typeFormat)
  {
    if (typeFormat.indexOf('T') < 0)
      return typeVariable.getName();

    final StringBuilder formattedTypeVariable = new StringBuilder();
    formattedTypeVariable.append('<').append(typeVariable.getName());

    final Type[] bounds = typeVariable.getBounds();
    if (bounds != null && bounds.length > 0)
    {
      final String typeFormat0 = typeFormat.replace("T", "");

      formattedTypeVariable.append(" extends ").append(Arrays.stream(bounds)
          .map(t -> toString(t, typeFormat0))
          .collect(joining(" & ")));
    }

    return formattedTypeVariable.append('>').toString();
  }


  private static String toString_wildcard(@NotNull WildcardType wildcardType, @NotNull String typeFormat)
  {
    final StringBuilder formattedWildcardType = new StringBuilder();
    final Type[] lowerBounds = wildcardType.getLowerBounds();
    Type[] bounds = lowerBounds;

    if (lowerBounds.length > 0)
      formattedWildcardType.append("? super ");
    else
    {
      Type[] upperBounds = wildcardType.getUpperBounds();
      if (upperBounds.length <= 0 || upperBounds[0].equals(Object.class))
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
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return singleton(Type.class);
  }
}
