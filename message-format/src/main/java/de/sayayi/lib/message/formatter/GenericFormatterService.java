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

import de.sayayi.lib.message.exception.FormatterServiceException;
import de.sayayi.lib.message.formatter.ParameterFormatter.DefaultFormatter;
import de.sayayi.lib.message.formatter.named.StringFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static de.sayayi.lib.message.formatter.FormattableType.DEFAULT;
import static java.util.Arrays.copyOf;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;


/**
 * Generic formatter service implementation.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0 (renamed in 0.4.1)
 */
public class GenericFormatterService implements FormatterService.WithRegistry
{
  /** default cache size for type to parameter formatters cache */
  public static final int DEFAULT_FORMATTER_CACHE_SIZE = 256;

  private static final @NotNull Map<Class<?>,Class<?>> WRAPPER_CLASS_MAP = new HashMap<>();

  private final @NotNull Map<String,NamedParameterFormatter> namedFormatters = new ConcurrentHashMap<>();
  private final @NotNull Map<Class<?>,List<PrioritizedFormatter>> typeFormatters = new ConcurrentHashMap<>();
  private final @NotNull Set<String> parameterConfigNames = new TreeSet<>();
  private final @NotNull Map<String,ParameterPostFormatter> parameterPostFormatters = new HashMap<>();
  private final @NotNull FormatterCache formatterCache;


  static
  {
    // primitives
    WRAPPER_CLASS_MAP.put(boolean.class, Boolean.class);
    WRAPPER_CLASS_MAP.put(char.class, Character.class);
    WRAPPER_CLASS_MAP.put(byte.class, Byte.class);
    WRAPPER_CLASS_MAP.put(double.class, Double.class);
    WRAPPER_CLASS_MAP.put(float.class, Float.class);
    WRAPPER_CLASS_MAP.put(int.class, Integer.class);
    WRAPPER_CLASS_MAP.put(long.class, Long.class);
    WRAPPER_CLASS_MAP.put(short.class, Short.class);

    // primitive arrays
    WRAPPER_CLASS_MAP.put(boolean[].class, Boolean[].class);
    WRAPPER_CLASS_MAP.put(char[].class, Character[].class);
    WRAPPER_CLASS_MAP.put(byte[].class, Byte[].class);
    WRAPPER_CLASS_MAP.put(double[].class, Double[].class);
    WRAPPER_CLASS_MAP.put(float[].class, Float[].class);
    WRAPPER_CLASS_MAP.put(int[].class, Integer[].class);
    WRAPPER_CLASS_MAP.put(long[].class, Long[].class);
    WRAPPER_CLASS_MAP.put(short[].class, Short[].class);
  }


  /**
   * Create a generic formatter service with a {@link #DEFAULT_FORMATTER_CACHE_SIZE default} size.
   * <p>
   * The formatter service provides a default formatter which translates every object into a string using the
   * {@link Object#toString()} method.
   */
  public GenericFormatterService() {
    this(DEFAULT_FORMATTER_CACHE_SIZE);
  }


  /**
   * Create a generic formatter service with the given {@code formatterCacheSize} size.
   * <p>
   * The formatter service provides a default formatter which translates every object into a string using the
   * {@link Object#toString()} method.
   */
  public GenericFormatterService(int formatterCacheSize)
  {
    formatterCache = new FormatterCache(formatterCacheSize);

    addFormatterForType(DEFAULT, new StringFormatter());
  }


  @Override
  @MustBeInvokedByOverriders
  public void addFormatterForType(@NotNull FormattableType formattableType, @NotNull ParameterFormatter formatter)
  {
    if (formattableType.getType() == Object.class && !(formatter instanceof DefaultFormatter))
      throw new FormatterServiceException("formatter associated with Object must implement DefaultFormatter interface");

    typeFormatters
        .computeIfAbsent(
            requireNonNull(formattableType, "formattableType must not be null").getType(),
            type -> new ArrayList<>(4))
        .add(new PrioritizedFormatter(formattableType.getOrder(), formatter));

    var formatterConfigNames = new TreeSet<>(formatter.getParameterConfigNames());
    parameterConfigNames.addAll(formatterConfigNames);

    formatterConfigNames.retainAll(parameterPostFormatters.keySet());

    switch(formatterConfigNames.size())
    {
      case 0:
        break;

      case 1:
        throw new FormatterServiceException("formatter " + formattableType.getType() +
            " has a parameter configuration name " + toDisplayNameList(formatterConfigNames) +
            " which is in conflict with a registered post formatter");

      default:
        throw new FormatterServiceException("formatter " + formattableType.getType() +
            " has parameter configuration names which are in conflict with registered post formatters " +
            toDisplayNameList(formatterConfigNames));
    }

    formatterCache.clear();
  }


  @Override
  @MustBeInvokedByOverriders
  public void addFormatter(@NotNull ParameterFormatter formatter)
  {
    requireNonNull(formatter, "formatter must not be null");

    if (formatter instanceof NamedParameterFormatter)
    {
      var namedParameterFormatter = (NamedParameterFormatter)formatter;
      var format = namedParameterFormatter.getName();

      //noinspection ConstantValue
      if (format == null || format.isEmpty())
        throw new FormatterServiceException("formatter name must not be empty");

      namedFormatters.put(format, namedParameterFormatter);
    }

    for(var formattableType: formatter.getFormattableTypes())
      addFormatterForType(formattableType, formatter);
  }


  @Override
  @MustBeInvokedByOverriders
  public void addParameterPostFormatter(@NotNull ParameterPostFormatter parameterPostFormatter)
  {
    var parameterConfigName = requireNonNull(parameterPostFormatter.getParameterConfigName());

    parameterPostFormatters.put(
        parameterConfigName,
        requireNonNull(parameterPostFormatter, "parameterPostFormatter must not be null"));

    if (parameterConfigNames.contains(parameterConfigName))
    {
      throw new FormatterServiceException("parameter post formatter '" + parameterConfigName +
          "' is in conflict with a registered parameter formatter");
    }
  }


  @Override
  public @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type)
  {
    requireNonNull(type, "type must not be null");

    if (format != null)
    {
      var namedFormatter = namedFormatters.get(format);
      if (namedFormatter != null && namedFormatter.canFormat(type))
        return new ParameterFormatter[] { namedFormatter };
    }

    var formatters = formatterCache.lookup(type, t ->
        streamTypes(t)
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
  private @NotNull Stream<Class<?>> streamTypes(@NotNull Class<?> type)
  {
    var collectedTypes = new HashSet<Class<?>>();

    if (!typeFormatters.containsKey(type))
    {
      var isArray = type.isArray();

      // if no formatter for this primitive (array) type exists, continue collecting using its wrapper type
      if (type.isPrimitive() || (isArray && type.getComponentType().isPrimitive()))
        type = WRAPPER_CLASS_MAP.get(type);

      // object arrays != Object[] (eg. String[]) will imply Object[] as well
      if (isArray && type.getComponentType() != Object.class)
        collectedTypes.add(Object[].class);  // default array formatter
    }

    for(; type != null; type = type.getSuperclass())
    {
      collectedTypes.add(type);
      addInterfaceTypes(type, collectedTypes);
    }

    collectedTypes.add(Object.class);

    return collectedTypes.stream();
  }


  @Contract(mutates = "param2")
  private static void addInterfaceTypes(@NotNull Class<?> type, @NotNull Set<Class<?>> collectedTypes)
  {
    for(var interfaceType: type.getInterfaces())
      if (!collectedTypes.contains(interfaceType))
      {
        collectedTypes.add(interfaceType);
        addInterfaceTypes(interfaceType, collectedTypes);
      }
  }


  @Override
  public @UnmodifiableView @NotNull Map<String,ParameterPostFormatter> getParameterPostFormatters() {
    return unmodifiableMap(parameterPostFormatters);
  }


  @Override
  public @UnmodifiableView @NotNull Set<String> getParameterConfigNames() {
    return unmodifiableSet(parameterConfigNames);
  }


  @Contract(pure = true)
  protected @NotNull String toDisplayNameList(@NotNull Set<String> configNames)
  {
    var s = new StringBuilder();
    var names = configNames.toArray(String[]::new);

    for(int n = 0, count = names.length; n < count; n++)
    {
      if (n > 0)
        s.append(n == count - 1 ? " and " : ", ");

      s.append('\'').append(names[n]).append('\'');
    }

    return s.toString();
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

      var that = (PrioritizedFormatter)o;

      return order == that.order && formatter.equals(that.formatter);
    }


    @Override
    public int hashCode() {
      return formatter.hashCode() * 31 + order;
    }


    @Override
    public String toString() {
      return "PrioritizedFormatter(order=" + order + ",formatter=" + formatter + ')';
    }
  }
}
