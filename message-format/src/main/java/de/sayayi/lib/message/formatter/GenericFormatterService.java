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
import de.sayayi.lib.message.part.MessagePart.Config;
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
 * Generic implementation of the {@link FormatterService.WithRegistry} interface that manages parameter formatters,
 * named formatters and post formatters.
 * <p>
 * This service resolves formatters for a given value type by walking the type hierarchy (superclasses and interfaces)
 * and returning all matching formatters in priority order. Named formatters can be selected explicitly by name in
 * message parameters, and some named formatters are automatically applied when their configuration key is present.
 * <p>
 * A {@link de.sayayi.lib.message.formatter.parameter.named.StringFormatter StringFormatter} is registered as the
 * default fallback formatter for {@link Object}.
 * <p>
 * Formatter lookup results are cached for performance. The cache size can be configured via the constructor.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0 (renamed in 0.4.1)
 */
public non-sealed class GenericFormatterService implements FormatterService.WithRegistry
{
  /** Default cache size for the type-to-formatter lookup cache. */
  public static final int DEFAULT_FORMATTER_CACHE_SIZE = 256;

  /** Maps primitive types and primitive array types to their corresponding wrapper types. */
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
   * Create a generic formatter service with the given {@code formatterCacheSize}.
   * <p>
   * The formatter service provides a default formatter which translates every object into a string using the
   * {@link Object#toString()} method.
   *
   * @param formatterCacheSize  maximum number of type-to-formatter mappings to cache
   */
  public GenericFormatterService(int formatterCacheSize)
  {
    formatterCache = new FormatterCache(formatterCacheSize);

    addFormatterForType(DEFAULT, new StringFormatter());
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formatters registered for {@link Object} must implement the {@link DefaultFormatter} interface. The formatter's
   * parameter configuration names are validated to follow the kebab-case naming convention.
   * <p>
   * Registering a formatter clears the internal formatter lookup cache.
   */
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


  /**
   * {@inheritDoc}
   * <p>
   * If the formatter implements {@link NamedParameterFormatter}, it is also registered by name. Named formatters that
   * support {@linkplain NamedParameterFormatter#autoApplyOnNamedConfigParameter() auto-apply} are additionally
   * registered to be selected automatically when their configuration key is present.
   * <p>
   * Formatter names and parameter configuration names are validated to follow the kebab-case naming convention.
   */
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


  /**
   * Registers a named formatter for automatic application when its configuration keys are present in the parameter
   * configuration.
   *
   * @param namedParameterFormatter  the named formatter to register for auto-apply, not {@code null}
   *
   * @throws FormatterServiceException  if a configuration key conflicts with an already registered auto-apply formatter
   */
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


  /**
   * {@inheritDoc}
   * <p>
   * The post formatter name is validated to follow the kebab-case naming convention. Duplicate registrations are not
   * allowed and will result in a {@link FormatterServiceException}.
   */
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


  /**
   * {@inheritDoc}
   * <p>
   * Resolution order:
   * <ol>
   *   <li>If a {@code format} name is given, the matching named formatter is returned (if it supports the type).</li>
   *   <li>
   *     Named formatters whose configuration key is present in {@code config} and that support the type are added.
   *   </li>
   *   <li>
   *     Type-based formatters are resolved by walking the type hierarchy (superclasses and interfaces) and collected
   *     in priority order.
   *   </li>
   * </ol>
   */
  @Override
  public @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type, Config config)
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


  /**
   * Returns a stream of all types in the class hierarchy of the given {@code type}, including superclasses,
   * interfaces and wrapper types for primitives.
   *
   * @param type  the type to resolve, not {@code null}
   *
   * @return  a stream of all candidate types to match against registered formatters, never {@code null}
   */
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


  /**
   * Recursively collects all interface types implemented by the given {@code type} into {@code collectedTypes}.
   *
   * @param type            the type whose interfaces to collect, not {@code null}
   * @param collectedTypes  the set to add discovered interface types to, not {@code null}
   */
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


  /** {@inheritDoc} */
  @Override
  public @UnmodifiableView @NotNull Map<String,PostFormatter> getPostFormatters() {
    return unmodifiableMap(postFormatters);
  }


  /** {@inheritDoc} */
  @Override
  public @UnmodifiableView @NotNull Set<String> getParameterConfigNames() {
    return unmodifiableSet(parameterConfigNames);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull FormatterService seal() {
    return new SealedFormatterService();
  }




  /**
   * A prioritized wrapper around a {@link ParameterFormatter} that is used for ordering formatters by their
   * registration priority.
   *
   * @param order      the priority order (lower values have higher priority)
   * @param formatter  the wrapped parameter formatter, not {@code null}
   */
  private record PrioritizedFormatter(int order, @NotNull ParameterFormatter formatter)
      implements Comparable<PrioritizedFormatter>
  {
    /** {@inheritDoc} */
    @Override
    public int compareTo(@NotNull PrioritizedFormatter o) {
      return Integer.compare(order, o.order);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull String toString() {
      return "PrioritizedFormatter(order=" + order + ",formatter=" + formatter + ')';
    }
  }




  /**
   * Immutable, sealed view of the enclosing {@link GenericFormatterService}. All queries are delegated to the
   * enclosing service instance. This class does not permit further formatter registrations.
   *
   * @since 0.22.0
   */
  final class SealedFormatterService implements FormatterService
  {
    private SealedFormatterService() {
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type, Config config) {
      return GenericFormatterService.this.getFormatters(format, type, config);
    }


    /** {@inheritDoc} */
    @Override
    public @UnmodifiableView @NotNull Map<String,PostFormatter> getPostFormatters() {
      return GenericFormatterService.this.getPostFormatters();
    }


    /** {@inheritDoc} */
    @Override
    public @UnmodifiableView @NotNull Set<String> getParameterConfigNames() {
      return GenericFormatterService.this.getParameterConfigNames();
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
      return GenericFormatterService.this.toString();
    }
  }
}
