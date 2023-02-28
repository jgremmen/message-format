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
package de.sayayi.lib.message.parameter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.parameter.key.ConfigKey;
import de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.parameter.value.ConfigValue;
import de.sayayi.lib.message.parameter.value.ConfigValue.Type;
import de.sayayi.lib.message.parameter.value.ConfigValueMessage;
import de.sayayi.lib.message.parameter.value.ConfigValueString;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import static de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult.EXACT;
import static de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult.MISMATCH;
import static de.sayayi.lib.message.parameter.value.ConfigValue.STRING_MESSAGE_TYPE;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toCollection;


/**
 * This class represents the message parameter configuration map.
 *
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
@EqualsAndHashCode(doNotUseGetters = true)
public final class ParamConfig implements Serializable
{
  private static final long serialVersionUID = 800L;

  private final @NotNull Map<ConfigKey, ConfigValue> map;


  @Contract(pure = true)
  public boolean isEmpty() {
    return map.isEmpty();
  }


  @Override
  @Contract(pure = true)
  public String toString() {
    return map.toString();
  }


  /**
   * Returns the parameter configuration as a map.
   *
   * @return  map, never {@code null}
   */
  @Contract(pure = true)
  @Unmodifiable
  public @NotNull Map<ConfigKey,ConfigValue> getMap() {
    return unmodifiableMap(map);
  }


  @Contract(pure = true)
  public ConfigValue find(@NotNull MessageContext messageContext, Object key,
                          @NotNull Parameters parameters, @NotNull Set<ConfigKey.Type> keyTypes,
                          Set<ConfigValue.Type> valueTypes)
  {
    final Locale locale = parameters.getLocale();
    MatchResult bestMatchResult = MISMATCH;
    ConfigKey configKey;
    ConfigValue bestMatch = null;

    for(Entry<ConfigKey,ConfigValue> entry: map.entrySet())
      if ((configKey = entry.getKey()) != null)
      {
        final ConfigValue configValue = entry.getValue();

        if (keyTypes.contains(configKey.getType()) &&
            (valueTypes == null || valueTypes.contains(configValue.getType())))
        {
          final MatchResult matchResult = configKey.match(messageContext, locale, key);

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
  public Message.WithSpaces getMessage(@NotNull MessageContext messageContext, Object key,
                                       @NotNull Parameters parameters, @NotNull Set<ConfigKey.Type> keyTypes,
                                       boolean includeDefault)
  {
    ConfigValue configValue = find(messageContext, key, parameters, keyTypes, STRING_MESSAGE_TYPE);

    if (configValue == null)
    {
      if (includeDefault &&
          map.keySet().stream().anyMatch(mk -> mk != null && keyTypes.contains(mk.getType())))
        configValue = map.get(null);

      if (configValue == null)
        return null;
    }

    return configValue.getType() == Type.STRING
        ? ((ConfigValueString)configValue).asMessage(messageContext.getMessageFactory())
        : (Message.WithSpaces)configValue.asObject();
  }


  /**
   * Returns all parameter names occurring in messages in this map.
   *
   * @return  all parameter names, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Set<String> getParameterNames()
  {
    return map.values().stream()
        .filter(mapValue -> mapValue instanceof ConfigValueMessage)
        .flatMap(mapValue -> ((ConfigValueMessage)mapValue).asObject().getParameterNames().stream())
        .collect(toCollection(TreeSet::new));
  }
}
