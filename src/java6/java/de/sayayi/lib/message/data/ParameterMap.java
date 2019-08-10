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
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;


/**
 * @author Jeroen Gremmen
 *
 */
@ToString
public class ParameterMap implements ParameterData
{
  private static final long serialVersionUID = 201L;

  private final Map<Serializable,Message> map;


  public ParameterMap(@NotNull Map<Serializable,Message> map) {
    this.map = map;
  }


  public Message getMessageFor(boolean key) {
    return getMessageForKey(key);
  }


  @SuppressWarnings("unused")
  public Message getMessageFor(int key) {
    return getMessageForKey(key);
  }


  @SuppressWarnings("unused")
  public Message getMessageFor(String key) {
    return getMessageForKey(key);
  }


  private Message getMessageForKey(Serializable key)
  {
    Message message = map.get(key);

    if (message == null && key != null)
      for(final Entry<Serializable,Message> entry: map.entrySet())
        if (compareKey(key, entry.getKey()))
        {
          message = entry.getValue();
          break;
        }

    return message;
  }


  @Override
  public String format(@NotNull Parameters parameters, Serializable key)
  {
    Message message = getMessageForKey(key);
    if (message == null)
      message = map.get(null);

    return (message == null) ? null : message.format(parameters);
  }


  private boolean compareKey(Object key, Object mapKey)
  {
    try {
      if (key instanceof Boolean || mapKey instanceof Boolean)
        return toBoolean(key) == toBoolean(mapKey);

      if (key instanceof Number || mapKey instanceof Number)
        return toInteger(key) == toInteger(mapKey);

      return String.valueOf(key).equals(String.valueOf(mapKey));
    } catch(final Exception ex) {
      return false;
    }
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
  public String format(@NotNull Parameters parameters) {
    return format(parameters, null);
  }


  @Override
  public Serializable asObject() {
    return new UnsupportedOperationException();
  }
}
