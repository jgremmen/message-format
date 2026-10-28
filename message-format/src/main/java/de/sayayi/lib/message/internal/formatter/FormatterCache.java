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
package de.sayayi.lib.message.internal.formatter;

import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static java.lang.System.arraycopy;
import static java.util.Arrays.fill;


/**
 * The formatter cache is a fixed size cache for storing a sorted list of parameter formatters for each value type.
 * <p>
 * The cache prioritizes frequently used value types and evicts entries using an approximate (recency-based)
 * least-recently-used policy as soon as the cache size is exhausted.
 * <p>
 * <b>Thread safety:</b> this class is thread-safe. Structural mutation is protected by a lock, builder functions run
 * outside the lock so that cache misses do not block other threads, and a modification-count check guards against
 * corruption from re-entrant access. Note that it is not single-flight: concurrent cache misses for the same type
 * may each independently build a formatter list.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@ApiStatus.Internal
public final class FormatterCache
{
  /** Sequence for assigning unique ids to types. */
  private static final AtomicLong TYPE_ID_SEQUENCE = new AtomicLong();

  /**
   * Unique id per type, used to order different types with the same class name (e.g. loaded by different class
   * loaders). {@code ClassValue} guarantees a single id per type and does not prevent the type from being unloaded.
   */
  private static final ClassValue<Long> TYPE_ID = new ClassValue<>() {
    @Override
    protected Long computeValue(@NotNull Class<?> type) {
      return TYPE_ID_SEQUENCE.incrementAndGet();
    }
  };

  /** Lock guarding all structural modifications of this cache. */
  private final Lock lock = new ReentrantLock();

  /** Maximum number of cached types. */
  private final int capacity;

  /**
   * Types and their list nodes, sorted by type for binary search: index {@code 2n} holds the type and index
   * {@code 2n + 1} the associated {@link Node}.
   */
  private final Object[] typeFormatters;  // 2 * n = type, 2 * n + 1 = node

  /** Number of cached types. */
  private int typeCount;
  /** Modification counter, incremented on each structural change. */
  private int modCount;

  /** Most recently promoted node, or {@code null} if the cache is empty. */
  private Node head;
  /** Least recently promoted node, which is evicted first, or {@code null} if the cache is empty. */
  private Node tail;


  /**
   * Creates a new formatter cache with the given capacity. The effective capacity is at least 8.
   *
   * @param n  desired cache capacity
   */
  public FormatterCache(int n)
  {
    capacity = Math.max(n, 8);
    typeFormatters = new Object[capacity * 2];

    clear();
  }


  /**
   * Removes all entries from this cache.
   */
  public void clear()
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
  public @NotNull ParameterFormatter[] lookup(@NotNull Class<?> type,
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
   *
   * @see #compareTypes(Class, Class)
   */
  private int findTypeIndex(@NotNull Class<?> type)
  {
    var low = 0;
    var high = typeCount - 1;

    while(low <= high)
    {
      final var mid = (low + high) >>> 1;
      final var cmp = compareTypes((Class<?>)typeFormatters[mid * 2], type);

      if (cmp < 0)
        low = mid + 1;
      else if (cmp > 0)
        high = mid - 1;
      else
        return mid;
    }

    return -(low + 1);  // type not found
  }


  /**
   * Compares two types by class name. Different types with the same class name (e.g. loaded by different class
   * loaders) are ordered by their unique type id. Hence, this comparison returns {@code 0} if, and only if, both types
   * are identical.
   *
   * @param type1  first type, not {@code null}
   * @param type2  second type, not {@code null}
   *
   * @return  a negative value, zero or a positive value if {@code type1} is less than, equal to or greater than
   *          {@code type2}
   *
   * @since 0.25.0
   */
  private static int compareTypes(@NotNull Class<?> type1, @NotNull Class<?> type2)
  {
    if (type1 == type2)
      return 0;

    final var cmp = type1.getName().compareTo(type2.getName());

    return cmp != 0 ? cmp : Long.compare(TYPE_ID.get(type1), TYPE_ID.get(type2));
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
   *
   * @since 0.8.0
   */
  private static final class Node
  {
    /** Cached value type. */
    private final Class<?> type;

    /** Sorted parameter formatters for {@link #type}. */
    private final ParameterFormatter[] formatters;

    /** Previous (more recently promoted) node, or {@code null} for the head node. */
    private Node prev;

    /** Next (less recently promoted) node, or {@code null} for the tail node. */
    private Node next;

    /** Number of nodes following this node in the list. */
    private int countNext;


    /**
     * Creates a new list node for the given type and formatters.
     *
     * @param type        cached value type, not {@code null}
     * @param formatters  sorted parameter formatters for {@code type}, not {@code null}
     */
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
