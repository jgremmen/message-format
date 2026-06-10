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
package de.sayayi.lib.message.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Objects.requireNonNull;


/**
 * A compact, array-backed {@link Map} implementation with {@link String} keys kept in sorted order. This map supports
 * an optional {@linkplain #seal() seal} operation that makes it immutable and trims internal storage.
 * <p>
 * Keys are maintained in natural string order, enabling binary search for efficient lookups. {@code null} keys are
 * not supported.
 * <p>
 * This map is not thread-safe.
 *
 * @param <V>  the type of values stored in this map
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
public final class SortedStringMap<V> extends AbstractMap<String,V> implements Cloneable
{
  private Object[] kv;
  private int size;
  private boolean sealed;


  /**
   * Creates an empty, unsealed map.
   */
  public SortedStringMap() {
    this(null);
  }


  /**
   * Creates an unsealed map initialized with the entries from the given map.
   *
   * @param map  the map whose entries are to be copied, or {@code null} for an empty map
   */
  public SortedStringMap(Map<String,V> map) {
    this(map, false);
  }


  /**
   * Creates a map initialized with the entries from the given map, optionally sealed.
   *
   * @param map   the map whose entries are to be copied, or {@code null} for an empty map
   * @param seal  {@code true} to seal the map immediately after construction
   */
  public SortedStringMap(Map<String,V> map, boolean seal)
  {
    if (map instanceof SortedStringMap<V> sortedStringMap)
    {
      size = map.size();
      kv = copyOf(sortedStringMap.kv, size * 2);
    }
    else if (map == null)
      kv = seal ? null : new Object[16];
    else if (!(seal && map.isEmpty()))
    {
      kv = new Object[map.size() * 2];

      putAll(map);
    }

    sealed = seal;
  }


  /**
   * Seals this map, making it immutable. Any subsequent modification attempt will throw an
   * {@link UnsupportedOperationException}. Sealing also trims the internal storage to minimize memory usage.
   */
  @Contract(mutates = "this")
  public void seal()
  {
    if (!sealed)
    {
      sealed = true;

      if (size == 0)
        kv = null;
      else
      {
        final var requiredLength = size * 2;
        if (kv.length > requiredLength)
          kv = copyOf(kv, requiredLength);
      }
    }
  }


  /** {@inheritDoc} */
  @Override
  public int size() {
    return size;
  }


  /** {@inheritDoc} */
  @Override
  public boolean isEmpty() {
    return size == 0;
  }


  /** {@inheritDoc} */
  @Override
  public boolean containsKey(Object key) {
    return key instanceof String string && findKeyIndex(string) >= 0;
  }


  /** {@inheritDoc} */
  @Override
  public boolean containsValue(Object value)
  {
    for(int offset = 1, length = size * 2; offset < length; offset += 2)
      if (Objects.equals(kv[offset], value))
        return true;

    return false;
  }


  /** {@inheritDoc} */
  @Override
  public V get(Object key) {
    return getOrDefault(key, null);
  }


  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("unchecked")
  public V getOrDefault(Object key, V defaultValue)
  {
    if (key instanceof String string)
    {
      var idx = findKeyIndex(string);
      if (idx >= 0)
        return (V)kv[idx * 2 + 1];
    }

    return defaultValue;
  }


  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException  if this map is sealed
   * @throws NullPointerException           if the key is {@code null}
   */
  @Override
  @Contract(mutates = "this")
  @SuppressWarnings("unchecked")
  public V put(String key, V value)
  {
    if (sealed)
      throw new UnsupportedOperationException("put");

    final var idx = findKeyIndex(requireNonNull(key, "key must not be null"));
    if (idx >= 0)
    {
      final var valueOffset = idx * 2 + 1;

      V previous = (V)kv[valueOffset];
      kv[valueOffset] = value;

      return previous;
    }
    else
    {
      final var length = size * 2;
      final var insertOffset = -(idx + 1) * 2;

      if (length == kv.length)
      {
        final var kvNew = new Object[length + 8];

        arraycopy(kv, 0, kvNew, 0, insertOffset);
        arraycopy(kv, insertOffset, kvNew, insertOffset + 2, length - insertOffset);

        kv = kvNew;
      }
      else
        arraycopy(kv, insertOffset, kv, insertOffset + 2, length - insertOffset);

      kv[insertOffset] = key;
      kv[insertOffset + 1] = value;

      size++;

      return null;
    }
  }


  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException  if this map is sealed
   */
  @Override
  @Contract(mutates = "this")
  @SuppressWarnings("unchecked")
  public V remove(Object key)
  {
    V result = null;

    if (key instanceof String string)
    {
      final var idx = findKeyIndex(string);
      if (idx >= 0)
      {
        if (sealed)
          throw new UnsupportedOperationException("remove");

        final var offset = idx * 2;

        result = (V)kv[offset + 1];
        arraycopy(kv, offset + 2, kv, offset, --size * 2 - offset);
      }
    }

    return result;
  }


  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException  if this map is sealed and non-empty
   */
  @Override
  @Contract(mutates = "this")
  public void clear()
  {
    if (size > 0)
    {
      if (sealed)
        throw new UnsupportedOperationException("clear");

      size = 0;
    }
  }


  /**
   * Returns a new array containing all keys in this map in sorted order.
   *
   * @return  an array of all keys, never {@code null}
   */
  @Contract(pure = true)
  public String @NotNull [] getKeys()
  {
    final var keys = new String[size];

    for(var n = 0; n < size; n++)
      keys[n] = (String)kv[n * 2];

    return keys;
  }


  /**
   * Returns a shallow copy of this map. The returned map is unsealed.
   *
   * @return  a new {@code SortedStringMap} with the same entries
   */
  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  public @NotNull SortedStringMap<V> clone() {
    return new SortedStringMap<>(this);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Set<Entry<String,V>> entrySet() {
    return new SortedEntrySet();
  }


  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("unchecked")
  public void forEach(BiConsumer<? super String,? super V> action)
  {
    for(int offset = 0, length = size * 2; offset < length; offset += 2)
      action.accept((String)kv[offset], (V)kv[offset + 1]);
  }


  /**
   * Returns a sequential {@link Stream} of the entries in this map, in key order.
   *
   * @return  a stream of map entries
   */
  @Contract(pure = true)
  public Stream<Entry<String,V>> stream() {
    return StreamSupport.stream(new EntrySpliterator(), false);
  }


  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o)
  {
    if (this != o)
    {
      if (!(o instanceof Map<?,?> that) || size != that.size())
        return false;

      try {
        for(int offset = 0, length = size * 2; offset < length; offset += 2)
        {
          final var key = kv[offset];
          final var value = kv[offset + 1];

          if (value == null)
          {
            if (that.get(key) != null || !that.containsKey(key))
              return false;
          }
          else if (!value.equals(that.get(key)))
            return false;
        }
      } catch(ClassCastException | NullPointerException ex) {
        return false;
      }
    }

    return true;
  }


  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    var hash = 0;

    // respect map hashcode contract!
    for(int offset = 0, length = size * 2; offset < length; offset += 2)
      hash += kv[offset].hashCode() ^ Objects.hashCode(kv[offset + 1]);

    return hash;
  }


  /** {@inheritDoc} */
  @Override
  public String toString()
  {
    if (size == 0)
      return "{}";

    final var sb = new StringBuilder("{");

    for(int offset = 0, length = size * 2; offset < length; offset += 2)
    {
      if (offset > 0)
        sb.append(", ");

      sb.append(kv[offset]).append('=').append(kv[offset + 1]);
    }

    return sb.append("}").toString();
  }


  /**
   * Performs a binary search for the given key in the internal sorted array.
   *
   * @param key  the key to search for, not {@code null}
   *
   * @return  the index of the key if found, or {@code -(insertion point) - 1} if not found
   */
  @Contract(pure = true)
  private int findKeyIndex(@NotNull String key)
  {
    var low = 0;

    for(var high = size - 1; low <= high;)
    {
      final var mid = (low + high) >>> 1;
      final var cmp = key.compareTo((String)kv[mid * 2]);

      if (cmp < 0)
        high = mid - 1;
      else if (cmp > 0)
        low = mid + 1;
      else
        return mid;
    }

    return -(low + 1);
  }


  /**
   * Returns an entry for the given index. For a sealed map, a lightweight {@link EntryDelegate} is returned that
   * directly references the internal array. For an unsealed map, a
   * {@link java.util.AbstractMap.SimpleImmutableEntry SimpleImmutableEntry} snapshot is returned, so the entry remains
   * stable even if the map is subsequently modified.
   *
   * @param index  the zero-based index of the entry in the map
   *
   * @return  a map entry for the given index, never {@code null}
   */
  @Contract(pure = true)
  @SuppressWarnings("unchecked")
  private @NotNull Entry<String,V> entry(int index)
  {
    if (sealed)
      return new EntryDelegate(index);

    final var offset = index * 2;

    return new SimpleImmutableEntry<>((String)kv[offset], (V)kv[offset + 1]);
  }




  /**
   * A {@link Set} view of the entries in the enclosing {@link SortedStringMap}, maintaining sorted key order.
   */
  private final class SortedEntrySet extends AbstractSet<Entry<String,V>>
  {
    /** {@inheritDoc} */
    @Override
    public int size() {
      return size;
    }


    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
      return size == 0;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Iterator<Entry<String,V>> iterator() {
      return new EntryIterator();
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Spliterator<Entry<String,V>> spliterator() {
      return new EntrySpliterator();
    }


    /** {@inheritDoc} */
    @Override
    public void clear() {
      SortedStringMap.this.clear();
    }


    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException  always
     */
    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException("remove");
    }


    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException  always
     */
    @Override
    public boolean removeIf(@NotNull Predicate<? super Entry<String,V>> filter) {
      throw new UnsupportedOperationException("removeIf");
    }


    /** {@inheritDoc} */
    @Override
    public void forEach(@NotNull Consumer<? super Entry<String,V>> action)
    {
      for(var idx = 0; idx < size; idx++)
        action.accept(entry(idx));
    }


    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(@NotNull IntFunction<T[]> generator)
    {
      final var array = generator.apply(size);

      for(var idx = 0; idx < size; idx++)
      {
        final var offset = idx * 2;
        array[idx] = (T)new SimpleImmutableEntry<>(kv[offset], kv[offset + 1]);
      }

      return array;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Object @NotNull [] toArray() {
      return toArray(Object[]::new);
    }


    /** {@inheritDoc} */
    @Override
    public String toString()
    {
      if (size == 0)
        return "[]";

      final var sb = new StringBuilder("[");

      for(int offset = 0, length = size * 2; offset < length; offset += 2)
      {
        if (offset > 0)
          sb.append(", ");

        sb.append((String)kv[offset]).append('=').append(kv[offset + 1]);
      }

      return sb.append("]").toString();
    }
  }




  /**
   * A lightweight {@link java.util.Map.Entry} delegate that provides a direct view onto a specific entry in the
   * enclosing {@link SortedStringMap}. This delegate is only used for sealed maps, where the internal data cannot
   * change, making it safe to reference the backing array by position.
   */
  private final class EntryDelegate implements Entry<String,V>
  {
    private final int offset;


    /**
     * Creates a new entry delegate for the entry at the given index.
     *
     * @param index  the zero-based index of the entry in the map
     */
    private EntryDelegate(int index) {
      offset = index * 2;
    }


    /** {@inheritDoc} */
    @Override
    public String getKey() {
      return (String)kv[offset];
    }


    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public V getValue() {
      return (V)kv[offset + 1];
    }


    /**
     * Always throws {@link UnsupportedOperationException} as this delegate is only used for sealed maps.
     *
     * @throws UnsupportedOperationException  always
     */
    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException("setValue");
    }


    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o)
    {
      return
          o instanceof Entry<?,?> entry &&
          kv[offset].equals(entry.getKey()) &&
          Objects.equals(kv[offset + 1], entry.getValue());
    }


    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return kv[offset].hashCode() ^ Objects.hashCode(kv[offset + 1]);
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
      return kv[offset] + "=" + kv[offset + 1];
    }
  }




  /**
   * A {@link Spliterator} over the entries of the enclosing {@link SortedStringMap}, providing
   * ordered, sized, and non-splitting traversal.
   */
  private final class EntrySpliterator implements Spliterator<Entry<String,V>>
  {
    int index = 0;


    private EntrySpliterator() {
    }


    /** {@inheritDoc} */
    @Override
    public boolean tryAdvance(Consumer<? super Entry<String,V>> action)
    {
      if (index >= size)
        return false;

      action.accept(entry(index++));

      return true;
    }


    /**
     * This spliterator does not support splitting.
     *
     * @return  always {@code null}
     */
    @Override
    public Spliterator<Entry<String,V>> trySplit() {
      return null;
    }


    /** {@inheritDoc} */
    @Override
    public long estimateSize() {
      return size;
    }


    /** {@inheritDoc} */
    @Override
    public Comparator<? super Entry<String,V>> getComparator() {
      return null;
    }


    /** {@inheritDoc} */
    @Override
    public int characteristics() {
      return ORDERED | DISTINCT | NONNULL | SIZED | IMMUTABLE;
    }
  }




  /**
   * An {@link Iterator} over the entries of the enclosing {@link SortedStringMap}, traversing entries in sorted
   * key order.
   */
  private final class EntryIterator implements Iterator<Entry<String,V>>
  {
    int index = 0;


    private EntryIterator() {
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
      return index < size;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Entry<String,V> next()
    {
      if (!hasNext())
        throw new NoSuchElementException();

      return entry(index++);
    }
  }
}
