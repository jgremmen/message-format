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
package de.sayayi.lib.message.internal.part.template;

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
 * Compact, immutable map backed by a sorted array of key-value pairs. Keys are stored in their
 * {@linkplain Comparable natural order} (with {@code null} sorted first) and lookups are performed using binary search.
 * <p>
 * The map is constructed from a regular {@link Map} and stored as a flat {@code Object[]} array where even indices
 * hold keys and odd indices hold the corresponding values. This layout minimizes memory overhead compared to
 * node-based map implementations.
 * <p>
 * Instances of this class are unmodifiable: iteration and streaming are supported, but mutating operations
 * (e.g. {@link Entry#setValue(Object)}) throw {@link UnsupportedOperationException}.
 *
 * @param <K>  the key type, must be {@link Comparable}
 * @param <V>  the value type
 *
 * @author Jeroen Gremmen
 * @since 0.9.2
 */
class SortedArrayMap<K extends Comparable<? super K>,V> implements Iterable<Entry<K,V>>
{
  /** Flat array holding interleaved key-value pairs: {@code [k0, v0, k1, v1, ...]}. */
  private final Object[] array;

  /** The number of key-value pairs in this map. */
  private final int size;

  /** {@code true} if the map contains a {@code null} key (always at index 0). */
  private final boolean hasNullKey;


  /**
   * Creates a new sorted array map from the given map.
   * <p>
   * The entries are copied and sorted by their keys' {@linkplain Comparable natural order},
   * with {@code null} keys sorted first. A {@code null} or empty map results in an empty
   * sorted array map.
   *
   * @param map  the source map to copy entries from, may be {@code null}
   */
  @SuppressWarnings("PointlessArithmeticExpression")
  SortedArrayMap(Map<K,V> map)
  {
    if ((size = map != null ? map.size() : 0) > 0)
    {
      final var keyList = new ArrayList<>(map.keySet());
      keyList.sort(nullsFirst(naturalOrder()));

      array = new Object[size * 2];

      for(var n = 0; n < size; n++)
      {
        var key = keyList.get(n);

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


  /**
   * Returns the number of key-value pairs in this map.
   *
   * @return the map size, never negative
   */
  @Contract(pure = true)
  public int size() {
    return size;
  }


  /**
   * Returns {@code true} if this map contains no key-value pairs.
   *
   * @return {@code true} if the map is empty, {@code false} otherwise
   */
  @Contract(pure = true)
  public boolean isEmpty() {
    return size == 0;
  }


  /**
   * Looks up the value associated with the given key using binary search.
   *
   * @param key  the key to search for, may be {@code null}
   *
   * @return the value mapped to the key, or {@code null} if the key is not present
   */
  @Contract(pure = true)
  @SuppressWarnings("unchecked")
  public V findValue(K key)
  {
    if (key != null)
    {
      for(int low = hasNullKey ? 1 : 0, high = size - 1; low <= high;)
      {
        var mid = (low + high) >>> 1;
        var cmp = ((K)array[mid * 2]).compareTo(key);

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


  /**
   * Returns an array containing all keys in this map, in sorted order.
   *
   * @param keyType  the component type of the resulting array, not {@code null}
   *
   * @return a newly allocated array of all keys, never {@code null}
   */
  @Contract(pure = true)
  @SuppressWarnings("unchecked")
  public @NotNull K[] getKeys(@NotNull Class<K> keyType)
  {
    var keys = (K[])Array.newInstance(keyType, size);

    for(var n = 0; n < size; n++)
      keys[n] = (K)array[n * 2];

    return keys;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns an iterator over the entries in this map, ordered by key. The returned
   * {@link Entry} instances are immutable.
   */
  @Override
  public @NotNull Iterator<Entry<K,V>> iterator()
  {
    return new Iterator<>() {
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


  /**
   * {@inheritDoc}
   * <p>
   * Returns a {@link Spliterator} over the entries in this map. The spliterator reports
   * {@link Spliterator#ORDERED}, {@link Spliterator#DISTINCT}, {@link Spliterator#IMMUTABLE},
   * {@link Spliterator#NONNULL}, and {@link Spliterator#SIZED} characteristics.
   */
  @Override
  public Spliterator<Entry<K,V>> spliterator()
  {
    return new Spliterator<>() {
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


  /**
   * Returns a sequential {@link Stream} of the entries in this map, ordered by key.
   *
   * @return a sequential stream of map entries, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Stream<Entry<K,V>> stream() {
    return StreamSupport.stream(spliterator(), false);
  }




  /**
   * Immutable {@link Entry} implementation backed by the enclosing map's internal array.
   * <p>
   * Each delegate references a fixed offset into the flat key-value array.
   * Calling {@link #setValue(Object)} always throws {@link UnsupportedOperationException}.
   */
  @SuppressWarnings({"DataFlowIssue", "unchecked"})
  private final class EntryDelegate implements Entry<K,V>
  {
    /** Offset into the flat array; the key is at {@code array[offset]} and the value at {@code array[offset + 1]}. */
    private final int offset;


    /**
     * Creates a new entry delegate for the entry at the given index.
     *
     * @param index  zero-based entry index (not the raw array offset)
     */
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


    /**
     * Always throws {@link UnsupportedOperationException} as this map is immutable.
     *
     * @param value  ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException("setValue");
    }


    @Override
    public boolean equals(Object o)
    {
      return
          o instanceof Entry<?,?> entry &&
          Objects.equals(array[offset], entry.getKey()) &&
          Objects.equals(array[offset + 1], entry.getValue());
    }


    @Override
    public int hashCode()
    {
      final var key = array[offset];
      final var value = array[offset + 1];

      return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }


    /**
     * Returns a string representation of this entry in the form {@code key=value}.
     *
     * @return a string representation of this entry
     */
    @Override
    public String toString() {
      return String.valueOf(array[offset]) + '=' + array[offset + 1];
    }
  }
}
