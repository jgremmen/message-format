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
import static java.util.Objects.requireNonNull;


/**
 * The formatter cache is a fixed size cache for storing a sorted list of parameter formatters for each value type.
 * <p>
 * The cache prioritizes frequently used value types and evicts entries using an approximate (recency-based)
 * least-recently-used policy as soon as the cache size is exhausted.
 * <p>
 * Cached types are identified by their class name and a unique type id; the cache keeps no reference to the
 * {@code Class} objects themselves. A cached type, and the class loader that loaded it, can therefore be unloaded
 * (e.g. after redeploying an application while this library is loaded by a parent class loader). The cache entry of an
 * unloaded type is never matched again and is eventually evicted.
 * <p>
 * <b>Thread safety:</b> this class is thread-safe. Structural mutation is protected by a lock, builder functions run
 * outside the lock so that cache misses do not block other threads, and a modification-count check guards against
 * corruption from re-entrant access. Note that it is not single-flight: concurrent cache misses for the same type may
 * each independently build a formatter list.
 * <p>
 * If {@link #clear()} runs while a formatter list is being built, the list built before the clear is still added to the
 * cache. Callers that clear the cache because the formatters have changed must therefore make sure that {@code clear()}
 * does not run concurrently with {@link #lookup(Class, Function) lookup}.
 * {@link de.sayayi.lib.message.formatter.GenericFormatterService GenericFormatterService} ensures this by calling
 * {@code lookup} under its read lock and {@code clear} under its write lock.
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
   * Unique id per type, used to distinguish different types with the same class name (e.g. loaded by different class
   * loaders). {@code ClassValue} guarantees a single id per type and does not prevent the type from being unloaded. Ids
   * start at 1 and are never reused, so the id of an unloaded type is never matched again.
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

  /** List nodes of the cached types, sorted by type name and type id for binary search. */
  private final TypeFormatterNode[] typeFormatterNodes;

  /** Number of cached types. */
  private int typeCount;

  /** Modification counter, incremented on each structural change. */
  private int modCount;

  /** Most recently promoted node, or {@code null} if the cache is empty. */
  private TypeFormatterNode head;

  /** Least recently promoted node, which is evicted first, or {@code null} if the cache is empty. */
  private TypeFormatterNode tail;


  /**
   * Creates a new formatter cache with the given capacity. The effective capacity is at least 8.
   *
   * @param n  desired cache capacity
   */
  public FormatterCache(int n)
  {
    capacity = Math.max(n, 8);
    typeFormatterNodes = new TypeFormatterNode[capacity];

    clear();
  }


  /**
   * Removes all entries from this cache.
   */
  public void clear()
  {
    lock.lock();
    try {
      fill(typeFormatterNodes, null);

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
   * The {@code buildFormatters} function is invoked outside the lock to avoid blocking other threads during potentially
   * expensive formatter construction. A modification counter is used to skip the redundant type lookup on re-entry when
   * no concurrent modification occurred.
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
        final var node = typeFormatterNodes[idx];

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
        return typeFormatterNodes[idx].formatters;

      addNew(type, formatters);

      return formatters;
    } finally {
      lock.unlock();
    }
  }


  /**
   * Moves the given node to the head of the linked list, promoting it as the most recently accessed entry.
   *
   * @param typeFormatterNode  the node to promote, not {@code null}
   */
  private void moveNodeToHead(@NotNull FormatterCache.TypeFormatterNode typeFormatterNode)
  {
    final var prevNode = typeFormatterNode.prev;
    assert prevNode != null;

    for(var n = prevNode; n != null; n = n.prev)
      n.countNext--;

    prevNode.next = typeFormatterNode.next;

    if (typeFormatterNode == tail)
      tail = prevNode;
    else
      typeFormatterNode.next.prev = prevNode;

    typeFormatterNode.prev = null;
    typeFormatterNode.next = head;
    typeFormatterNode.countNext = head.countNext + 1;

    head.prev = typeFormatterNode;
    head = typeFormatterNode;
  }


  /**
   * Adds a new type-to-formatters mapping to the cache. If the cache is at full capacity, the least recently used entry
   * (tail) is evicted first.
   *
   * @param type        the value type to cache, not {@code null}
   * @param formatters  the formatter list for the type, not {@code null}
   */
  private void addNew(@NotNull Class<?> type, @NotNull ParameterFormatter[] formatters)
  {
    // if capacity has been reached -> remove tail
    if (typeCount == capacity)
    {
      final var tailIndex = findIndex(tail.typeName, null, tail.typeId);

      arraycopy(typeFormatterNodes, tailIndex + 1, typeFormatterNodes, tailIndex, capacity - 1 - tailIndex);
      typeFormatterNodes[--typeCount] = null;

      final var prevNode = tail.prev;
      assert prevNode != null;

      for(var n = prevNode; n != null; n = n.prev)
        n.countNext--;

      prevNode.next = null;
      tail = prevNode;
    }

    final var typeFormatterNode = new TypeFormatterNode(type.getName(), TYPE_ID.get(type), formatters);
    final int insertIndex;

    if (head != null)
    {
      insertIndex = -findIndex(typeFormatterNode.typeName, null, typeFormatterNode.typeId) - 1;

      arraycopy(typeFormatterNodes, insertIndex, typeFormatterNodes, insertIndex + 1, typeCount - insertIndex);

      typeFormatterNode.next = head;
      typeFormatterNode.countNext = head.countNext + 1;
      head.prev = typeFormatterNode;
    }
    else
    {
      insertIndex = 0;
      tail = typeFormatterNode;
    }

    typeFormatterNodes[insertIndex] = typeFormatterNode;
    typeCount++;
    modCount++;

    head = typeFormatterNode;
  }


  /**
   * Performs a binary search for the given type in the sorted node array.
   *
   * @param type  the type to search for, not {@code null}
   *
   * @return  the index if found, or {@code -(insertion point) - 1} if not found
   */
  private int findTypeIndex(@NotNull Class<?> type) {
    return findIndex(type.getName(), type, 0);
  }


  /**
   * Performs a binary search for the given type name and type id in the sorted node array. Nodes are ordered by type
   * name; different types with the same name (e.g. loaded by different class loaders) are ordered by their unique type
   * id. Hence, a node matches if, and only if, it was created for the identical type.
   * <p>
   * If {@code typeId} is {@code 0}, the type id is obtained from {@code type}, but only if a node with the same type
   * name is found. This keeps the common case, where type names are unique, free of type id lookups.
   *
   * @param typeName  name of the type to search for, not {@code null}
   * @param type      type to obtain the type id from, or {@code null} if {@code typeId} is given
   * @param typeId    unique type id, or {@code 0} if it must be obtained from {@code type}
   *
   * @return  the index if found, or {@code -(insertion point) - 1} if not found
   *
   * @since 0.25.0
   */
  private int findIndex(@NotNull String typeName, Class<?> type, long typeId)
  {
    var low = 0;
    var high = typeCount - 1;

    while(low <= high)
    {
      final var mid = (low + high) >>> 1;
      final var typeFormatterNode = typeFormatterNodes[mid];
      var cmp = typeFormatterNode.typeName.compareTo(typeName);

      if (cmp == 0)
      {
        if (typeId == 0)
          typeId = TYPE_ID.get(requireNonNull(type));

        cmp = Long.compare(typeFormatterNode.typeId, typeId);
      }

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
   * Doubly-linked list node holding the cached formatter list for a single value type. The node identifies the type by
   * its name and unique type id and holds no reference to the type itself, so the type can be unloaded.
   *
   * @since 0.8.0
   */
  private static final class TypeFormatterNode
  {
    /** Class name of the cached value type. */
    private final String typeName;

    /** Unique id of the cached value type, see {@link FormatterCache#TYPE_ID}. */
    private final long typeId;

    /** Sorted parameter formatters for the cached value type. */
    private final ParameterFormatter[] formatters;

    /** Previous (more recently promoted) node, or {@code null} for the head node. */
    private TypeFormatterNode prev;

    /** Next (less recently promoted) node, or {@code null} for the tail node. */
    private TypeFormatterNode next;

    /** Number of nodes following this node in the list. */
    private int countNext;


    /**
     * Creates a new list node for the given type and formatters.
     *
     * @param typeName    class name of the cached value type, not {@code null}
     * @param typeId      unique id of the cached value type
     * @param formatters  sorted parameter formatters for the cached value type, not {@code null}
     */
    private TypeFormatterNode(@NotNull String typeName, long typeId, @NotNull ParameterFormatter[] formatters)
    {
      this.typeName = typeName;
      this.typeId = typeId;
      this.formatters = formatters;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return countNext + ":(" + typeName + ')';
    }
  }
}
