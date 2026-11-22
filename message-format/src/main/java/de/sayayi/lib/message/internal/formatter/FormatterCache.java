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
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static java.lang.System.arraycopy;
import static java.util.Objects.requireNonNull;


/**
 * The formatter cache is a fixed size cache for storing a sorted list of parameter formatters for each value type.
 * <p>
 * The cache prioritizes recently used value types and evicts entries using an approximate least-recently-used policy as
 * soon as the cache size is exhausted.
 * <p>
 * Cached types are identified by their class name and a unique type id; the cache keeps no reference to the
 * {@code Class} objects themselves. A cached type, and the class loader that loaded it, can therefore be unloaded
 * (e.g. after redeploying an application while this library is loaded by a parent class loader). The cache entry of an
 * unloaded type is never matched again and is eventually evicted.
 * <p>
 * <b>Thread safety:</b> this class is thread-safe. The cached entries are kept in an immutable array which is replaced
 * on every structural change (copy-on-write), so a cache hit does not acquire any lock. Structural changes are
 * serialized by a lock and builder functions run outside of it, so cache misses do not block other threads. Note that
 * it is not single-flight: concurrent cache misses for the same type may each independently build a formatter list.
 * <p>
 * A formatter list built while {@link #clear()} runs concurrently is returned to its caller, but it is not added to the
 * cache.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@Internal
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

  /**
   * Empty node array of an empty cache.
   *
   * @since 0.26.0
   */
  private static final TypeFormatterNode[] NO_NODES = new TypeFormatterNode[0];

  /** Lock serializing all structural modifications of this cache. Lookups of cached types do not acquire it. */
  private final Lock lock = new ReentrantLock();

  /** Maximum number of cached types. */
  private final int capacity;

  /**
   * Nodes of the cached types, sorted by type name and type id for binary search. The array is never modified once
   * published; each structural change publishes a new array.
   */
  private volatile TypeFormatterNode[] typeFormatterNodes = NO_NODES;

  /**
   * Recency tick, advanced on each added type. An added node is stamped with the tick before advancing it and a cache
   * hit stamps the node with the current tick, so a hit node is always more recent than all nodes added before the hit
   * and the node with the oldest stamp is the least recently used one. As the tick changes on cache misses only, cache
   * hits do not write shared state, apart from stamping a node once per tick.
   *
   * @since 0.26.0
   */
  private volatile int tick;

  /**
   * Generation, incremented by {@link #clear()}. A formatter list built before a clear is not cached.
   *
   * @since 0.26.0
   */
  private volatile int generation;


  /**
   * Creates a new formatter cache with the given capacity. The effective capacity is at least 8.
   *
   * @param n  desired cache capacity
   */
  public FormatterCache(int n) {
    capacity = Math.max(n, 8);
  }


  /**
   * Removes all entries from this cache.
   */
  public void clear()
  {
    lock.lock();
    try {
      typeFormatterNodes = NO_NODES;
      generation++;
    } finally {
      lock.unlock();
    }
  }


  /**
   * Looks up the parameter formatters for the given {@code type}. If the type is not yet cached, the
   * {@code buildFormatters} function is invoked to create the formatter list and the result is added to the cache.
   * <p>
   * A cached type is looked up without acquiring a lock. The {@code buildFormatters} function is invoked outside the
   * lock to avoid blocking other threads during potentially expensive formatter construction.
   *
   * @param type             value type to look up formatters for, not {@code null}
   * @param buildFormatters  function to build the formatter list if not cached, not {@code null}
   *
   * @return  cached or newly built parameter formatters for the given type, never {@code null}
   */
  public @NotNull ParameterFormatter[] lookup(@NotNull Class<?> type,
                                              @NotNull Function<Class<?>,ParameterFormatter[]> buildFormatters)
  {
    final var typeName = type.getName();
    final var nodes = typeFormatterNodes;
    var idx = findIndex(nodes, typeName, type, 0);

    if (idx >= 0)
    {
      final var node = nodes[idx];
      final var _tick = tick;

      // stamp the node at most once per tick, so frequently used types do not write to shared memory on each lookup
      if (node.lastAccess != _tick)
        node.lastAccess = _tick;

      return node.formatters;
    }

    final var _generation = generation;

    // build formatters outside the lock to avoid blocking other threads
    final var formatters = buildFormatters.apply(type);

    lock.lock();
    try {
      final var currentNodes = typeFormatterNodes;

      // another thread may have cached the type in the meantime
      if ((idx = findIndex(currentNodes, typeName, type, 0)) >= 0)
        return currentNodes[idx].formatters;

      // don't cache formatters which have been built before the cache was cleared
      if (generation == _generation)
      {
        typeFormatterNodes =
            addNew(currentNodes, new TypeFormatterNode(typeName, TYPE_ID.get(type), formatters, tick++));
      }
    } finally {
      lock.unlock();
    }

    return formatters;
  }


  /**
   * Returns a new node array containing all nodes of {@code nodes} and {@code newNode}. If the cache is at full
   * capacity, the least recently used node is left out.
   *
   * @param nodes    current, sorted node array, not {@code null}
   * @param newNode  node to add, not {@code null}
   *
   * @return  new sorted node array, never {@code null}
   *
   * @since 0.26.0
   */
  private @NotNull TypeFormatterNode[] addNew(@NotNull TypeFormatterNode[] nodes, @NotNull TypeFormatterNode newNode)
  {
    // if capacity has been reached -> leave out the least recently used node
    if (nodes.length == capacity)
    {
      final var evictIndex = findLeastRecentlyUsedIndex(nodes);
      final var remainingNodes = new TypeFormatterNode[capacity - 1];

      arraycopy(nodes, 0, remainingNodes, 0, evictIndex);
      arraycopy(nodes, evictIndex + 1, remainingNodes, evictIndex, capacity - 1 - evictIndex);

      nodes = remainingNodes;
    }

    final var insertIndex = -findIndex(nodes, newNode.typeName, null, newNode.typeId) - 1;
    final var newNodes = new TypeFormatterNode[nodes.length + 1];

    arraycopy(nodes, 0, newNodes, 0, insertIndex);
    newNodes[insertIndex] = newNode;
    arraycopy(nodes, insertIndex, newNodes, insertIndex + 1, nodes.length - insertIndex);

    return newNodes;
  }


  /**
   * Returns the index of the least recently used node, which is the node with the oldest recency stamp.
   *
   * @param nodes  node array, not {@code null} and not empty
   *
   * @return  index of the least recently used node
   *
   * @since 0.26.0
   */
  private int findLeastRecentlyUsedIndex(@NotNull TypeFormatterNode[] nodes)
  {
    final var _tick = tick;
    var lruIndex = 0;
    var lruAge = -1;

    for(int n = 0, l = nodes.length; n < l; n++)
    {
      // tick difference instead of comparing stamps, which is robust against tick overflow
      final var age = _tick - nodes[n].lastAccess;

      if (age > lruAge)
      {
        lruAge = age;
        lruIndex = n;
      }
    }

    return lruIndex;
  }


  /**
   * Performs a binary search for the given type name and type id in the sorted node array. Nodes are ordered by type
   * name; different types with the same name (e.g. loaded by different class loaders) are ordered by their unique type
   * id. Hence, a node matches if, and only if, it was created for the identical type.
   * <p>
   * If {@code typeId} is {@code 0}, the type id is obtained from {@code type}, but only if a node with the same type
   * name is found. This keeps the common case, where type names are unique, free of type id lookups.
   *
   * @param nodes     sorted node array to search, not {@code null}
   * @param typeName  name of the type to search for, not {@code null}
   * @param type      type to obtain the type id from, or {@code null} if {@code typeId} is given
   * @param typeId    unique type id, or {@code 0} if it must be obtained from {@code type}
   *
   * @return  the index if found, or {@code -(insertion point) - 1} if not found
   *
   * @since 0.25.0
   */
  private static int findIndex(@NotNull TypeFormatterNode[] nodes, @NotNull String typeName, Class<?> type, long typeId)
  {
    var low = 0;
    var high = nodes.length - 1;

    while(low <= high)
    {
      final var mid = (low + high) >>> 1;
      final var typeFormatterNode = nodes[mid];
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
    final var s = new StringJoiner(", ", "[", "]");

    for(var node: typeFormatterNodes)
      s.add(node.toString());

    return s.toString();
  }




  /**
   * Node holding the cached formatter list for a single value type. The node identifies the type by its name and unique
   * type id and holds no reference to the type itself, so the type can be unloaded.
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

    /**
     * Recency tick of the last access, see {@link FormatterCache#tick}.
     *
     * @since 0.26.0
     */
    private volatile int lastAccess;


    /**
     * Creates a new node for the given type and formatters.
     *
     * @param typeName    class name of the cached value type, not {@code null}
     * @param typeId      unique id of the cached value type
     * @param formatters  sorted parameter formatters for the cached value type, not {@code null}
     * @param lastAccess  recency tick of the node creation
     */
    private TypeFormatterNode(@NotNull String typeName, long typeId, @NotNull ParameterFormatter[] formatters,
                              int lastAccess)
    {
      this.typeName = typeName;
      this.typeId = typeId;
      this.formatters = formatters;
      this.lastAccess = lastAccess;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return lastAccess + ":(" + typeName + ')';
    }
  }
}
