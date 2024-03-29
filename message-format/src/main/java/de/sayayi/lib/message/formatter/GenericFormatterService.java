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

import de.sayayi.lib.message.formatter.ParameterFormatter.DefaultFormatter;
import de.sayayi.lib.message.formatter.named.StringFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.copyOf;
import static java.util.Objects.requireNonNull;


/**
 * Generic formatter service implementation.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0 (renamed in 0.4.1)
 */
@SuppressWarnings("UnstableApiUsage")
public class GenericFormatterService implements FormatterService.WithRegistry
{
  /** default cache size for type to parameter formatters cache */
  public static final int DEFAULT_FORMATTER_CACHE_SIZE = 256;

  private static final @NotNull Map<Class<?>,Class<?>> WRAPPER_CLASS_MAP = new HashMap<>();

  private final @NotNull Map<String,NamedParameterFormatter> namedFormatters =
      new ConcurrentHashMap<>();
  private final @NotNull Map<Class<?>,List<PrioritizedFormatter>> typeFormatters =
      new ConcurrentHashMap<>();
  private final @NotNull FormatterCache formatterCache;


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
    this(DEFAULT_FORMATTER_CACHE_SIZE);
  }


  public GenericFormatterService(int formatterCacheSize)
  {
    formatterCache = new FormatterCache(formatterCacheSize);

    addFormatterForType(new FormattableType(Object.class), new StringFormatter());
  }


  @Override
  @MustBeInvokedByOverriders
  public void addFormatterForType(@NotNull FormattableType formattableType,
                                  @NotNull ParameterFormatter formatter)
  {
    if (new FormattableType(Object.class).equals(formattableType) &&
        !(formatter instanceof DefaultFormatter))
    {
      throw new IllegalArgumentException(
          "formatter associated with Object must implement DefaultFormatter interface");
    }

    final Class<?> type =
        requireNonNull(formattableType, "formattableType must not be null").getType();

    typeFormatters
        .computeIfAbsent(type, t -> new ArrayList<>(4))
        .add(new PrioritizedFormatter(formattableType.getOrder(), formatter));

    formatterCache.clear();
  }


  @Override
  @MustBeInvokedByOverriders
  public void addFormatter(@NotNull ParameterFormatter formatter)
  {
    requireNonNull(formatter, "formatter must not be null");

    if (formatter instanceof NamedParameterFormatter)
    {
      final NamedParameterFormatter namedParameterFormatter = (NamedParameterFormatter)formatter;
      final String format = namedParameterFormatter.getName();

      if (format.isEmpty())
        throw new IllegalArgumentException("formatter name must not be empty");

      namedFormatters.put(format, namedParameterFormatter);
    }

    formatter
        .getFormattableTypes()
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

    final ParameterFormatter[] formatters = formatterCache.lookup(type, t ->
        getFormatters_collectTypes(t)
            .stream()
            .map(typeFormatters::get)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .sorted()
            .map(pf -> pf.formatter)
            .distinct()
            .toArray(ParameterFormatter[]::new));

    return copyOf(formatters, formatters.length, ParameterFormatter[].class);
  }


  @Contract(pure = true)
  private @NotNull Set<Class<?>> getFormatters_collectTypes(@NotNull Class<?> type)
  {
    final Set<Class<?>> collectedTypes = new HashSet<>();

    if (!typeFormatters.containsKey(type))
    {
      // 1. substitute wrapper type for primitive type
      // 2. object arrays != Object[] (eg. String[]) will imply Object[] as well
      if (type.isPrimitive())
        type = WRAPPER_CLASS_MAP.get(type);
      else if (type.isArray() && type.getComponentType() != Object.class)
        collectedTypes.add(Object[].class);  // default array formatter
    }

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
    private final int order;
    private final @NotNull ParameterFormatter formatter;


    private PrioritizedFormatter(int order, @NotNull ParameterFormatter formatter)
    {
      this.order = order;
      this.formatter = formatter;
    }


    @Override
    public int compareTo(@NotNull PrioritizedFormatter o) {
      return Integer.compare(order, o.order);
    }


    @Override
    public boolean equals(Object o)
    {
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
