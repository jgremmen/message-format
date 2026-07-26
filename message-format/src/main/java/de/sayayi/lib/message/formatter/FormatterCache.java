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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static java.lang.System.arraycopy;
import static java.util.Arrays.fill;


/**
 * The formatter cache is a fixed size cache for storing a sorted list of parameter formatters for each value type.
 * <p>
 * The cache prioritizes frequently used value types and drops the least used ones as soon as the cache size is
 * exhausted.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
final class FormatterCache
{
  private final Lock lock = new ReentrantLock();

  private final int capacity;
  private final Object[] typeFormatters;  // 2 * n = type, 2 * n + 1 = node

  private int typeCount;
  private int modCount;

  private Node head;
  private Node tail;


  /**
   * Creates a new formatter cache with the given capacity. The effective capacity is at least 8.
   *
   * @param n  desired cache capacity
   */
  FormatterCache(int n)
  {
    capacity = Math.max(n, 8);
    typeFormatters = new Object[capacity * 2];

    clear();
  }


  /**
   * Removes all entries from this cache.
   */
  void clear()
  {
    lock.lock();
    try {
      fill(typeFormatters, null);

      head = null;
      tail = null;

      typeCount = 0;
      modCount++;
    } finally {
      lock.unlock();
    }
  }


  /**
   * Looks up the parameter formatters for the given {@code type}. If the type is not yet cached, the
   * {@code buildFormatters} function is invoked to create the formatter list and the result is added to the cache.
   * <p>
   * The {@code buildFormatters} function is invoked outside the lock to avoid blocking other threads during
   * potentially expensive formatter construction. A modification counter is used to skip the redundant type lookup
   * on re-entry when no concurrent modification occurred.
   *
   * @param type             value type to look up formatters for, not {@code null}
   * @param buildFormatters  function to build the formatter list if not cached, not {@code null}
   *
   * @return  cached or newly built parameter formatters for the given type, never {@code null}
   */
  @NotNull ParameterFormatter[] lookup(@NotNull Class<?> type,
                                       @NotNull Function<Class<?>,ParameterFormatter[]> buildFormatters)
  {
    final int _modCount;
    int idx;

    lock.lock();
    try {
      if ((idx = findTypeIndex(type)) >= 0)
      {
        final var node = (Node)typeFormatters[idx * 2 + 1];

        // move to head?
        // start moving if we've reached 75% of the total capacity and the node is located in the lower 25%
        if (node != head && typeCount >= capacity * 3 / 4 && node.countNext < typeCount / 4)
          moveNodeToHead(node);

        return node.formatters;
      }

      _modCount = modCount;
    } finally {
      lock.unlock();
    }

    // build formatters outside the lock to avoid blocking other threads
    final var formatters = buildFormatters.apply(type);

    lock.lock();
    try {
      if (modCount != _modCount && (idx = findTypeIndex(type)) >= 0)
        return ((Node)typeFormatters[idx * 2 + 1]).formatters;

      addNew(type, formatters);

      return formatters;
    } finally {
      lock.unlock();
    }
  }


  /**
   * Moves the given node to the head of the linked list, promoting it as the most recently accessed entry.
   *
   * @param node  the node to promote, not {@code null}
   */
  private void moveNodeToHead(@NotNull Node node)
  {
    final var prevNode = node.prev;
    assert prevNode != null;

    for(var n = prevNode; n != null; n = n.prev)
      n.countNext--;

    prevNode.next = node.next;

    if (node == tail)
      tail = prevNode;
    else
      node.next.prev = prevNode;

    node.prev = null;
    node.next = head;
    node.countNext = head.countNext + 1;

    head.prev = node;
    head = node;
  }


  /**
   * Adds a new type-to-formatters mapping to the cache. If the cache is at full capacity, the least recently used
   * entry (tail) is evicted first.
   *
   * @param type        the value type to cache, not {@code null}
   * @param formatters  the formatter list for the type, not {@code null}
   */
  private void addNew(@NotNull Class<?> type, @NotNull ParameterFormatter[] formatters)
  {
    // if capacity has been reached -> remove tail
    if (typeCount == capacity)
    {
      final var typeOffset = findTypeIndex(tail.type) * 2;

      arraycopy(typeFormatters, typeOffset + 2, typeFormatters, typeOffset,
          (capacity - 1) * 2 - typeOffset);
      typeCount--;

      final var prevNode = tail.prev;
      assert prevNode != null;

      for(var n = prevNode; n != null; n = n.prev)
        n.countNext--;

      prevNode.next = null;
      tail = prevNode;
    }

    final var node = new Node(type, formatters);
    final int insertOffset;

    if (head != null)
    {
      insertOffset = (-findTypeIndex(type) - 1) * 2;

      arraycopy(typeFormatters, insertOffset, typeFormatters, insertOffset + 2,
          typeCount * 2 - insertOffset);

      node.next = head;
      node.countNext = head.countNext + 1;
      head.prev = node;
    }
    else
    {
      insertOffset = 0;
      tail = node;
    }

    typeFormatters[insertOffset] = type;
    typeFormatters[insertOffset + 1] = node;
    typeCount++;
    modCount++;

    head = node;
  }


  /**
   * Performs a binary search for the given type in the sorted type array.
   *
   * @param type  the type to search for, not {@code null}
   *
   * @return  the index if found, or {@code -(insertion point) - 1} if not found
   */
  private int findTypeIndex(@NotNull Class<?> type)
  {
    final var typeName = type.getName();
    var low = 0;
    var high = typeCount - 1;

    while(low <= high)
    {
      final var mid = (low + high) >>> 1;
      final var midClass = (Class<?>)typeFormatters[mid * 2];

      if (midClass == type)
        return mid;

      final var midValCmp = midClass.getName().compareTo(typeName);

      if (midValCmp < 0)
        low = mid + 1;
      else if (midValCmp > 0)
        high = mid - 1;
    }

    return -(low + 1);  // type not found
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    lock.lock();
    try {
      final var s = new StringJoiner(", ", "[", "]");

      for(var n = head; n != null; n = n.next)
        s.add(n.toString());

      return s.toString();
    } finally {
      lock.unlock();
    }
  }




  /**
   * Doubly-linked list node holding the cached formatter list for a single value type.
   */
  private static final class Node
  {
    private final Class<?> type;
    private final ParameterFormatter[] formatters;

    private Node prev;
    private Node next;
    private int countNext;


    private Node(@NotNull Class<?> type, @NotNull ParameterFormatter[] formatters)
    {
      this.type = type;
      this.formatters = formatters;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
      final var name = type.getCanonicalName();

      return countNext + ":(" + (name == null ? type.toString() : name) + ')';
    }
  }
}
