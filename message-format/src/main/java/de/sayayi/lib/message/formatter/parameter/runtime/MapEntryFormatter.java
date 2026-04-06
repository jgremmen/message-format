/*
 * Copyright 2024 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractMultiSelectFormatter;
import org.jetbrains.annotations.NotNull;

import java.util.Map.Entry;
import java.util.Set;


/**
 * Parameter formatter for {@link Entry Map.Entry} values.
 * <p>
 * This formatter uses the {@code entry} configuration key to select which part of the map entry to format:
 * <ul>
 *   <li>{@code key} &ndash; formats the entry's key</li>
 *   <li>{@code value} &ndash; formats the entry's value</li>
 * </ul>
 * <p>
 * If the configuration key is absent or does not match a known option, formatting is delegated to the next available
 * formatter.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class MapEntryFormatter extends AbstractMultiSelectFormatter<Entry<?,?>>
{
  /**
   * Creates a new map entry formatter with selection options {@code key} and {@code value}, using the configuration
   * key {@code entry}.
   */
  public MapEntryFormatter()
  {
    super("entry");

    register("key", (context,entry) -> context.format(entry.getKey()));
    register("value", (context,entry) -> context.format(entry.getValue()));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link Entry Map.Entry} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Entry.class));
  }
}
