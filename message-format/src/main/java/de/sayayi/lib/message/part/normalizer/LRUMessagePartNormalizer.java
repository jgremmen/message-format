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

import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;
import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 * @since 0.6.0
 */
public final class LRUMessagePartNormalizer implements MessagePartNormalizer
{
  private final int maxSize;
  private MessagePart[] parts;

  private int size;


  public LRUMessagePartNormalizer(int maxSize)
  {
    if (maxSize <= 0)
      throw new IllegalArgumentException("maxSize must be a positive number");

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

    int n = -1;
    MessagePart cachedPart = null;

    if (parts != null)
      for(int i = 0; i < size; i++)
        if ((cachedPart = parts[i]).equals(part))
        {
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
          : copyOf(parts, min(Math.max(size * 2, 8), maxSize));
    }
  }
}
