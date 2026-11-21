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
import de.sayayi.lib.message.internal.formatter.FormatterCache;
import de.sayayi.lib.message.internal.formatter.FormatterServiceDelegate;
import de.sayayi.lib.message.internal.formatter.PrioritizedParameterFormatter;
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
import static java.util.Objects.requireNonNull;


/**
 * Generic implementation of the {@link FormatterService.WithRegistry} interface that manages parameter formatters,
 * named formatters and post formatters.
 * <p>
 * This service resolves formatters for a given value type by walking the type hierarchy (superclasses and interfaces)
 * and returning all matching formatters in priority order. Named formatters can be selected explicitly by name in
 * message parameters and some named formatters are automatically applied when their configuration key is present.
 * <p>
 * A {@link de.sayayi.lib.message.formatter.parameter.named.StringFormatter StringFormatter} is registered as the
 * default fallback formatter for {@link Object}.
 * <p>
 * Formatter lookup results are cached for performance. The cache size can be configured via the constructor.
 * <p>
 * This class is thread-safe. The registered formatters are kept in an immutable registry snapshot. Read operations
 * ({@link #getFormatters(String, Class, Config)}, {@link #getPostFormatters()}, {@link #getPostFormatter(String)},
 * {@link #getParameterConfigNames()}) work on the current snapshot without acquiring a lock. Mutating operations
 * ({@link #addFormatter(ParameterFormatter)}, {@link #addFormatterForType(FormattableType, ParameterFormatter)},
 * {@link #addPostFormatter(PostFormatter)}) publish a new snapshot and are serialized by a lock. The methods of the
 * formatters being registered are invoked without holding that lock.
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

  /** Lock serializing all formatter registrations. Lookups do not acquire it. */
  private final Lock writeLock = new ReentrantLock();

  /** Maximum number of type-to-formatter mappings to cache. */
  private final int formatterCacheSize;

  /** Current registry snapshot, replaced on each registration. */
  private volatile @NotNull Registry registry;


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
   * Creates a generic formatter service with a {@link #DEFAULT_FORMATTER_CACHE_SIZE default} size.
   * <p>
   * The formatter service provides a default formatter which translates every object into a string using the
   * {@link Object#toString()} method.
   */
  public GenericFormatterService() {
    this(DEFAULT_FORMATTER_CACHE_SIZE);
  }


  /**
   * Creates a generic formatter service with the given {@code formatterCacheSize}.
   * <p>
   * The formatter service provides a default formatter which translates every object into a string using the
   * {@link Object#toString()} method.
   *
   * @param formatterCacheSize  maximum number of type-to-formatter mappings to cache
   */
  public GenericFormatterService(int formatterCacheSize)
  {
    this.formatterCacheSize = formatterCacheSize;

    registry = new Registry(Map.of(), Map.of(), Map.of(), Set.of(), Map.of(), new FormatterCache(formatterCacheSize));

    addFormatterForType(DEFAULT, new StringFormatter());
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formatters registered for {@link Object} must implement the {@link DefaultFormatter} interface. The formatter's
   * parameter configuration names are validated to follow the kebab-case naming convention. All validation is
   * performed before any internal state is mutated, so a failed registration leaves the formatter service unchanged.
   * <p>
   * The methods of {@code formatter} are invoked before the registration lock is acquired.
   * <p>
   * Registering a formatter clears the internal formatter lookup cache.
   */
  @Override
  @MustBeInvokedByOverriders
  public void addFormatterForType(@NotNull FormattableType formattableType, @NotNull ParameterFormatter formatter)
  {
    requireNonNull(formattableType, "formattableType must not be null");

    final var properties = FormatterProperties
        .of(requireNonNull(formatter, "formatter must not be null"));

    validateFormatterForType(formattableType.getType(), properties);

    writeLock.lock();
    try {
      final var r = registry;
      final var typeFormatters = new HashMap<>(r.typeFormatters);
      final var parameterConfigNames = new TreeSet<>(r.parameterConfigNames);

      registerFormatterForType(typeFormatters, parameterConfigNames, formattableType, properties);

      registry = new Registry(r.namedFormatters, r.configNameToNamedFormatterMap, typeFormatters,
          Set.copyOf(parameterConfigNames), r.postFormatters, new FormatterCache(formatterCacheSize));
    } finally {
      writeLock.unlock();
    }
  }


  /**
   * Validates that a formatter can be registered for {@code type}.
   *
   * @param type        the type the formatter is being registered for, not {@code null}
   * @param properties  properties of the formatter to validate, not {@code null}
   *
   * @throws FormatterServiceException  if the formatter is not eligible for registration
   *
   * @since 0.25.0
   */
  private static void validateFormatterForType(@NotNull Class<?> type, @NotNull FormatterProperties properties)
  {
    if (type == Object.class && !(properties.formatter instanceof DefaultFormatter))
      throw new FormatterServiceException("formatter associated with Object must implement DefaultFormatter interface");

    for(var parameterConfigName: properties.parameterConfigNames)
      if (!isKebabCaseName(parameterConfigName))
      {
        final var formatterName = properties.namedFormatter != null
            ? '\'' + properties.name + '\''
            : properties.formatter.getClass().getSimpleName();

        throw new FormatterServiceException("parameter configuration name '" + parameterConfigName +
            "' for formatter " + formatterName + " does not match the kebab case naming convention");
      }
  }


  /**
   * Registers a formatter for {@code formattableType} in the given registry copies. This method must only be called
   * after {@link #validateFormatterForType(Class, FormatterProperties)} has passed for the same formatter and type.
   * <p>
   * The formatter list for the type is replaced by a new list, as the existing list may be shared with the current
   * registry snapshot.
   *
   * @param typeFormatters        copy of the type based formatters to register the formatter in, not {@code null}
   * @param parameterConfigNames  copy of the parameter configuration names to add the formatter's names to, not
   *                              {@code null}
   * @param formattableType       the formattable type to register the formatter for, not {@code null}
   * @param properties            properties of the formatter to register, not {@code null}
   *
   * @since 0.25.0
   */
  private static void registerFormatterForType(
      @NotNull Map<Class<?>,List<PrioritizedParameterFormatter>> typeFormatters,
      @NotNull Set<String> parameterConfigNames, @NotNull FormattableType formattableType,
      @NotNull FormatterProperties properties)
  {
    final var type = formattableType.getType();
    final var formatters = new ArrayList<>(typeFormatters.getOrDefault(type, List.of()));

    formatters.add(new PrioritizedParameterFormatter(formattableType.getOrder(), properties.formatter));
    typeFormatters.put(type, List.copyOf(formatters));

    parameterConfigNames.addAll(properties.parameterConfigNames);
  }


  /**
   * {@inheritDoc}
   * <p>
   * If the formatter implements {@link NamedParameterFormatter}, it is also registered by name. Named formatters that
   * support {@linkplain NamedParameterFormatter#autoApplyOnNamedConfigParameter() auto-apply} are additionally
   * registered to be selected automatically when their configuration key is present.
   * <p>
   * Formatter names and parameter configuration names are validated to follow the kebab-case naming convention. A named
   * formatter whose name has already been registered is rejected with a {@link FormatterServiceException}, consistent
   * with {@link #addPostFormatter(PostFormatter)}.
   * <p>
   * All validation is performed before any internal state is mutated, so a failed registration leaves the formatter
   * service unchanged.
   * <p>
   * The methods of {@code formatter} and of already registered formatters are invoked without holding the registration
   * lock, so a formatter whose methods synchronize with other threads registering formatters does not deadlock.
   */
  @Override
  @MustBeInvokedByOverriders
  public void addFormatter(@NotNull ParameterFormatter formatter)
  {
    final var properties = FormatterProperties
        .of(requireNonNull(formatter, "formatter must not be null"));
    final var formattableTypes = List.copyOf(formatter.getFormattableTypes());

    for(;;)
    {
      // validate and build the new registry from the current snapshot without holding the lock, as validation invokes
      // equals of already registered formatters
      final var r = registry;
      final var newRegistry = registerFormatter(r, properties, formattableTypes);

      writeLock.lock();
      try {
        // publish the new registry, unless another registration has replaced the snapshot in the meantime
        if (registry == r)
        {
          registry = newRegistry;
          return;
        }
      } finally {
        writeLock.unlock();
      }
    }
  }


  /**
   * Validates a formatter against the registry snapshot {@code r} and returns a new registry with the formatter added.
   *
   * @param registry          registry snapshot, not {@code null}
   * @param properties        properties of the formatter to register, not {@code null}
   * @param formattableTypes  formattable types of the formatter, not {@code null}
   *
   * @return  new registry containing the formatter, never {@code null}
   *
   * @throws FormatterServiceException  if the formatter is not eligible for registration
   *
   * @since 0.25.1
   */
  private @NotNull Registry registerFormatter(@NotNull Registry registry, @NotNull FormatterProperties properties,
                                              @NotNull List<FormattableType> formattableTypes)
  {
    if (properties.namedFormatter != null)
    {
      final var formatterName = properties.name;

      if (isEmpty(formatterName))
        throw new FormatterServiceException("formatter name must not be empty");
      else if (!isKebabCaseName(formatterName))
      {
        throw new FormatterServiceException("formatter name '" + formatterName +
            "' must match the kebab case naming convention");
      }
      else if (registry.namedFormatters.containsKey(formatterName))
        throw new FormatterServiceException("named formatter '" + formatterName + "' has already been registered");

      if (properties.autoApply)
        validateAutoApplyNamedFormatter(registry.configNameToNamedFormatterMap, properties);
    }

    for(var formattableType: formattableTypes)
      validateFormatterForType(formattableType.getType(), properties);

    // all validations passed; commit the mutations to copies of the registry
    var namedFormatters = registry.namedFormatters;
    var configNameToNamedFormatterMap = registry.configNameToNamedFormatterMap;

    if (properties.namedFormatter != null)
    {
      final var newNamedFormatters = new TreeMap<>(namedFormatters);

      newNamedFormatters.put(properties.name, properties.namedFormatter);
      namedFormatters = newNamedFormatters;

      if (properties.autoApply)
      {
        final var newConfigNameToNamedFormatterMap = new TreeMap<>(configNameToNamedFormatterMap);

        registerAutoApplyNamedFormatter(newConfigNameToNamedFormatterMap, properties);
        configNameToNamedFormatterMap = newConfigNameToNamedFormatterMap;
      }
    }

    final var typeFormatters = new HashMap<>(registry.typeFormatters);
    final var parameterConfigNames = new TreeSet<>(registry.parameterConfigNames);

    for(var formattableType: formattableTypes)
      registerFormatterForType(typeFormatters, parameterConfigNames, formattableType, properties);

    return new Registry(namedFormatters, configNameToNamedFormatterMap, typeFormatters,
        Set.copyOf(parameterConfigNames), registry.postFormatters, new FormatterCache(formatterCacheSize));
  }


  /**
   * Validates that a named formatter can be registered for auto-apply.
   *
   * @param configNameToNamedFormatterMap  registered auto-apply named formatters, keyed by configuration name, not
   *                                       {@code null}
   * @param properties                     properties of the named formatter to validate for auto-apply, not
   *                                       {@code null}
   *
   * @throws FormatterServiceException  if a configuration key conflicts with an already registered auto-apply formatter
   *
   * @since 0.25.0
   */
  private static void validateAutoApplyNamedFormatter(
      @NotNull Map<String,NamedParameterFormatter> configNameToNamedFormatterMap,
      @NotNull FormatterProperties properties)
  {
    for(var parameterConfigName: properties.parameterConfigNames)
    {
      final var existingNamedFormatter = configNameToNamedFormatterMap.get(parameterConfigName);

      if (existingNamedFormatter != null && !existingNamedFormatter.equals(properties.namedFormatter))
      {
        throw new FormatterServiceException("parameter config name '" + parameterConfigName +  "' for formatter '" +
            properties.name + "' is in conflict with formatter '" + existingNamedFormatter.getName() + '\'');
      }
    }
  }


  /**
   * Registers a named formatter for automatic application when its configuration keys are present in the parameter
   * configuration. This method must only be called after
   * {@link #validateAutoApplyNamedFormatter(Map, FormatterProperties)} has passed for the same formatter.
   *
   * @param configNameToNamedFormatterMap  copy of the auto-apply named formatters to register the formatter in, not
   *                                       {@code null}
   * @param properties                     properties of the named formatter to register for auto-apply, not
   *                                       {@code null}
   *
   * @since 0.25.0
   */
  private static void registerAutoApplyNamedFormatter(
      @NotNull Map<String,NamedParameterFormatter> configNameToNamedFormatterMap,
      @NotNull FormatterProperties properties)
  {
    for(var parameterConfigName: properties.parameterConfigNames)
      configNameToNamedFormatterMap.put(parameterConfigName, properties.namedFormatter);
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
    final var postFormatterName = requireNonNull(
        requireNonNull(postFormatter, "postFormatter must not be null").getName(),
        () -> "name of post formatter " + postFormatter.getClass().getSimpleName() + " must not be null");

    if (!isKebabCaseName(postFormatterName))
    {
      throw new FormatterServiceException("name '" + postFormatterName + "' for post formatter " +
          postFormatter.getClass().getSimpleName() + " does not match the kebab case naming convention");
    }

    writeLock.lock();
    try {
      final var r = registry;

      if (r.postFormatters.containsKey(postFormatterName))
        throw new FormatterServiceException("post formatter '" + postFormatterName + "' has already been registered");

      final var postFormatters = new HashMap<>(r.postFormatters);
      postFormatters.put(postFormatterName, postFormatter);

      // post formatters do not affect type based formatter lookups, so the formatter cache is kept
      registry = new Registry(r.namedFormatters, r.configNameToNamedFormatterMap, r.typeFormatters,
          r.parameterConfigNames, Map.copyOf(postFormatters), r.formatterCache);
    } finally {
      writeLock.unlock();
    }
  }


  /**
   * {@inheritDoc}
   * <p>
   * Resolution order:
   * <ol>
   *   <li>
   *     If a {@code format} name is given and the matching named formatter supports the type, that named formatter is
   *     returned <em>on its own</em>; no type-based formatters are added behind it. This is by design: an explicit
   *     by-name selection is authoritative, so such a formatter has no next formatter to delegate to via
   *     {@link de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext#delegateToNextFormatter()}.
   *   </li>
   *   <li>
   *     Otherwise, if {@code config} contains a configuration key of an auto-apply named formatter that supports the
   *     type, the first such formatter is returned on its own.
   *   </li>
   *   <li>
   *     Otherwise, type-based formatters are resolved by walking the type hierarchy (superclasses and interfaces) and
   *     collected in priority order.
   *   </li>
   * </ol>
   */
  @Override
  public @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type, Config config)
  {
    requireNonNull(type, "type must not be null");

    // work on a consistent snapshot; a registration published meanwhile comes with its own formatter cache, so
    // formatters built from this snapshot never end up in the cache of a newer snapshot
    final var r = registry;

    // explicit named formatter
    if (format != null)
    {
      var namedFormatter = r.namedFormatters.get(format);
      if (namedFormatter != null && namedFormatter.canFormat(type))
        return new ParameterFormatter[] { namedFormatter };
    }

    // auto-config named formatter
    if (config != null && !r.configNameToNamedFormatterMap.isEmpty())
      for(var parameterConfigName: config.getConfigNames())
      {
        var namedFormatter = r.configNameToNamedFormatterMap.get(parameterConfigName);
        if (namedFormatter != null && namedFormatter.canFormat(type))
          return new ParameterFormatter[] { namedFormatter };
      }

    // type based formatters
    final var typeFormatters = r.typeFormatters;
    final var formattersByType = r.formatterCache.lookup(type, t ->
        streamTypes(typeFormatters, t)
            .map(typeFormatters::get)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .sorted()
            .map(PrioritizedParameterFormatter::formatter)
            .distinct()
            .toArray(ParameterFormatter[]::new));

    return formattersByType.clone();  // defensive copy
  }


  /**
   * Returns a stream of all types in the class hierarchy of the given {@code type}, including superclasses, interfaces
   * and wrapper types for primitives.
   *
   * @param typeFormatters  registered type based formatters, not {@code null}
   * @param type            the type to resolve, not {@code null}
   *
   * @return  a stream of all candidate types to match against registered formatters, never {@code null}
   */
  @Contract(pure = true)
  private static @NotNull Stream<Class<?>> streamTypes(
      @NotNull Map<Class<?>,List<PrioritizedParameterFormatter>> typeFormatters, @NotNull Class<?> type)
  {
    final var collectedTypes = new HashSet<Class<?>>();

    if (!typeFormatters.containsKey(type))
    {
      final var isArray = type.isArray();

      // if no formatter for this primitive (array) type exists, continue collecting using its wrapper type
      if (type.isPrimitive() || (isArray && type.getComponentType().isPrimitive()))
        type = WRAPPER_CLASS_MAP.get(type);

      // object arrays != Object[] (e.g. String[]) will imply Object[] as well
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


  /**
   * {@inheritDoc}
   * <p>
   * This implementation returns an immutable snapshot of the post formatters registered at the time of the call.
   */
  @Override
  public @UnmodifiableView @NotNull Map<String,PostFormatter> getPostFormatters() {
    return registry.postFormatters;
  }


  /**
   * {@inheritDoc}
   * <p>
   * This implementation looks up the post formatter directly, without creating a snapshot of all post formatters.
   */
  @Override
  public PostFormatter getPostFormatter(@NotNull String name) {
    return registry.postFormatters.get(name);
  }


  /**
   * {@inheritDoc}
   * <p>
   * This implementation returns an immutable snapshot of the configuration names known at the time of the call.
   */
  @Override
  public @UnmodifiableView @NotNull Set<String> getParameterConfigNames() {
    return registry.parameterConfigNames;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull FormatterService seal() {
    return new FormatterServiceDelegate(this);
  }




  /**
   * Properties of a parameter formatter to register. They are obtained once, before the registration acquires its lock,
   * so the formatter's methods are never invoked while holding the lock and validation and registration use the same
   * values.
   *
   * @param formatter             formatter to register, not {@code null}
   * @param namedFormatter        {@code formatter} if it is a named formatter, otherwise {@code null}
   * @param name                  name of the named formatter, or {@code null} if it is not a named formatter
   * @param autoApply             {@code true} if the named formatter is applied automatically on its configuration keys
   * @param parameterConfigNames  parameter configuration names of the formatter, not {@code null}
   *
   * @since 0.25.1
   */
  private record FormatterProperties(@NotNull ParameterFormatter formatter, NamedParameterFormatter namedFormatter,
                                     String name, boolean autoApply, @NotNull List<String> parameterConfigNames)
  {
    /**
     * Obtains the properties of {@code formatter} by invoking its methods.
     *
     * @param formatter  formatter to register, not {@code null}
     *
     * @return  formatter properties, never {@code null}
     */
    private static @NotNull FormatterProperties of(@NotNull ParameterFormatter formatter)
    {
      final var parameterConfigNames = List.copyOf(formatter.getParameterConfigNames());

      return formatter instanceof NamedParameterFormatter namedFormatter
          ? new FormatterProperties(formatter, namedFormatter, namedFormatter.getName(),
                namedFormatter.autoApplyOnNamedConfigParameter(), parameterConfigNames)
          : new FormatterProperties(formatter, null, null, false, parameterConfigNames);
    }
  }




  /**
   * Immutable snapshot of all registered formatters. The maps and sets of a published snapshot are never modified; a
   * registration publishes a new snapshot instead.
   * <p>
   * The formatter cache belongs to the snapshot whose type based formatters it caches. A registration that changes the
   * type based formatters therefore publishes a snapshot with a new, empty formatter cache.
   *
   * @param namedFormatters                registered named formatters, keyed by formatter name
   * @param configNameToNamedFormatterMap  auto-apply named formatters, keyed by the parameter configuration names that
   *                                       trigger them
   * @param typeFormatters                 type based formatters with their priority, keyed by the type they have been
   *                                       registered for
   * @param parameterConfigNames           accumulated parameter configuration names of all registered formatters
   * @param postFormatters                 registered post formatters, keyed by post formatter name
   * @param formatterCache                 cache of resolved type based formatters, keyed by value type
   *
   * @since 0.25.1
   */
  private record Registry(@NotNull Map<String,NamedParameterFormatter> namedFormatters,
                          @NotNull Map<String,NamedParameterFormatter> configNameToNamedFormatterMap,
                          @NotNull Map<Class<?>,List<PrioritizedParameterFormatter>> typeFormatters,
                          @NotNull Set<String> parameterConfigNames,
                          @NotNull Map<String,PostFormatter> postFormatters,
                          @NotNull FormatterCache formatterCache) {
  }
}
