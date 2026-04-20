/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.part.normalizer;

import de.sayayi.lib.message.part.MessagePart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Math.clamp;
import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;


/**
 * Factory for creating LRU (Least Recently Used) based {@link MessagePartNormalizer} instances.
 * <p>
 * The normalizer deduplicates {@link MessagePart} instances by maintaining a bounded cache. When a part equal to a
 * previously cached part is normalized, the cached instance is returned, reducing the overall memory footprint.
 * <p>
 * Depending on the requested cache size, the factory selects an implementation optimized for either small or large
 * numbers of entries.
 *
 * @author Jeroen Gremmen
 * @since 0.6.0
 *
 * @see MessagePartNormalizer
 */
public final class LRUMessagePartNormalizer
{
  private LRUMessagePartNormalizer() {
  }


  /**
   * Create an LRU-based message part normalizer with the given maximum cache size.
   *
   * @param maxSize  maximum number of cached parts, must be positive
   *
   * @return  message part normalizer instance, never {@code null}
   *
   * @throws IllegalArgumentException if {@code maxSize} is not positive
   *
   * @since 0.21.0
   */
  @Contract(pure = true)
  public static @NotNull MessagePartNormalizer create(int maxSize)
  {
    if (maxSize <= 0)
      throw new IllegalArgumentException("maxSize must be a positive number");

    return maxSize <= 64 ? new Small(maxSize) : new Large(maxSize);
  }




  /**
   * Array-based LRU normalizer optimized for small cache sizes. Provides excellent CPU cache locality for fast
   * linear scans over a limited number of entries.
   */
  private static final class Small implements MessagePartNormalizer
  {
    private final int maxSize;
    private MessagePart[] parts;
    private int size;


    private Small(int maxSize)
    {
      this.maxSize = maxSize;

      parts = null;
      size = 0;
    }


    @Override
    @Contract(mutates = "this")
    @SuppressWarnings("unchecked")
    public <T extends MessagePart> @NotNull T normalize(@NotNull T part)
    {
      requireNonNull(part, "part must not be null");

      var n = -1;
      MessagePart cachedPart = null;

      if (parts != null)
        for(var i = 0; i < size; i++)
          if (parts[i] == part || (cachedPart = parts[i]).equals(part))
          {
            if (cachedPart == null)
              cachedPart = parts[i];
            n = i;
            break;
          }

      if (n != 0)
      {
        if (n == -1)
        {
          ensureSize();

          arraycopy(parts, 0, parts, 1, min(maxSize - 1, size));
          cachedPart = part;

          if (size < maxSize)
            size++;
        }
        else
          arraycopy(parts, 0, parts, 1, n);

        parts[0] = cachedPart;
      }

      return (T)cachedPart;
    }


    private void ensureSize()
    {
      if (parts == null || (parts.length == size && size < maxSize))
      {
        parts = parts == null
            ? new MessagePart[min(8, maxSize)]
            : copyOf(parts, clamp(size * 2L, 8, maxSize));
      }
    }
  }




  /**
   * {@link LinkedHashMap}-based LRU normalizer suitable for larger cache sizes. Provides constant-time lookup and
   * automatic eviction of the least recently used entry.
   */
  @SuppressWarnings("ClassCanBeRecord")
  private static final class Large implements MessagePartNormalizer
  {
    private final LinkedHashMap<MessagePart,MessagePart> cache;


    private Large(int maxSize)
    {
      cache = new LinkedHashMap<>(clamp(maxSize, 16, 64), 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<MessagePart,MessagePart> eldest) {
          return size() > maxSize;
        }
      };
    }


    @Override
    @Contract(mutates = "this")
    @SuppressWarnings("unchecked")
    public <T extends MessagePart> @NotNull T normalize(@NotNull T part) {
      return (T)cache.computeIfAbsent(requireNonNull(part, "part must not be null"), identity());
    }
  }
}
