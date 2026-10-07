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
package de.sayayi.lib.message.internal.formatter;

import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;


/**
 * A prioritized wrapper around a {@link ParameterFormatter} that is used for ordering formatters by their
 * registration priority.
 *
 * @param order     the priority order (lower values have higher priority)
 * @param formatter the wrapped parameter formatter, not {@code null}
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@ApiStatus.Internal
public record PrioritizedParameterFormatter(int order, @NotNull ParameterFormatter formatter)
    implements Comparable<PrioritizedParameterFormatter>
{
  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(@NotNull PrioritizedParameterFormatter o)
  {
    var cmp = Integer.compare(order, o.order);
    if (cmp == 0)
      cmp = formatter.getClass().getName().compareTo(o.formatter.getClass().getName());

    return cmp;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String toString() {
    return "PrioritizedFormatter(order=" + order + ",formatter=" + formatter + ')';
  }
}
