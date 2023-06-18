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

import lombok.SneakyThrows;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static java.util.Arrays.fill;
import static java.util.Collections.*;


/**
 * @param <K>  map key type
 * @param <V>  map value type
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ImmutableArrayMap<K,V> extends AbstractMap<K,V>
{
  /** number of hash slots */
  private final int hashSlots;

  // size: hash code for key
  // size: next index with same hash
  // slots: hash index to 1st key index
  private final int[] hi;
  private final Object[] kv;

  /** map size */
  private final int size;


  private ImmutableArrayMap(@NotNull Object[] kv)
  {
    size = kv.length / 2;
    hashSlots = size == 1 ? 1 : max(size * 3 / 2, 5);
    fill(hi = new int[size * 2 + hashSlots], -1);
    this.kv = kv;

    final int[] lastIndex4Slot = new int[size];
    fill(lastIndex4Slot, -1);

    for(int n = 0; n < size; n++)
    {
      final int hash = hi[n] = hashCode(kv[n * 2]);
      final int slotIdx = size * 2 + (hash % hashSlots);
      final int initialSlot;

      if (hi[slotIdx] == -1)
        initialSlot = hi[slotIdx] = n;
      else
        initialSlot = hi[slotIdx];

      final int li4s = lastIndex4Slot[initialSlot];
      if (li4s >= 0)
        hi[size + li4s] = n;

      lastIndex4Slot[initialSlot] = n;
    }
  }


  /**
   * Construct an immutable map for the given {@code map}.
   * <p>
   * The entries in the immutable map have the same order as the original map.
   *
   * @param map  map to copy entries from, not {@code null}
   */
  public ImmutableArrayMap(@NotNull Map<K,V> map)
  {
    if ((size = map.size()) == 0)
    {
      hashSlots = 0;
      hi = null;
      kv = null;
    }
    else
    {
      hashSlots = size == 1 ? 1 : max(size * 3 / 2, 5);
      fill(hi = new int[size * 2 + hashSlots], -1);
      kv = new Object[size * 2];

      final int[] lastIndex4Slot = new int[size];
      fill(lastIndex4Slot, -1);

      int n = 0;

      for(final Entry<K,V> entry: map.entrySet())
      {
        final Object key = entry.getKey();
        final int hash = hashCode(key);

        kv[n * 2] = key;
        kv[n * 2 + 1] = entry.getValue();
        hi[n] = hash;

        final int slotIdx = size * 2 + (hash % hashSlots);
        final int initialSlot;

        if (hi[slotIdx] == -1)
          initialSlot = hi[slotIdx] = n;
        else
          initialSlot = hi[slotIdx];

        final int li4s = lastIndex4Slot[initialSlot];
        if (li4s >= 0)
          hi[size + li4s] = n;

        lastIndex4Slot[initialSlot] = n++;
      }
    }
  }


  @Contract(pure = true)
  public static @NotNull <K,V> ImmutableArrayMap<K,V> of(K key, V value) {
    return new ImmutableArrayMap<>(new Object[] { key, value });
  }


  @Contract(pure = true)
  public static @NotNull <K,V> ImmutableArrayMap<K,V> of(K key1, V value1,
                                                         K key2, V value2) {
    return new ImmutableArrayMap<>(new Object[] { key1, value1, key2, value2 });
  }


  @Contract(pure = true)
  public static @NotNull <K,V> ImmutableArrayMap<K,V> of(K key1, V value1,
                                                         K key2, V value2,
                                                         K key3, V value3) {
    return new ImmutableArrayMap<>(new Object[] { key1, value1, key2, value2, key3, value3 });
  }


  @Contract(pure = true)
  public static @NotNull <K,V> ImmutableArrayMap<K,V> of(K key1, V value1,
                                                         K key2, V value2,
                                                         K key3, V value3,
                                                         K key4, V value4)
  {
    return new ImmutableArrayMap<>(
        new Object[] { key1, value1, key2, value2, key3, value3, key4, value4 });
  }


  @Contract(pure = true)
  private int hashCode(Object key) {
    return key == null ? 0 : (key.hashCode() & MAX_VALUE);
  }


  @Override public int size() { return size; }
  @Override public boolean isEmpty() { return size == 0; }
  @Override public V get(Object key) { return getOrDefault(key, null); }
  @Override public void clear() { throw new UnsupportedOperationException("clear"); }
  @Override public V put(K key, V value) { throw new UnsupportedOperationException("put"); }
  @Override public V remove(Object key) { throw new UnsupportedOperationException("remove"); }


  @Override
  @SuppressWarnings("unchecked")
  public void forEach(BiConsumer<? super K,? super V> action)
  {
    for(int n = 0; n < size; n++)
      action.accept((K)kv[n * 2], (V)kv[n * 2 + 1]);
  }


  @Override
  @SuppressWarnings("unchecked")
  public V getOrDefault(Object key, V defaultValue)
  {
    if (size > 0)
      for(int hash = hashCode(key), idx = hi[size * 2 + hash % hashSlots]; idx >= 0;)
        if (hi[idx] == hash && Objects.equals(key, kv[idx * 2]))
          return (V)kv[idx * 2 + 1];
        else
          idx = hi[size + idx];

    return defaultValue;
  }


  @Override
  public boolean containsKey(Object key)
  {
    if (size > 0)
      for(int hash = hashCode(key), idx = hi[size * 2 + hash % hashSlots]; idx >= 0;)
        if (hi[idx] == hash && Objects.equals(key, kv[idx * 2]))
          return true;
        else
          idx = hi[size + idx];

    return false;
  }


  @Override
  public boolean containsValue(Object value)
  {
    for(int n = 0; n < size; n++)
      if (Objects.equals(value, kv[n * 2 + 1]))
        return true;

    return false;
  }


  @Override
  public @NotNull Set<K> keySet() {
    return size == 0 ? emptySet() : new KeySet();
  }


  @Override
  public @NotNull Collection<V> values() {
    return size == 0 ? emptyList() : new ValueCollection();
  }


  @Override
  public @NotNull Set<Entry<K,V>> entrySet() {
    return size == 0 ? emptySet() : new EntrySet();
  }


  @Override
  @SneakyThrows(CloneNotSupportedException.class)
  @SuppressWarnings("unchecked")
  public @NotNull Map<K,V> clone()
  {
    switch(size)
    {
      case 0:
        return emptyMap();

      case 1:
        return singletonMap((K)kv[0], (V)kv[1]);

      default:
        return (Map<K,V>)super.clone();
    }
  }


  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (!(o instanceof Map))
      return false;

    final Map<?,?> m = (Map<?,?>)o;
    if (size != m.size())
      return false;

    try {
      for(int n = 0; n < size; n++)
      {
        final Object key = kv[n * 2];
        final Object value = kv[n * 2 + 1];

        if (value == null)
        {
          if (!(m.get(key) == null && m.containsKey(key)))
            return false;
        }
        else if (!value.equals(m.get(key)))
          return false;
      }
    } catch(ClassCastException | NullPointerException unused) {
      return false;
    }

    return true;
  }


  @Override
  public int hashCode()
  {
    int hash = 0;

    for(int n = 0; n < size; n++)
      hash += hi[n];

    return hash;
  }


  @Override
  public String toString()
  {
    if (size == 0)
      return "{}";

    final StringBuilder sb = new StringBuilder().append('{');

    for(int n = 0; n < size; n++)
    {
      final Object key = kv[n * 2];
      final Object value = kv[n * 2 + 1];

      sb.append(key == this ? "(this Map)" : key)
        .append('=')
        .append(value == this ? "(this Map)" : value);

      if (n + 1 < size)
        sb.append(", ");
    }

    return sb.append('}').toString();
  }




  @SuppressWarnings("DataFlowIssue")
  private final class KeySet extends AbstractSet<K>
  {
    @Override
    public @NotNull Iterator<K> iterator()
    {
      return new Iterator<K>() {
        private int n = 0;


        @Override
        public boolean hasNext() {
          return n < size;
        }


        @Override
        @SuppressWarnings("unchecked")
        public K next()
        {
          if (!hasNext())
            throw new NoSuchElementException();

          return (K)kv[n++ * 2];
        }
      };
    }


    @Override
    public @NotNull Spliterator<K> spliterator()
    {
      return new Spliterator<K>() {
        private int n = 0;


        @Override
        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super K> action)
        {
          if (n < size)
          {
            action.accept((K)kv[n++ * 2]);
            return true;
          }

          return false;
        }


        @Override public Spliterator<K> trySplit() { return null; }
        @Override public long estimateSize() { return size; }
        @Override public int characteristics() { return SIZED | DISTINCT | IMMUTABLE | ORDERED; }
      };
    }


    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }
    @Override public void clear() { throw new UnsupportedOperationException("clear"); }
    @Override public boolean remove(Object o) { throw new UnsupportedOperationException("remove"); }
    @Override public boolean contains(Object o) { return containsKey(o); }
    @Override public int hashCode() { return ImmutableArrayMap.this.hashCode(); }


    @Override
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super K> action)
    {
      for(int n = 0, l = size * 2; n < l; n += 2)
        action.accept((K)kv[n]);
    }


    @Override
    public @NotNull Object[] toArray()
    {
      final Object[] array = new Object[size];

      for(int n = 0, l = size * 2; n < l; n += 2)
        array[n] = kv[n];

      return array;
    }


    @Override
    @SuppressWarnings("unchecked")
    public @NotNull <T> T[] toArray(@NotNull T[] array)
    {
      if (array.length < size)
        array = (T[])Array.newInstance(array.getClass().getComponentType(), size);

      for(int n = 0, l = size * 2; n < l; n += 2)
        array[n] = (T)kv[n];

      if (array.length > size)
        array[size] = null;

      return array;
    }


    @Override
    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      else if (!(o instanceof Set))
        return false;

      final Set<?> that = (Set<?>)o;

      if (size == that.size())
      {
        try {
          for(int n = 0, l = size * 2; n < l; n += 2)
            if (!that.contains(kv[n]))
              return false;

          return true;
        } catch(ClassCastException | NullPointerException ignored) {
        }
      }

      return false;
    }


    @Override
    public String toString()
    {
      if (size == 0)
        return "[]";

      final StringBuilder sb = new StringBuilder().append('[');

      for(int n = 0, l = size * 2; n < l; n += 2)
      {
        final Object key = kv[n];

        sb.append(key == this ? "(this Set)" : key);

        if (n + 1 < size)
          sb.append(", ");
      }

      return sb.append(']').toString();
    }
  }




  @SuppressWarnings("DataFlowIssue")
  private final class ValueCollection extends AbstractCollection<V>
  {
    @Override
    public @NotNull Iterator<V> iterator()
    {
      return new Iterator<V>() {
        private int n = 0;


        @Override
        public boolean hasNext() {
          return n < size;
        }


        @Override
        @SuppressWarnings("unchecked")
        public V next()
        {
          if (!hasNext())
            throw new NoSuchElementException();

          return (V)kv[n++ * 2 + 1];
        }
      };
    }


    @Override
    public Spliterator<V> spliterator()
    {
      return new Spliterator<V>() {
        private int n = 0;


        @Override
        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super V> action)
        {
          if (n < size)
          {
            action.accept((V)kv[n++ * 2 + 1]);
            return true;
          }

          return false;
        }


        @Override public Spliterator<V> trySplit() { return null; }
        @Override public long estimateSize() { return size; }
        @Override public int characteristics() { return SIZED | IMMUTABLE | ORDERED; }
      };
    }


    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }
    @Override public void clear() { throw new UnsupportedOperationException("clear"); }
    @Override public boolean remove(Object o) { throw new UnsupportedOperationException("remove"); }
    @Override public boolean contains(Object o) { return containsValue(o); }


    @Override
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super V> action)
    {
      for(int n = 0; n < size; n++)
        action.accept((V)kv[n * 2 + 1]);
    }


    @Override
    public @NotNull Object[] toArray()
    {
      final Object[] array = new Object[size];

      for(int n = 0; n < size; n++)
        array[n] = kv[n * 2 + 1];

      return array;
    }


    @Override
    @SuppressWarnings("unchecked")
    public @NotNull <T> T[] toArray(@NotNull T[] array)
    {
      if (array.length < size)
        array = (T[])Array.newInstance(array.getClass().getComponentType(), size);

      for(int n = 0; n < size; n++)
        array[n] = (T)kv[n * 2 + 1];

      if (array.length > size)
        array[size] = null;

      return array;
    }


    @Override
    public String toString()
    {
      if (size == 0)
        return "[]";

      final StringBuilder sb = new StringBuilder().append('[');

      for(int n = 0; n < size; n++)
      {
        final Object value = kv[n * 2 + 1];

        sb.append(value == this ? "(this Collection)" : value);

        if (n + 1 < size)
          sb.append(", ");
      }

      return sb.append(']').toString();
    }
  }




  @SuppressWarnings("DataFlowIssue")
  private final class EntrySet extends AbstractSet<Entry<K,V>>
  {
    @Override
    public @NotNull Iterator<Entry<K,V>> iterator()
    {
      return new Iterator<Entry<K,V>>() {
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

          return new ImmutableEntry(n++);
        }
      };
    }


    @Override
    public Spliterator<Entry<K,V>> spliterator()
    {
      return new Spliterator<Entry<K,V>>() {
        private int n = 0;


        @Override
        public boolean tryAdvance(Consumer<? super Entry<K,V>> action)
        {
          if (n < size)
          {
            action.accept(new ImmutableEntry(n++));
            return true;
          }

          return false;
        }


        @Override public Spliterator<Entry<K,V>> trySplit() { return null; }
        @Override public long estimateSize() { return size; }
        @Override public int characteristics() { return SIZED | DISTINCT | IMMUTABLE | ORDERED; }
      };
    }


    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }
    @Override public void clear() { throw new UnsupportedOperationException("clear"); }
    @Override public boolean remove(Object o) { throw new UnsupportedOperationException("remove"); }


    @Override
    public boolean contains(Object o)
    {
      if (size == 0 || !(o instanceof Entry))
        return false;

      final Entry<?,?> entry = (Entry<?,?>)o;
      final Object key = entry.getKey();

      for(int hash = ImmutableArrayMap.this.hashCode(key),
          idx = hi[size * 2 + hash % hashSlots]; idx >= 0;)
        if (hi[idx] == hash &&
            Objects.equals(key, kv[idx * 2]) &&
            Objects.equals(entry.getValue(), kv[idx * 2 + 1]))
          return true;
        else
          idx = hi[size + idx];

      return false;
    }


    @Override
    public void forEach(Consumer<? super Entry<K,V>> action)
    {
      for(int n = 0; n < size; n++)
        action.accept(new ImmutableEntry(n));
    }


    @Override
    public String toString()
    {
      if (size == 0)
        return "[]";

      final StringBuilder sb = new StringBuilder().append('[');

      for(int n = 0; n < size; n++)
      {
        sb.append(kv[n * 2]).append('=').append(kv[n * 2 + 1]);
        if (n + 1 < size)
          sb.append(", ");
      }

      return sb.append(']').toString();
    }
  }




  @SuppressWarnings("DataFlowIssue")
  private final class ImmutableEntry implements Entry<K,V>
  {
    private final int idx;


    private ImmutableEntry(int n) {
      idx = n * 2;
    }


    @Override
    @SuppressWarnings("unchecked")
    public K getKey() {
      return (K)kv[idx];
    }


    @Override
    @SuppressWarnings("unchecked")
    public V getValue() {
      return (V)kv[idx + 1];
    }


    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException("setValue");
    }


    @Override
    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      else if (!(o instanceof Entry))
        return false;

      final Entry<?,?> that = (Entry<?,?>)o;

      return Objects.equals(kv[idx], that.getKey()) &&
             Objects.equals(kv[idx + 1], that.getValue());
    }


    @Override
    public int hashCode() {
      return idx + 1;
    }


    @Override
    public String toString() {
      return String.valueOf(kv[idx]) + '=' + kv[idx + 1];
    }
  }
}
