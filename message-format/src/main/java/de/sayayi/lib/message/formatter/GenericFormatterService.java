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

import static java.util.Arrays.copyOf;
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
  private final @NotNull Map<Class<?>,List<PrioritizedFormatter>> typeFormatters =
      new ConcurrentHashMap<>();
  private final @NotNull Map<Class<?>,ParameterFormatter[]> cachedFormatters =
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

    typeFormatters.computeIfAbsent(type, t -> new ArrayList<>(4)).add(
        new PrioritizedFormatter(formattableType.getOrder(), formatter));
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
  public @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type)
  {
    requireNonNull(type, "type must not be null");

    if (format != null)
    {
      final NamedParameterFormatter namedFormatter = namedFormatters.get(format);
      if (namedFormatter != null && namedFormatter.canFormat(type))
        return new ParameterFormatter[] { namedFormatter };
    }

    final ParameterFormatter[] formatters =
        cachedFormatters.computeIfAbsent(type, this::getFormatters_resolve);

    return copyOf(formatters, formatters.length, ParameterFormatter[].class);
  }


  @Contract(pure = true)
  private @NotNull ParameterFormatter[] getFormatters_resolve(@NotNull Class<?> type)
  {
    return getFormatters_resolve_collectTypes(type)
        .stream()
        .map(typeFormatters::get)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .sorted()
        .map(pf -> pf.formatter)
        .distinct()
        .toArray(ParameterFormatter[]::new);
  }


  @Contract(pure = true)
  private @NotNull Set<Class<?>> getFormatters_resolve_collectTypes(@NotNull Class<?> type)
  {
    final Set<Class<?>> collectedTypes = new HashSet<>();

    // substitute wrapper type for primitive type if necessary
    if (type.isPrimitive() && !typeFormatters.containsKey(type))
      type = WRAPPER_CLASS_MAP.get(type);

    for(; type != null; type = type.getSuperclass())
    {
      collectedTypes.add(type);
      addInterfaceTypes(type, collectedTypes);
    }

    collectedTypes.add(Object.class);

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




  private static final class PrioritizedFormatter implements Comparable<PrioritizedFormatter>
  {
    private final byte order;
    private final @NotNull ParameterFormatter formatter;


    private PrioritizedFormatter(byte order, @NotNull ParameterFormatter formatter)
    {
      this.order = order;
      this.formatter = formatter;
    }


    @Override
    public int compareTo(@NotNull PrioritizedFormatter o) {
      return Byte.compare(order, o.order);
    }


    @Override
    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      if (!(o instanceof PrioritizedFormatter))
        return false;

      final PrioritizedFormatter that = (PrioritizedFormatter)o;

      return order == that.order && formatter.equals(that.formatter);
    }


    @Override
    public int hashCode() {
      return (59 + order) * 59 + formatter.hashCode();
    }


    @Override
    public String toString() {
      return "PrioritizedFormatter(order=" + order + ",formatter=" + formatter + ')';
    }
  }
}
