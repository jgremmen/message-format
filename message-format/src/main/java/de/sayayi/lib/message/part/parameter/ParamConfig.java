/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.part.parameter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import de.sayayi.lib.message.part.parameter.value.ConfigValue.Type;
import de.sayayi.lib.message.part.parameter.value.ConfigValueMessage;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import de.sayayi.lib.message.util.ImmutableArrayMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.EXACT;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.MISMATCH;
import static de.sayayi.lib.message.part.parameter.value.ConfigValue.STRING_MESSAGE_TYPE;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;


/**
 * This class represents the message parameter configuration map.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0
 */
public final class ParamConfig implements Serializable
{
  private static final long serialVersionUID = 800L;

  /** Parameter configuration map. */
  private final @NotNull Map<ConfigKey,ConfigValue> map;


  /**
   * Create a message parameter config instance with the given {@code map}.
   *
   * @param map  message parameter config map, not {@code null}
   */
  public ParamConfig(@NotNull Map<ConfigKey,ConfigValue> map)
  {
    switch(requireNonNull(map, "map must not be null").size())
    {
      case 0:
        this.map = emptyMap();
        break;

      case 1:
        final Entry<ConfigKey,ConfigValue> entry = map.entrySet().iterator().next();
        this.map = singletonMap(entry.getKey(), entry.getValue());
        break;

      default:
        this.map = new ImmutableArrayMap<>(map);
        break;
    }
  }


  /**
   * Tells whether the parameter configuration contains any values.
   *
   * @return  {@code true} if the parameter configuration contains at least 1 value,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  public boolean isEmpty() {
    return map.isEmpty();
  }


  /**
   * Returns the parameter configuration as a map.
   *
   * @return  unmodifiable map, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Map<ConfigKey,ConfigValue> getMap() {
    return unmodifiableMap(map);
  }


  /**
   * Tells whether the parameter configuration map contains an entry with the given {@code keyType}.
   *
   * @param keyType  entry key type to look for, not {@code null}
   *
   * @return  {@code true} if the map contains an entry with the given key type,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  public boolean hasEntryWithKeyType(@NotNull ConfigKey.Type keyType) {
    return map.keySet().stream().anyMatch(ck -> ck.getType() == keyType);
  }


  @Contract(pure = true)
  public ConfigValue find(@NotNull MessageAccessor messageAccessor, Object key,
                          @NotNull Locale locale, @NotNull Set<ConfigKey.Type> keyTypes,
                          Set<ConfigValue.Type> valueTypes)
  {
    MatchResult bestMatchResult = MISMATCH;
    ConfigKey configKey;
    ConfigValue bestMatch = null;

    for(final Entry<ConfigKey,ConfigValue> entry: map.entrySet())
      if ((configKey = entry.getKey()) != null)
      {
        final ConfigValue configValue = entry.getValue();

        if (keyTypes.contains(configKey.getType()) &&
            (valueTypes == null || valueTypes.contains(configValue.getType())))
        {
          final MatchResult matchResult = configKey.match(messageAccessor, locale, key);

          if (matchResult == EXACT)
            return configValue;

          if (matchResult.compareTo(bestMatchResult) > 0)
          {
            bestMatchResult = matchResult;
            bestMatch = configValue;
          }
        }
      }

    return bestMatch;
  }


  @Contract(pure = true)
  public Message.WithSpaces getMessage(@NotNull MessageAccessor messageAccessor,
                                       Object key, @NotNull Locale locale,
                                       @NotNull Set<ConfigKey.Type> keyTypes,
                                       boolean includeDefault)
  {
    ConfigValue configValue = find(messageAccessor, key, locale, keyTypes, STRING_MESSAGE_TYPE);

    if (configValue == null)
    {
      if (includeDefault &&
          map.keySet().stream().anyMatch(mk -> mk != null && keyTypes.contains(mk.getType())))
        configValue = map.get(null);

      if (configValue == null)
        return null;
    }

    return configValue.getType() == Type.STRING
        ? ((ConfigValueString)configValue).asMessage(messageAccessor.getMessageFactory())
        : (Message.WithSpaces)configValue.asObject();
  }


  /**
   * Returns a set of template names referenced in all message values which are available in
   * the message parameter configuration.
   *
   * @return  unmodifiable set of all referenced template names, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Set<String> getTemplateNames()
  {
    final Set<String> templateNames = new TreeSet<>();

    map.values().forEach(configValue -> {
      if (configValue instanceof ConfigValueMessage)
        templateNames.addAll(((ConfigValueMessage)configValue).asObject().getTemplateNames());
    });

    return unmodifiableSet(templateNames);
  }


  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof ParamConfig && map.equals(((ParamConfig)o).map);
  }


  @Override
  public int hashCode() {
    return 59 + map.hashCode();
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    return map.entrySet()
        .stream()
        .map(kv -> String.valueOf(kv.getKey()) + ':' + kv.getValue())
        .collect(joining(",", "{", "}"));
  }
}
