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
package de.sayayi.lib.message.formatter;

import lombok.Getter;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("squid:S2160")
public class FixedSizeCacheMap<K,V> extends AbstractMap<K,V>
{
  private final Comparator<Link<K,V>> comparator;
  private final Link<K,V> meru;
  private final int maxSize;

  private Link<K,V>[] entries;
  private int size;

  private EntrySet entrySet;
  private KeySet cacheKeySet;
  private int modCount;


  @SuppressWarnings("unused")
  public FixedSizeCacheMap(int maxSize) {
    this(null, maxSize);
  }


  @SuppressWarnings("WeakerAccess")
  public FixedSizeCacheMap(Comparator<K> comparator, int maxSize)
  {
    if (maxSize < 2)
      throw new IllegalArgumentException("maxSize must be at least 2");

    this.comparator = new LinkSorter<K,V>(comparator);
    this.maxSize = maxSize;

    //noinspection unchecked
    entries = (Link<K,V>[])new Link[Math.min(16, maxSize)];
    meru = new Link<K,V>(null);

    clear();
  }


  @SuppressWarnings("squid:S3776")
  @Override
  public V put(K key, V value)
  {
    Link<K,V> link = new Link<K,V>(key);
    int idx = Arrays.binarySearch(entries, 0, size, link, comparator);
    V oldValue = null;

    if (idx >= 0)
    {
      // same key, replace value
      link = entries[idx];
      putFirst(link);

      oldValue = link.value;
      link.value = value;
    }
    else
    {
      final int pos = -(idx + 1);

      if (size < maxSize)
      {
        ensureCapacity();

        if (pos < size)
          System.arraycopy(entries, pos, entries, pos + 1, size - pos);

        link.value = value;
        link.previous = meru;
        link.next = meru.next;

        meru.next.previous = link;
        meru.next = link;

        entries[pos] = link;
        size++;
      }
      else
      {
        link = meru.previous;
        oldValue = link.value;
        idx = Arrays.binarySearch(entries, 0, size, link, comparator);

        // remove tail element
        meru.previous = link.previous;
        link.previous.next = meru;

        // recycle removed element and insert it at head
        link.previous = meru;
        link.next = meru.next;
        meru.next.previous = link;
        meru.next = link;

        if (idx > pos)
        {
          System.arraycopy(entries, pos, entries, pos + 1, idx - pos);
          entries[pos] = link;
        }
        else if (pos - idx > 1)
        {
          System.arraycopy(entries, idx + 1, entries, idx, (pos == size) ? pos - 1 - idx : pos - idx);
          entries[pos - 1] = link;
        }

        link.key = key;
        link.value = value;
      }

      modCount++;
    }

    return oldValue;
  }


  private void ensureCapacity()
  {
    if (size == entries.length)
    {
      int newSize = Math.min(entries.length * 3 / 2, maxSize);
      Link[] newEntries = new Link[newSize];

      System.arraycopy(entries, 0, newEntries, 0, size);
      //noinspection unchecked
      entries = (Link<K,V>[])newEntries;
    }
  }


  private V remove0(Link<K,V> kv)
  {
    int idx = Arrays.binarySearch(entries, 0, size, kv, comparator);
    if (idx < 0)
      return null;

    kv = entries[idx];

    // remove from chain
    kv.previous.next = kv.next;
    kv.next.previous = kv.previous;

    // remove from sorted entry list
    if (idx < size - 1)
      System.arraycopy(entries, idx + 1, entries, idx, size - idx - 1);

    entries[--size] = null;

    modCount++;

    return kv.value;
  }


  @SuppressWarnings("unchecked")
  @Override
  public V remove(Object key)
  {
    try {
      return remove0(new Link<K,V>((K)key));
    } catch(ClassCastException ex) {
      return null;
    }
  }


  @SuppressWarnings("unchecked")
  @Override
  public boolean containsKey(Object key) {
    return findEntry((K)key) != null;
  }


  @SuppressWarnings("unchecked")
  @Override
  public V get(Object key)
  {
    Link<K,V> link = findEntry((K)key);
    if (link == null)
      return null;

    putFirst(link);

    return link.value;
  }


  private Link<K,V> findEntry(K key)
  {
    int idx = Arrays.binarySearch(entries, 0, size, new Link<K,V>(key), comparator);
    return (idx >= 0) ? entries[idx] : null;
  }


  private void putFirst(Link<K,V> link)
  {
    if (meru.next != link)
    {
      // remove from chain
      link.previous.next = link.next;
      link.next.previous = link.previous;

      // insert at the beginning
      link.previous = meru;
      link.next = meru.next;
      meru.next.previous = link;
      meru.next = link;

      modCount++;
    }
  }


  @Override
  public void clear()
  {
    meru.previous = meru;
    meru.next = meru;

    size = 0;
    modCount++;
  }


  @NotNull
  @Synchronized
  @Override
  public Set<K> keySet()
  {
    if (cacheKeySet == null)
      cacheKeySet = new KeySet();

    return cacheKeySet;
  }


  final class KeySet extends AbstractSet<K>
  {
    public final int size() {
      return size;
    }

    public final void clear() {
      FixedSizeCacheMap.this.clear();
    }

    public final Iterator<K> iterator() {
      return new KeyIterator();
    }

    public final boolean contains(Object o) {
      return containsKey(o);
    }

    public final boolean remove(Object key) {
      return FixedSizeCacheMap.this.remove(key) != null;
    }
  }


  @NotNull
  @Synchronized
  public Set<Entry<K,V>> entrySet()
  {
    if (entrySet == null)
      entrySet = new EntrySet();

    return entrySet;
  }


  final class EntrySet extends AbstractSet<Entry<K,V>>
  {
    public final int size() {
      return size;
    }

    public final void clear() {
      FixedSizeCacheMap.this.clear();
    }

    public final Iterator<Entry<K,V>> iterator() {
      return new EntryIterator();
    }

    @SuppressWarnings("unchecked")
    public final boolean contains(Object o) {
      return (o instanceof FixedSizeCacheMap.Link) && findEntry(((Link<K,V>)o).key) == o;
    }

    public final boolean remove(Object o)
    {
      if (o instanceof FixedSizeCacheMap.Link)
      {
        //noinspection unchecked
        Link<K,V> e = (Link<K,V>)o;
        return remove0(e) != null;
      }

      return false;
    }
  }


  final class EntryIterator extends BaseIterator implements Iterator<Map.Entry<K,V>>
  {
    @SuppressWarnings("squid:S2272")
    public final Map.Entry<K,V> next() {
      return nextNode();
    }
  }


  final class KeyIterator extends BaseIterator implements Iterator<K>
  {
    @SuppressWarnings("squid:S2272")
    public final K next() {
      return nextNode().key;
    }
  }


  final class ValueIterator extends BaseIterator implements Iterator<V>
  {
    @SuppressWarnings("squid:S2272")
    public final V next() {
      return nextNode().value;
    }
  }


  abstract class BaseIterator
  {
    Link<K,V> next;
    Link<K,V> current;
    int expectedModCount;

    BaseIterator()
    {
      expectedModCount = modCount;
      current = null;
      next = meru.next;
    }


    @SuppressWarnings("WeakerAccess")
    public final boolean hasNext() {
      return next != meru;
    }


    final Link<K,V> nextNode()
    {
      if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
      if (!hasNext())
        throw new NoSuchElementException();

      current = next;
      next = next.next;

      return current;
    }


    public final void remove()
    {
      if (current == null)
        throw new IllegalStateException();
      if (modCount != expectedModCount)
        throw new ConcurrentModificationException();

      FixedSizeCacheMap.this.remove(current);
      current = null;

      expectedModCount = modCount;
    }
  }


  private static class Link<K,V> implements Map.Entry<K,V>
  {
    @Getter K key;
    @Getter V value;

    Link<K,V> previous;
    Link<K,V> next;


    Link(K key) {
      this.key = key;
    }


    @Override
    public V setValue(V value)
    {
      V oldValue = this.value;
      this.value = value;

      return oldValue;
    }


    @Override
    public int hashCode() {
      return key.hashCode();
    }


    @Override
    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      if (!(o instanceof FixedSizeCacheMap.Link))
        return false;

      //noinspection unchecked
      Link<K,V> that = (Link<K,V>)o;

      return key.equals(that.key) && ((value == null) ? that.value == null : value.equals(that.value));
    }


    @Override
    public String toString() {
      return String.valueOf(key) + '=' + value;
    }
  }


  private static class LinkSorter<K,V> implements Comparator<Link<K,V>>
  {
    private final Comparator<K> comparator;


    LinkSorter(Comparator<K> comparator) {
      this.comparator = comparator;
    }


    @SuppressWarnings("unchecked")
    @Override
    public int compare(Link<K,V> o1, Link<K,V> o2) {
      return (comparator == null) ? ((Comparable<K>)o1.key).compareTo(o2.key) : comparator.compare(o1.key, o2.key);
    }
  }
}
