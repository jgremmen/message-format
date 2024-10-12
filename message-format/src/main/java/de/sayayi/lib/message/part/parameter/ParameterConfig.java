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
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.ComparatorContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.formatter.ParameterFormatter.DefaultFormatter;
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.part.parameter.key.*;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.part.parameter.key.OrderedConfigKeySorter.OrderedConfigKey;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import de.sayayi.lib.message.part.parameter.value.ConfigValue.Type;
import de.sayayi.lib.message.part.parameter.value.ConfigValueMessage;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.Type.*;
import static java.util.Collections.unmodifiableSet;


/**
 * This class represents the message parameter configuration map.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0
 */
public final class ParameterConfig
{
  /** map containing the parameter configuration values keyed by configuration name. */
  private final @NotNull Map<String,ConfigValue> config;

  /** Array containing the sorted mapped message keys (null, empty, bool, number and string). */
  private final @NotNull ConfigKey[] mapKeys;

  /** Array containing the mapped message corresponding to the map key at the same index. */
  private final @NotNull ConfigValue[] mapValues;

  /** Default message. */
  private final ConfigValue defaultValue;

  /** Bitmask for {@link ConfigKey.Type} stating which keys map to a message. */
  private final byte hasKeyType;


  /**
   * Create a message parameter config instance with the given {@code map}.
   *
   * @param map  message parameter config map, not {@code null}
   */
  public ParameterConfig(@NotNull Map<ConfigKey,ConfigValue> map)
  {
    config = new TreeMap<>();

    final List<OrderedConfigKey> mapKeyList = new ArrayList<>();
    ConfigValue mapNullValue = null;
    ConfigKey key;
    int keyTypeMask = 0;

    for(final Entry<ConfigKey,ConfigValue> entry: map.entrySet())
    {
      ConfigKey.Type keyType;

      if ((key = entry.getKey()) == null)
        mapNullValue = entry.getValue();
      else if ((keyType = key.getType()) != NAME)
      {
        mapKeyList.add(new OrderedConfigKey(mapKeyList.size(), key));
        keyTypeMask |= 1 << keyType.ordinal();
      }
      else
        config.put(((ConfigKeyName)key).getName(), entry.getValue());
    }

    mapKeyList.sort(OrderedConfigKeySorter.INSTANCE);

    final int mapLength = mapKeyList.size();
    mapKeys = new ConfigKey[mapLength];
    mapValues = new ConfigValue[mapLength];

    for(int n = 0; n < mapLength; n++)
    {
      mapKeys[n] = mapKeyList.get(n).getConfigKey();
      mapValues[n] = map.get(mapKeys[n]);
    }

    this.defaultValue = mapNullValue;
    this.hasKeyType = (byte)keyTypeMask;
  }


  /**
   * Tells whether the parameter configuration contains any values.
   *
   * @return  {@code true} if the parameter configuration contains at least 1 value,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  public boolean isEmpty() {
    return config.isEmpty() && mapValues.length == 0 && defaultValue == null;
  }


  /**
   * Tells whether the parameter configuration map contains a message entry with the given
   * {@code keyType}.
   *
   * @param keyType  entry key type to look for, not {@code null}
   *
   * @return  {@code true} if the map contains a message with the given key type,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  public boolean hasMessageWithKeyType(@NotNull ConfigKey.Type keyType) {
    return (hasKeyType & (1 << keyType.ordinal())) != 0;
  }


  @Contract(pure = true)
  public ConfigValue getConfigValue(@NotNull String name) {
    return config.get(name);
  }


  @Contract(pure = true)
  public Message.WithSpaces getMessage(@NotNull MessageAccessor messageAccessor, Object key, @NotNull Locale locale,
                                       @NotNull Set<ConfigKey.Type> keyTypes, boolean includeDefault)
  {
    ConfigValue configValue = getMessage_findMappedValue(messageAccessor, locale, key, keyTypes);

    if (configValue == null)
    {
      if (includeDefault && defaultValue != null &&
          Arrays.stream(mapKeys).anyMatch(mk -> keyTypes.contains(mk.getType())))
        configValue = defaultValue;
      else
        return null;
    }

    return configValue.getType() == Type.STRING
        ? ((ConfigValueString)configValue).asMessage(messageAccessor.getMessageFactory())
        : (Message.WithSpaces)configValue.asObject();
  }


  @Contract(pure = true)
  private ConfigValue getMessage_findMappedValue(@NotNull MessageAccessor messageAccessor, @NotNull Locale locale,
                                                 Object value, @NotNull Set<ConfigKey.Type> keyTypes)
  {
    ConfigValue bestMatch = null;

    final ConfigKeyComparatorContext comparatorContext = new ConfigKeyComparatorContext(messageAccessor, locale);
    final ParameterFormatter[] formatters =
        messageAccessor.getFormatters(value == null ? Object.class : value.getClass());

    MatchResult bestMatchResult = MISMATCH;

    for(int n = 0, l = mapKeys.length; n < l; n++)
      if (keyTypes.contains((comparatorContext.configKey = mapKeys[n]).getType()))
      {
        final MatchResult matchResult = findBestMatch(comparatorContext, formatters, value);

        if (MatchResult.compare(matchResult, bestMatchResult) > 0)
        {
          bestMatchResult = matchResult;
          bestMatch = mapValues[n];
        }
      }

    return bestMatch;
  }


  @SuppressWarnings({"rawtypes", "unchecked"})
  private static @NotNull MatchResult findBestMatch(@NotNull ComparatorContext context,
                                                    @NotNull ParameterFormatter[] formatters, Object value)
  {
    final ConfigKey.Type keyType = context.getKeyType();
    MatchResult bestMatchResult = MISMATCH;

    for(int i = 0, l = formatters.length - 1; i <= l; i++)
    {
      final ParameterFormatter formatter = formatters[i];

      if (formatter instanceof ConfigKeyComparator &&
          !(i > 0 && i == l && formatter instanceof DefaultFormatter))
      {
        final MatchResult matchResult = value != null || keyType == NULL || keyType == EMPTY
            ? keyType.compareValueToKey((ConfigKeyComparator)formatter, value, context)
            : MISMATCH;

        if (MatchResult.compare(matchResult, bestMatchResult) > 0)
          bestMatchResult = matchResult;
      }
    }

    return bestMatchResult;
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

    for(final ConfigValue configValue: config.values())
      if (configValue instanceof ConfigValueMessage)
        templateNames.addAll(((ConfigValueMessage)configValue).asObject().getTemplateNames());

    for(final ConfigValue configValue: mapValues)
      if (configValue instanceof ConfigValueMessage)
        templateNames.addAll(((ConfigValueMessage)configValue).asObject().getTemplateNames());

    return unmodifiableSet(templateNames);
  }


  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof ParameterConfig))
      return false;

    final ParameterConfig that = (ParameterConfig)o;

    return
        hasKeyType == that.hasKeyType &&
        config.equals(that.config) &&
        Arrays.equals(mapKeys, that.mapKeys) &&
        Arrays.equals(mapValues, that.mapValues) &&
        Objects.equals(defaultValue, that.defaultValue);
  }


  @Override
  public int hashCode()
  {
    return (((config.hashCode() * 59) + Objects.hashCode(defaultValue)) * 59 +
        Arrays.hashCode(mapKeys)) * 59 + Arrays.hashCode(mapValues);
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final StringJoiner s = new StringJoiner(",", "{", "}");

    // config
    for(final Entry<String,ConfigValue> configEntry: config.entrySet())
      s.add(configEntry.getKey() + '=' + configEntry.getValue());

    // map
    for(int n = 0; n < mapKeys.length; n++)
      s.add(mapKeys[n].toString() + ':' + mapValues[n]);

    if (defaultValue != null)
      s.add(":" + defaultValue);

    return s.toString();
  }


  /**
   * @hidden
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeSmallVar(config.size() + mapKeys.length + (defaultValue == null ? 0 : 1));

    // config
    for(final Entry<String,ConfigValue> configEntry: config.entrySet())
    {
      PackHelper.pack(new ConfigKeyName(configEntry.getKey()), packStream);
      PackHelper.pack(configEntry.getValue(), packStream);
    }

    // map
    for(int n = 0, l = mapKeys.length; n < l; n++)
    {
      PackHelper.pack(mapKeys[n], packStream);
      PackHelper.pack(mapValues[n], packStream);
    }

    if (defaultValue != null)
    {
      PackHelper.pack((ConfigKey)null, packStream);
      PackHelper.pack(defaultValue, packStream);
    }
  }


  /**
   * @hidden
   */
  public static @NotNull ParameterConfig unpack(@NotNull PackHelper unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    final Map<ConfigKey,ConfigValue> map = new LinkedHashMap<>();

    for(int n = 0, size = packStream.readSmallVar(); n < size; n++)
      map.put(unpack.unpackMapKey(packStream), unpack.unpackMapValue(packStream));

    return new ParameterConfig(map);
  }




  private final class ConfigKeyComparatorContext extends AbstractParameterConfigAccessor implements ComparatorContext
  {
    private final Locale locale;
    private ConfigKey configKey;


    private ConfigKeyComparatorContext(@NotNull MessageAccessor messageAccessor, @NotNull Locale locale)
    {
      super(messageAccessor, ParameterConfig.this);

      this.locale = locale;
    }


    @Override
    public @NotNull ConfigKey.CompareType getCompareType() {
      return configKey.getCompareType();
    }


    @Override
    public @NotNull ConfigKey.Type getKeyType() {
      return configKey.getType();
    }


    @Override
    public boolean getBoolKeyValue() {
      return ((ConfigKeyBool)configKey).isBool();
    }


    @Override
    public long getNumberKeyValue() {
      return ((ConfigKeyNumber)configKey).getNumber();
    }


    @Override
    public @NotNull String getStringKeyValue() {
      return ((ConfigKeyString)configKey).getString();
    }


    @Override
    public @NotNull Locale getLocale() {
      return locale;
    }


    @Override
    public @NotNull MatchResult matchForObject(Object value)
    {
      return findBestMatch(this,
          messageAccessor.getFormatters(value == null ? Object.class : value.getClass()), value);
    }


    @Override
    public @NotNull <T> MatchResult matchForObject(@NotNull T value, @NotNull Class<T> valueType) {
      return findBestMatch(this, messageAccessor.getFormatters(valueType), value);
    }
  }
}
