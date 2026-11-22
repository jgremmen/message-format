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
package de.sayayi.lib.message.internal;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;


/**
 * Thread-safe cache with a maximum size, which evicts entries using an approximate least-recently-used policy.
 * <p>
 * A cache hit does not acquire any lock. Values are created outside of any lock as well, so a cache miss does not block
 * other threads. The cache is not single-flight: concurrent cache misses for the same key may each create a value, in
 * which case the value cached first is returned to all of them.
 * <p>
 * Eviction happens after a new entry has been added. As at most one thread evicts at a time and other threads do not
 * wait for it, the cache may briefly hold a few more entries than its maximum size.
 *
 * @param <K>  key type
 * @param <V>  value type
 *
 * @author Jeroen Gremmen
 * @since 0.26.0
 */
@Internal
public final class BoundedCache<K,V>
{
  /** Maximum number of cached entries. */
  private final int maxSize;

  /** Cached entries by key. */
  private final ConcurrentHashMap<K,Entry<V>> cache;

  /** Lock serializing evictions. Threads finding it locked skip eviction instead of waiting. */
  private final Lock evictionLock = new ReentrantLock();

  /**
   * Recency tick, advanced on each added entry. An added entry is stamped with the tick before advancing it and a cache
   * hit stamps the entry with the current tick, so a hit entry is always more recent than all entries added before the
   * hit and the entry with the oldest stamp is the least recently used one. As the tick changes on cache misses only,
   * cache hits do not write shared state, apart from stamping an entry once per tick.
   */
  private final AtomicInteger tick = new AtomicInteger();


  /**
   * Creates an empty cache with the given maximum size.
   *
   * @param maxSize  maximum number of cached entries, positive
   *
   * @throws IllegalArgumentException  if {@code maxSize} is not positive
   */
  public BoundedCache(int maxSize)
  {
    if (maxSize <= 0)
      throw new IllegalArgumentException("maxSize must be positive");

    this.maxSize = maxSize;

    cache = new ConcurrentHashMap<>(Math.min(maxSize, 64));
  }


  /**
   * Returns the value cached for {@code key}. If no value is cached, {@code createValue} is invoked to create the value
   * and the value is added to the cache.
   * <p>
   * {@code createValue} is invoked without holding a lock. If it throws an exception, nothing is cached and the
   * exception is relayed to the caller.
   *
   * @param key          key, not {@code null}
   * @param createValue  function creating the value for a key, not {@code null}. It must not return {@code null}
   *
   * @return  cached or newly created value, never {@code null}
   */
  public @NotNull V get(@NotNull K key, @NotNull Function<K,V> createValue)
  {
    var entry = cache.get(key);

    if (entry != null)
    {
      final var _tick = tick.get();

      // stamp the entry at most once per tick, so frequently used entries do not write to shared memory on each hit
      if (entry.lastAccess != _tick)
        entry.lastAccess = _tick;

      return entry.value;
    }

    final var newEntry = new Entry<>(createValue.apply(key), tick.getAndIncrement());

    // another thread may have cached a value for the same key in the meantime
    if ((entry = cache.putIfAbsent(key, newEntry)) != null)
      return entry.value;

    if (cache.size() > maxSize && evictionLock.tryLock())
    {
      try {
        while(cache.size() > maxSize)
          evictLeastRecentlyUsed();
      } finally {
        evictionLock.unlock();
      }
    }

    return newEntry.value;
  }


  /**
   * Removes the entry with the oldest recency stamp.
   */
  private void evictLeastRecentlyUsed()
  {
    final var _tick = tick.get();
    K lruKey = null;
    Entry<V> lruEntry = null;
    var lruAge = -1;

    for(var mapEntry: cache.entrySet())
    {
      final var entry = mapEntry.getValue();

      // tick difference instead of comparing stamps, which is robust against tick overflow
      final var age = _tick - entry.lastAccess;

      if (age > lruAge)
      {
        lruAge = age;
        lruKey = mapEntry.getKey();
        lruEntry = entry;
      }
    }

    if (lruKey != null)
      cache.remove(lruKey, lruEntry);
  }


  /**
   * Returns the number of cached entries.
   *
   * @return  number of cached entries
   */
  public int size() {
    return cache.size();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "BoundedCache(size=" + cache.size() + ",maxSize=" + maxSize + ')';
  }




  /**
   * Cached value with its recency stamp.
   *
   * @param <V>  value type
   */
  private static final class Entry<V>
  {
    /** Cached value. */
    private final V value;

    /** Recency tick of the last access, see {@link BoundedCache#tick}. */
    private volatile int lastAccess;


    /**
     * Creates a new entry.
     *
     * @param value       cached value, not {@code null}
     * @param lastAccess  recency tick of the entry creation
     */
    private Entry(@NotNull V value, int lastAccess)
    {
      this.value = value;
      this.lastAccess = lastAccess;
    }
  }
}
