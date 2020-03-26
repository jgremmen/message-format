/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.data;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class ParameterMap implements ParameterData
{
  private static final long serialVersionUID = 302L;

  private final Map<Key,Message> map;


  public ParameterMap(@NotNull Map<Key,Message> map) {
    this.map = map;
  }


  @Contract(pure = true)
  public Message getMessageFor(boolean key) {
    return getMessageForKey(key);
  }


  @SuppressWarnings("unused")
  @Contract(pure = true)
  public Message getMessageFor(int key) {
    return getMessageForKey(key);
  }


  @SuppressWarnings("unused")
  @Contract(pure = true)
  public Message getMessageFor(String key) {
    return getMessageForKey(key);
  }


  @Contract(pure = true)
  public boolean hasMessageForKey(Serializable key) {
    return getMessageForKey(key) != null || map.get(null) != null;
  }


  @SuppressWarnings("squid:S3776")
  private Message getMessageForKey(Serializable key)
  {
    if (key != null)
      for(final Entry<Key,Message> entry: map.entrySet())
      {
        final Key cmpkey = entry.getKey();

        if (cmpkey != null)
        {
          final int cmp = compareKey(key, cmpkey.value);
          final Message message = entry.getValue();

          switch(cmpkey.compareType)
          {
            case EQ:  if (cmp == 0) return message; break;
            case NE:  if (cmp != 0) return message; break;
            case GT:  if (cmp > 0)  return message; break;
            case GTE: if (cmp >= 0) return message; break;
            case LT:  if (cmp < 0)  return message; break;
            case LTE: if (cmp <= 0) return message; break;
          }
        }
     }

    return map.get(null);
  }


  @Override
  @Contract(pure = true)
  public String format(@NotNull Parameters parameters, Serializable key)
  {
    Message message = getMessageForKey(key);
    if (message == null)
      message = map.get(null);

    return (message == null) ? null : message.format(parameters);
  }


  private int compareKey(Object key, Object mapKey)
  {
    if (key instanceof Boolean || mapKey instanceof Boolean)
      return (toBoolean(key) ? 1 : 0) - (toBoolean(mapKey) ? 1 : 0);

    if (key instanceof Number || mapKey instanceof Number)
      return Integer.signum(toInteger(key) - toInteger(mapKey));

    return String.valueOf(key).compareTo(String.valueOf(mapKey));
  }


  private boolean toBoolean(Object value)
  {
    if (value instanceof Boolean)
      return (Boolean)value;

    if (value instanceof BigInteger)
      return ((BigInteger)value).signum() != 0;

    if (value instanceof Number)
      return ((Number)value).longValue() != 0;

    return Boolean.parseBoolean(String.valueOf(value));
  }


  private int toInteger(Object value)
  {
    if (value instanceof Number)
      return ((Number)value).intValue();

    return Integer.parseInt(String.valueOf(value));
  }


  @Override
  @Contract(pure = true)
  public String format(@NotNull Parameters parameters) {
    return format(parameters, null);
  }


  @Override
  @Contract(value = "-> fail", pure = true)
  public Serializable asObject() {
    return new UnsupportedOperationException();
  }


  public static class Key implements Serializable
  {
    private static final long serialVersionUID = 201L;

    private final CompareType compareType;
    private final Serializable value;


    public Key(CompareType compareType, Serializable value)
    {
      this.compareType = compareType;
      this.value = value;
    }
  }


  public enum CompareType {
    LT, LTE, EQ, NE, GT, GTE
  }
}
