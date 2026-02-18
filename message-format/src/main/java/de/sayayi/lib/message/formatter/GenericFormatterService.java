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
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.DefaultFormatter;
import de.sayayi.lib.message.formatter.parameter.named.StringFormatter;
import de.sayayi.lib.message.formatter.post.PostFormatter;
import de.sayayi.lib.message.part.MessagePart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static de.sayayi.lib.message.formatter.FormattableType.DEFAULT;
import static de.sayayi.lib.message.util.MessageUtil.isEmpty;
import static de.sayayi.lib.message.util.MessageUtil.isKebabCaseName;
import static java.util.Arrays.asList;
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

  private final Lock $lock = new ReentrantLock();

  private final @NotNull Map<String, NamedParameterFormatter> namedFormatters = new TreeMap<>();
  private final @NotNull Map<String,NamedParameterFormatter> configNameToNamedFormatterMap = new TreeMap<>();
  private final @NotNull Map<Class<?>,List<PrioritizedFormatter>> typeFormatters = new HashMap<>();
  private final @NotNull Set<String> parameterConfigNames = new TreeSet<>();
  private final @NotNull Map<String,PostFormatter> postFormatters = new HashMap<>();
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

    for(var parameterConfigName: formatter.getParameterConfigNames())
      if (!isKebabCaseName(parameterConfigName))
      {
        final var formatterName = formatter instanceof NamedParameterFormatter namedParameterFormatter
            ? '\'' + namedParameterFormatter.getName() + '\''
            : formatter.getClass().getSimpleName();

        throw new FormatterServiceException("parameter configuration name '" + parameterConfigName +
            "' for formatter " + formatterName + " does not match the kebab case naming convention");
      }
      else
        parameterConfigNames.add(parameterConfigName);
  
    formatterCache.clear();
  }


  @Override
  @MustBeInvokedByOverriders
  public void addFormatter(@NotNull ParameterFormatter formatter)
  {
    requireNonNull(formatter, "formatter must not be null");

    $lock.lock();
    try {
      if (formatter instanceof NamedParameterFormatter namedParameterFormatter)
      {
        final var formatterName = namedParameterFormatter.getName();

        if (isEmpty(formatterName))
          throw new FormatterServiceException("formatter name must not be empty");
        else if (!isKebabCaseName(formatterName))
        {
          throw new FormatterServiceException("formatter name '" + formatterName +
              "' must match the kebab case naming convention");
        }

        namedFormatters.put(formatterName, namedParameterFormatter);

        if (namedParameterFormatter.autoApplyOnNamedConfigParameter())
          addAutoApplyNamedFormatter(namedParameterFormatter);
      }

      for(var formattableType: formatter.getFormattableTypes())
        addFormatterForType(formattableType, formatter);
    } finally {
      $lock.unlock();
    }
  }


  private void addAutoApplyNamedFormatter(@NotNull NamedParameterFormatter namedParameterFormatter)
  {
    for(var parameterConfigName: namedParameterFormatter.getParameterConfigNames())
    {
      final var existingNamedFormatter =
          configNameToNamedFormatterMap.put(parameterConfigName, namedParameterFormatter);

      if (existingNamedFormatter != null && !existingNamedFormatter.equals(namedParameterFormatter))
      {
        throw new FormatterServiceException("parameter config name '" + parameterConfigName +
            "' for formatter '" + namedParameterFormatter.getName() + "' is in conflict with formatter '" +
            existingNamedFormatter.getName() + '\'');
      }
    }
  }


  @Override
  @MustBeInvokedByOverriders
  public void addPostFormatter(@NotNull PostFormatter postFormatter)
  {
    final var postFormatterName = requireNonNull(postFormatter.getName());
    if (!isKebabCaseName(postFormatterName))
    {
      throw new FormatterServiceException("name '" + postFormatterName + "' for post formatter " +
          postFormatter.getClass().getSimpleName() + " does not match the kebab case naming convention");
    }

    if (postFormatters.put(postFormatterName, postFormatter) != null)
      throw new FormatterServiceException("post formatter '" + postFormatter + "' has already been registered");
  }


  @Override
  public @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type,
                                                     MessagePart.Config config)
  {
    requireNonNull(type, "type must not be null");

    $lock.lock();
    try {
      if (format != null)
      {
        var namedFormatter = namedFormatters.get(format);
        if (namedFormatter != null && namedFormatter.canFormat(type))
          return new ParameterFormatter[] { namedFormatter };
      }

      final var formatters = new LinkedHashSet<ParameterFormatter>();

      if (!configNameToNamedFormatterMap.isEmpty())
        for(var parameterConfigName: config.getConfigNames())
        {
          var namedFormatter = configNameToNamedFormatterMap.get(parameterConfigName);
          if (namedFormatter != null && namedFormatter.canFormat(type))
            formatters.add(namedFormatter);
        }

      final var formattersByType = formatterCache.lookup(type, t ->
          streamTypes(t)
              .map(typeFormatters::get)
              .filter(Objects::nonNull)
              .flatMap(Collection::stream)
              .sorted()
              .map(pf -> pf.formatter)
              .distinct()
              .toArray(ParameterFormatter[]::new));

      formatters.addAll(asList(formattersByType));

      return formatters.toArray(new ParameterFormatter[0]);
    } finally {
      $lock.unlock();
    }
  }


  @Contract(pure = true)
  private @NotNull Stream<Class<?>> streamTypes(@NotNull Class<?> type)
  {
    final var collectedTypes = new HashSet<Class<?>>();

    if (!typeFormatters.containsKey(type))
    {
      final var isArray = type.isArray();

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
  public @UnmodifiableView @NotNull Map<String,PostFormatter> getPostFormatters() {
    return unmodifiableMap(postFormatters);
  }


  @Override
  public @UnmodifiableView @NotNull Set<String> getParameterConfigNames() {
    return unmodifiableSet(parameterConfigNames);
  }




  private record PrioritizedFormatter(int order, @NotNull ParameterFormatter formatter)
      implements Comparable<PrioritizedFormatter>
  {
    @Override
    public int compareTo(@NotNull PrioritizedFormatter o) {
      return Integer.compare(order, o.order);
    }


    @Override
    public @NotNull String toString() {
      return "PrioritizedFormatter(order=" + order + ",formatter=" + formatter + ')';
    }
  }
}
