/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;


/**
 * @author Jeroen Gremmen
 * @since 0.9.2
 */
public class SortedArrayMap<K extends Comparable<? super K>,V> implements Iterable<Entry<K,V>>
{
  private final Object[] array;
  private final int size;
  private final boolean hasNullKey;


  @SuppressWarnings("PointlessArithmeticExpression")
  public SortedArrayMap(Map<K,V> map)
  {
    if ((size = map != null ? map.size() : 0) > 0)
    {
      final List<K> keyList = new ArrayList<>(map.keySet());
      keyList.sort(nullsFirst(naturalOrder()));

      array = new Object[size * 2];

      for(int n = 0; n < size; n++)
      {
        final K key = keyList.get(n);

        array[n * 2 + 0] = key;
        array[n * 2 + 1] = map.get(key);
      }

      hasNullKey = array[0] == null;
    }
    else
    {
      array = null;
      hasNullKey = false;
    }
  }


  @Contract(pure = true)
  public int size() {
    return size;
  }


  @Contract(pure = true)
  public boolean isEmpty() {
    return size == 0;
  }


  @Contract(pure = true)
  @SuppressWarnings("unchecked")
  public V findValue(K key)
  {
    if (key != null)
    {
      for(int low = hasNullKey ? 1 : 0, high = size - 1; low <= high;)
      {
        final int mid = (low + high) >>> 1;
        final int cmp = ((K)array[mid * 2]).compareTo(key);

        if (cmp < 0)
          low = mid + 1;
        else if (cmp > 0)
          high = mid - 1;
        else
          return (V)array[mid * 2 + 1];
      }
    }
    else if (hasNullKey)
      return (V)array[1];

    return null;
  }


  @Contract(pure = true)
  @SuppressWarnings("unchecked")
  public @NotNull K[] getKeys(@NotNull Class<K> keyType)
  {
    final K[] keys = (K[])Array.newInstance(keyType, size);

    for(int n = 0; n < size; n++)
      keys[n] = (K)array[n * 2];

    return keys;
  }


  @Override
  public @NotNull Iterator<Entry<K, V>> iterator()
  {
    return new Iterator<Entry<K, V>>() {
      private int n = 0;


      @Override
      public boolean hasNext() {
        return n < size;
      }


      @Override
      public Entry<K,V> next()
      {
        if (!hasNext())
          throw new NoSuchElementException();

        return new EntryDelegate(n++);
      }
    };
  }


  @Override
  public Spliterator<Entry<K,V>> spliterator()
  {
    return new Spliterator<Entry<K, V>>() {
      private int n = 0;


      @Override
      public boolean tryAdvance(Consumer<? super Entry<K,V>> action)
      {
        if (n < size)
        {
          action.accept(new EntryDelegate(n++));
          return true;
        }

        return false;
      }


      @Override
      public Spliterator<Entry<K,V>> trySplit() {
        return null;
      }


      @Override
      public long estimateSize() {
        return size;
      }


      @Override
      public int characteristics() {
        return ORDERED | DISTINCT | IMMUTABLE | NONNULL | SIZED ;
      }
    };
  }


  public @NotNull Stream<Entry<K,V>> stream() {
    return StreamSupport.stream(spliterator(), false);
  }




  @SuppressWarnings({"DataFlowIssue", "unchecked"})
  private final class EntryDelegate implements Entry<K,V>
  {
    private final int offset;


    private EntryDelegate(int index) {
      offset = index * 2;
    }


    @Override
    public K getKey() {
      return (K)array[offset];
    }


    @Override
    public V getValue() {
      return (V)array[offset + 1];
    }


    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException("setValue");
    }


    @Override
    public boolean equals(Object o)
    {
      if (o instanceof Entry)
      {
        final Entry<?,?> entry = (Entry<?,?>)o;

        return
            Objects.equals(array[offset], entry.getKey()) &&
            Objects.equals(array[offset + 1], entry.getValue());
      }

      return false;
    }


    @Override
    public int hashCode() {
      return SortedArrayMap.this.hashCode() * 31 + offset;
    }


    @Override
    public String toString() {
      return String.valueOf(array[offset]) + '=' + array[offset + 1];
    }
  }
}
