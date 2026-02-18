/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.internal.part.map;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ComparatorContext;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.DefaultFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.internal.part.config.BaseConfigAccessor;
import de.sayayi.lib.message.internal.part.map.key.MapKeyBool;
import de.sayayi.lib.message.internal.part.map.key.MapKeyNumber;
import de.sayayi.lib.message.internal.part.map.key.MapKeyString;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.TypedValue;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.*;

import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.MapKey.Type.EMPTY;
import static de.sayayi.lib.message.part.MapKey.Type.NULL;
import static java.util.Collections.unmodifiableSet;


/**
 * This class represents the message part map.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public final class MessagePartMap implements MessagePart.Map
{
  public static final MessagePartMap EMPTY_MAP = new MessagePartMap(Map.of());


  /** Array containing the sorted mapped message keys (null, empty, bool, number and string). */
  private final @NotNull MapKey[] mapKeys;

  /** Array containing the mapped message corresponding to the map key at the same index. */
  private final @NotNull TypedValue<?>[] mapValues;

  /** Default message. */
  private final TypedValue<?> defaultValue;

  /** Bitmask for {@link MapKey.Type} stating which keys map to a message. */
  private final byte hasKeyType;


  /**
   * Create a message parameter config instance with the given {@code map}.
   *
   * @param map  message parameter config map, not {@code null}
   */
  public MessagePartMap(@NotNull Map<MapKey,TypedValue<?>> map)
  {
    final var mapKeyList = new ArrayList<OrderedConfigKey>();
    TypedValue<?> mapNullValue = null;
    MapKey key;
    var keyTypeMask = 0;

    for(var entry: map.entrySet())
    {
      if ((key = entry.getKey()) == null)
        mapNullValue = entry.getValue();
      else
      {
        mapKeyList.add(new OrderedConfigKey(mapKeyList.size(), key));
        keyTypeMask |= 1 << key.getType().ordinal();
      }
    }

    mapKeyList.trimToSize();
    mapKeyList.sort(OrderedConfigKey.SORTER);

    final var mapLength = mapKeyList.size();
    mapKeys = new MapKey[mapLength];
    mapValues = new TypedValue[mapLength];

    for(var n = 0; n < mapLength; n++)
    {
      mapKeys[n] = mapKeyList.get(n).mapKey();
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
  @Override
  @Contract(pure = true)
  public boolean isEmpty() {
    return mapValues.length == 0 && defaultValue == null;
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
  @Override
  @Contract(pure = true)
  public boolean hasMessageWithKeyType(@NotNull MapKey.Type keyType) {
    return (hasKeyType & (1 << keyType.ordinal())) != 0;
  }


  /**
   * Returns the default value from the parameter configuration map.

   * @return  default configuration value or {@code null} if no default value has been defined
   *
   * @since 0.20.0
   */
  @Override
  @Contract(pure = true)
  public TypedValue<?> getDefaultValue() {
    return defaultValue;
  }


  @Override
  @Contract(pure = true)
  public Message.WithSpaces getMessage(@NotNull MessageAccessor messageAccessor, Object key, @NotNull Locale locale,
                                       @NotNull Set<MapKey.Type> keyTypes, boolean includeDefault,
                                       MessagePart.Config config)
  {
    var configValue = findMappedValue(messageAccessor, locale, key, keyTypes, config);
    if (configValue == null)
    {
      if (includeDefault && defaultValue != null &&
          Arrays.stream(mapKeys).anyMatch(mk -> keyTypes.contains(mk.getType())))
        configValue = defaultValue;
      else
        return null;
    }

    return configValue instanceof TypedValue.StringValue stringValue
        ? stringValue.asMessage(messageAccessor.getMessageFactory())
        : (Message.WithSpaces)configValue.asObject();
  }


  @Contract(pure = true)
  private TypedValue<?> findMappedValue(@NotNull MessageAccessor messageAccessor, @NotNull Locale locale,
                                        Object value, @NotNull Set<MapKey.Type> keyTypes, MessagePart.Config config)
  {
    TypedValue<?> bestMatch = null;

    final var comparatorContext = new ConfigKeyComparatorContext(messageAccessor, locale, config);
    final var formatters = messageAccessor
        .getFormatters(value == null ? Object.class : value.getClass(), config);

    MatchResult bestMatchResult = MISMATCH;

    for(int n = 0, l = mapKeys.length; n < l; n++)
      if (keyTypes.contains((comparatorContext.mapKey = mapKeys[n]).getType()))
      {
        var matchResult = findBestMatch(comparatorContext, formatters, value);

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
    var keyType = context.getKeyType();
    MatchResult bestMatchResult = MISMATCH;

    for(int i = 0, l = formatters.length - 1; i <= l; i++)
    {
      var formatter = formatters[i];
      if (formatter instanceof MapKeyComparator &&
          !(i > 0 && i == l && formatter instanceof DefaultFormatter))
      {
        var matchResult = value != null || keyType == NULL || keyType == EMPTY
            ? keyType.compareValueToKey((MapKeyComparator)formatter, value, context)
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
  @Override
  @Contract(pure = true)
  @Unmodifiable
  public @NotNull Set<String> getTemplateNames()
  {
    var templateNames = new TreeSet<String>();

    for(var configValue: mapValues)
      if (configValue instanceof TypedValueMessage)
        templateNames.addAll(((TypedValueMessage)configValue).asObject().getTemplateNames());

    return unmodifiableSet(templateNames);
  }


  @Override
  public boolean equals(Object o)
  {
    return o instanceof MessagePartMap that &&
        hasKeyType == that.hasKeyType &&
        Arrays.equals(mapKeys, that.mapKeys) &&
        Arrays.equals(mapValues, that.mapValues) &&
        Objects.equals(defaultValue, that.defaultValue);
  }


  @Override
  public int hashCode() {
    return (Objects.hashCode(defaultValue) * 59 + Arrays.hashCode(mapKeys)) * 59 + Arrays.hashCode(mapValues);
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final var s = new StringJoiner(",", "{", "}");

    // map
    for(var n = 0; n < mapKeys.length; n++)
      s.add(mapKeys[n].toString() + ':' + mapValues[n]);

    if (defaultValue != null)
      s.add(":" + defaultValue);

    return s.toString();
  }


  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeSmallVar(mapKeys.length);

    for(int n = 0, l = mapKeys.length; n < l; n++)
    {
      PackSupport.pack(mapKeys[n], packStream);
      PackSupport.pack(mapValues[n], packStream);
    }

    final var hasDefaultValue = defaultValue != null;
    packStream.writeBoolean(hasDefaultValue);

    if (hasDefaultValue)
      PackSupport.pack(defaultValue, packStream);
  }


  public static @NotNull MessagePartMap unpack(@NotNull PackSupport unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    final var map = new LinkedHashMap<MapKey,TypedValue<?>>();

    for(int n = 0, size = packStream.readSmallVar(); n < size; n++)
      map.put(unpack.unpackMapKey(packStream), unpack.unpackTypedValue(packStream));

    if (packStream.readBoolean())
      map.put(null, unpack.unpackTypedValue(packStream));

    return new MessagePartMap(map);
  }




  private static final class ConfigKeyComparatorContext extends BaseConfigAccessor implements ComparatorContext
  {
    private final Locale locale;
    private MapKey mapKey;


    private ConfigKeyComparatorContext(@NotNull MessageAccessor messageAccessor, @NotNull Locale locale,
                                       MessagePart.Config config)
    {
      super(messageAccessor, config);

      this.locale = locale;
    }


    @Override
    public @NotNull MapKey.CompareType getCompareType() {
      return mapKey.getCompareType();
    }


    @Override
    public @NotNull MapKey.Type getKeyType() {
      return mapKey.getType();
    }


    @Override
    public boolean getBoolKeyValue() {
      return ((MapKeyBool)mapKey).isBool();
    }


    @Override
    public long getNumberKeyValue() {
      return ((MapKeyNumber)mapKey).getNumber();
    }


    @Override
    public @NotNull String getStringKeyValue() {
      return ((MapKeyString)mapKey).getString();
    }


    @Override
    public @NotNull Locale getLocale() {
      return locale;
    }


    @Override
    public @NotNull MatchResult matchForObject(Object value)
    {
      return findBestMatch(this, messageAccessor
          .getFormatters(value == null ? Object.class : value.getClass(), config), value);
    }


    @Override
    public @NotNull <T> MatchResult matchForObject(@NotNull T value, @NotNull Class<T> valueType) {
      return findBestMatch(this, messageAccessor.getFormatters(valueType, config), value);
    }
  }
}
