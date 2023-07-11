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
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import de.sayayi.lib.message.part.parameter.value.ConfigValue.Type;
import de.sayayi.lib.message.part.parameter.value.ConfigValueMessage;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.EXACT;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.MISMATCH;
import static de.sayayi.lib.message.part.parameter.value.ConfigValue.STRING_MESSAGE_TYPE;
import static java.util.Collections.unmodifiableSet;


/**
 * This class represents the message parameter configuration map.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0
 */
public final class ParamConfig implements Serializable
{
  private static final long serialVersionUID = 800L;

  private final @NotNull ConfigKey[] keys;
  private final @NotNull ConfigValue[] values;
  private final ConfigValue nullKeyValue;


  /**
   * Create a message parameter config instance with the given {@code map}.
   *
   * @param map  message parameter config map, not {@code null}
   */
  public ParamConfig(@NotNull Map<ConfigKey,ConfigValue> map)
  {
    final int size = map.size();

    keys = new ConfigKey[size];
    values = new ConfigValue[size];

    ConfigValue nullKeyValue = null;
    int n = 0;

    for(final Entry<ConfigKey,ConfigValue> entry: map.entrySet())
    {
      final ConfigValue value = values[n] = entry.getValue();

      if ((keys[n] = entry.getKey()) == null)
        nullKeyValue = value;

      n++;
    }

    this.nullKeyValue = nullKeyValue;
  }


  /**
   * Tells whether the parameter configuration contains any values.
   *
   * @return  {@code true} if the parameter configuration contains at least 1 value,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  public boolean isEmpty() {
    return keys.length == 0;
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
  public boolean hasEntryWithKeyType(@NotNull ConfigKey.Type keyType)
  {
    for(final ConfigKey key: keys)
      if (key != null && key.getType() == keyType)
        return true;

    return false;
  }


  @Contract(pure = true)
  public ConfigValue find(@NotNull MessageAccessor messageAccessor, Object key,
                          @NotNull Locale locale, @NotNull Set<ConfigKey.Type> keyTypes,
                          Set<ConfigValue.Type> valueTypes)
  {
    MatchResult bestMatchResult = MISMATCH;
    ConfigKey configKey;
    ConfigValue bestMatch = null;

    for(int n = 0; n < keys.length; n++)
      if ((configKey = keys[n]) != null)
      {
        final ConfigValue configValue = values[n];

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
          Arrays.stream(keys).anyMatch(mk -> mk != null && keyTypes.contains(mk.getType())))
        configValue = nullKeyValue;

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

    for(ConfigValue configValue: values)
      if (configValue instanceof ConfigValueMessage)
        templateNames.addAll(((ConfigValueMessage)configValue).asObject().getTemplateNames());

    return unmodifiableSet(templateNames);
  }


  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (!(o instanceof ParamConfig))
      return false;

    final ParamConfig that = (ParamConfig)o;

    return Arrays.equals(keys, that.keys) && Arrays.equals(values, that.values);
  }


  @Override
  public int hashCode() {
    return (59 + Arrays.hashCode(keys)) * 59 + Arrays.hashCode(values);
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final StringJoiner s = new StringJoiner(",", "{", "}");

    for(int n = 0; n < keys.length; n++)
    {
      final ConfigKey key = keys[n];
      s.add((key == null ? "(default):" : String.valueOf(key) + ':') + values[n]);
    }

    return s.toString();
  }


  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    final int size = keys.length;

    packStream.writeSmallVar(size);

    for(int n = 0; n < size; n++)
    {
      PackHelper.pack(keys[n], packStream);
      PackHelper.pack(values[n], packStream);
    }
  }


  public static @NotNull ParamConfig unpack(@NotNull PackHelper unpack,
                                            @NotNull PackInputStream packStream) throws IOException
  {
    final Map<ConfigKey,ConfigValue> map = new LinkedHashMap<>();

    for(int n = 0, size = packStream.readSmallVar(); n < size; n++)
      map.put(unpack.unpackMapKey(packStream), unpack.unpackMapValue(packStream));

    return new ParamConfig(map);
  }
}
