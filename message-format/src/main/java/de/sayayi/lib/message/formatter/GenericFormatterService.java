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
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.Collections.*;


/**
 * @author Jeroen Gremmen
 */
public class GenericFormatterService implements FormatterService.WithRegistry
{
  private static final Comparator<Class<?>> CLASS_SORTER =
      (o1, o2) -> o1 == o2 ? 0 : o1.getName().compareTo(o2.getName());

  private static final @NotNull Map<Class<?>,Class<?>> WRAPPER_CLASS_MAP = new HashMap<>();

  private final @NotNull Map<String,NamedParameterFormatter> namedFormatters = new ConcurrentHashMap<>();
  private final @NotNull Map<Class<?>,Integer> typeOrderMap = new ConcurrentHashMap<>();
  private final @NotNull Map<Class<?>,ParameterFormatter> typeFormatters = new ConcurrentHashMap<>();
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
  public void addFormatterForType(@NotNull FormattableType formattableType, @NotNull ParameterFormatter formatter)
  {
    final Class<?> type = formattableType.getType();

    typeOrderMap.put(type, formattableType.getOrder());
    typeFormatters.put(type, formatter);
    cachedFormatters.clear();
  }


  @Override
  @MustBeInvokedByOverriders
  public void addFormatter(@NotNull ParameterFormatter formatter)
  {
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
  public void setFormattableTypeOrder(@NotNull Class<?> type, @Range(from = 0, to = 127) int order)
  {
    if (type == Object.class && order != 127)
      throw new IllegalArgumentException("Object type order must be 127");

    if (typeFormatters.containsKey(type))
    {
      typeOrderMap.put(type, order);
      cachedFormatters.clear();
    }
  }


  @Override
  public @NotNull List<ParameterFormatter> getFormatters(String format, @NotNull Class<?> type)
  {
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

    final Set<Class<?>> types = new HashSet<>();
    types.add(Object.class);

    while(type != null)
    {
      types.add(type);
      types.addAll(getInterfaceTypes(type));

      type = type.getSuperclass();
    }

    return types
        .stream()
        .map(this::convertToFormattableType)
        .filter(Objects::nonNull)
        .sorted()
        .map(this::convertToParameterFormatter)
        .collect(ParameterFormatterCollector.INSTANCE);
  }


  @Contract(pure = true)
  private FormattableType convertToFormattableType(@NotNull Class<?> type)
  {
    final Integer order = typeOrderMap.get(type);

    return order == null ? null : new FormattableType(type, order);
  }


  @Contract(pure = true)
  private ParameterFormatter convertToParameterFormatter(@NotNull FormattableType formattableType) {
    return typeFormatters.get(formattableType.getType());
  }


  private static @NotNull Set<Class<?>> getInterfaceTypes(@NotNull Class<?> type)
  {
    final Set<Class<?>> interfaceTypes = new LinkedHashSet<>();

    for(final Class<?> interfaceType: type.getInterfaces())
      if (!interfaceTypes.contains(interfaceType))
      {
        interfaceTypes.add(interfaceType);
        interfaceTypes.addAll(getInterfaceTypes(interfaceType));
      }

    return interfaceTypes;
  }




  private enum ParameterFormatterCollector
      implements Collector<ParameterFormatter,ArrayList<ParameterFormatter>,List<ParameterFormatter>>
  {
    INSTANCE;


    @Override
    public Supplier<ArrayList<ParameterFormatter>> supplier() {
      return () -> new ArrayList<>(8);
    }


    @Override
    public BiConsumer<ArrayList<ParameterFormatter>,ParameterFormatter> accumulator() {
      return List::add;
    }


    @Override
    public BinaryOperator<ArrayList<ParameterFormatter>> combiner() {
      return (list1, list2) -> { list1.addAll(list2); return list1; };
    }


    @Override
    public Function<ArrayList<ParameterFormatter>,List<ParameterFormatter>> finisher()
    {
      return list -> {
        if (list.size() == 1)
          return singletonList(list.get(0));

        // expected size < 8; reduce memory footprint
        list.trimToSize();

        return unmodifiableList(list);
      };
    }


    @Override
    public Set<Characteristics> characteristics() {
      return emptySet();
    }
  }
}
