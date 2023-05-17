/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.runtime.StringFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("UnstableApiUsage")
public class GenericFormatterService implements FormatterService.WithRegistry
{
  private static final Comparator<Class<?>> CLASS_SORTER =
      (o1, o2) -> o1 == o2 ? 0 : o1.getName().compareTo(o2.getName());

  private static final @NotNull Map<Class<?>,Class<?>> WRAPPER_CLASS_MAP = new HashMap<>();

  private final @NotNull Map<String,NamedParameterFormatter> namedFormatters =
      new ConcurrentHashMap<>();
  private final @NotNull Map<Class<?>,Byte> typeOrderMap = new ConcurrentHashMap<>();
  private final @NotNull Map<Class<?>,ParameterFormatter> typeFormatters =
      new ConcurrentHashMap<>();
  private final @NotNull Map<Class<?>,List<ParameterFormatter>> cachedFormatters =
      synchronizedMap(new FixedSizeCacheMap<>(CLASS_SORTER, 256));


  static
  {
    WRAPPER_CLASS_MAP.put(char.class, Character.class);
    WRAPPER_CLASS_MAP.put(byte.class, Byte.class);
    WRAPPER_CLASS_MAP.put(double.class, Double.class);
    WRAPPER_CLASS_MAP.put(float.class, Float.class);
    WRAPPER_CLASS_MAP.put(int.class, Integer.class);
    WRAPPER_CLASS_MAP.put(long.class, Long.class);
    WRAPPER_CLASS_MAP.put(short.class, Short.class);
  }


  public GenericFormatterService() {
    addFormatterForType(new FormattableType(Object.class), new StringFormatter());
  }


  @Override
  @MustBeInvokedByOverriders
  public void addFormatterForType(@NotNull FormattableType formattableType,
                                  @NotNull ParameterFormatter formatter)
  {
    final Class<?> type =
        requireNonNull(formattableType, "formattableType must not be null").getType();

    typeOrderMap.put(type, formattableType.getOrder());
    typeFormatters.put(type, requireNonNull(formatter, "formatter must not be null"));
    cachedFormatters.clear();
  }


  @Override
  @MustBeInvokedByOverriders
  public void addFormatter(@NotNull ParameterFormatter formatter)
  {
    requireNonNull(formatter, "formatter must not be null");

    if (formatter instanceof NamedParameterFormatter)
    {
      final String format = ((NamedParameterFormatter)formatter).getName();
      if (format.isEmpty())
        throw new IllegalArgumentException("formatter name must not be empty");

      namedFormatters.put(format, (NamedParameterFormatter)formatter);
    }

    formatter.getFormattableTypes()
        .forEach(formattableType -> addFormatterForType(formattableType, formatter));
  }


  @Override
  @MustBeInvokedByOverriders
  public void setFormattableTypeOrder(@NotNull Class<?> type, byte order)
  {
    if (requireNonNull(type, "type must not be null") == Object.class && order != 127)
      throw new IllegalArgumentException("Object type order must be 127");
    else if (order < 0)
      throw new IllegalArgumentException("order must be in range 0..127");

    if (typeFormatters.containsKey(type))
    {
      typeOrderMap.put(type, order);
      cachedFormatters.clear();
    }
  }


  @Override
  public @NotNull List<ParameterFormatter> getFormatters(String format, @NotNull Class<?> type)
  {
    requireNonNull(type, "type must not be null");

    if (format != null)
    {
      final NamedParameterFormatter namedFormatter = namedFormatters.get(format);
      if (namedFormatter != null && namedFormatter.canFormat(type))
        return singletonList(namedFormatter);
    }

    return cachedFormatters.computeIfAbsent(type, this::resolveFormattersForType);
  }


  private @NotNull List<ParameterFormatter> resolveFormattersForType(@NotNull Class<?> type)
  {
    // substitute wrapper type for primitive type if necessary
    if (type.isPrimitive() && !typeOrderMap.containsKey(type))
      type = WRAPPER_CLASS_MAP.get(type);

    final List<FormattableType> formattableTypes = new ArrayList<>(4);
    Byte order;

    // build list with known formattable types
    for(Class<?> t: resolveFormattersForType_collectTypes(type))
      if ((order = typeOrderMap.get(t)) != null)
        formattableTypes.add(new FormattableType(t, order));

    final int typeCount = formattableTypes.size();

    if (typeCount == 0)
      return singletonList(typeFormatters.get(Object.class));
    else if (typeCount == 1)
      return singletonList(typeFormatters.get(formattableTypes.get(0).getType()));
    else
    {
      final FormattableType[] sortedTypes =
          formattableTypes.toArray(new FormattableType[typeCount]);
      Arrays.sort(sortedTypes);

      final ParameterFormatter[] parameterFormatters = new ParameterFormatter[typeCount];
      for(int n = 0; n < typeCount; n++)
        parameterFormatters[n] = typeFormatters.get(sortedTypes[n].getType());

      return asList(parameterFormatters);
    }
  }


  @Contract(pure = true)
  private @NotNull Set<Class<?>> resolveFormattersForType_collectTypes(Class<?> type)
  {
    final Set<Class<?>> collectedTypes = new HashSet<>();

    for(; type != null; type = type.getSuperclass())
    {
      collectedTypes.add(type);
      addInterfaceTypes(type, collectedTypes);
    }

    return collectedTypes;
  }


  @Contract(mutates = "param2")
  private static void addInterfaceTypes(@NotNull Class<?> type,
                                        @NotNull Set<Class<?>> collectedTypes)
  {
    for(final Class<?> interfaceType: type.getInterfaces())
      if (!collectedTypes.contains(interfaceType))
      {
        collectedTypes.add(interfaceType);
        addInterfaceTypes(interfaceType, collectedTypes);
      }
  }
}
