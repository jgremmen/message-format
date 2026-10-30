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
 * This formatter uses the {@code entry} configuration key to select which part of the map entry to format.
 * <p>
 * If the configuration key is absent or does not match a known option, formatting is delegated to the next available
 * formatter.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the map entry formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code entry}</td>
 *     <td>{@code key}, {@code value}</td>
 *     <td>(none)</td>
 *     <td>
 *       Part of the entry to format, using the formatter appropriate for its type. If absent or any other value,
 *       formatting is delegated to the next formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * Format the key and the value of an entry independently:
 * <pre>{@code
 * var entry = Map.entry("score", 42);
 *
 * messageSupport
 *     .message("%{e,entry:key} is %{e,entry:value}")
 *     .with("e", entry)
 *     .format();
 * // "score is 42"
 * }</pre>
 * <p>
 * Map keys and configuration keys apply to the selected part:
 * <pre>{@code
 * messageSupport
 *     .message("%{e,entry:value,true:'enabled',false:'disabled'}")
 *     .with("e", Map.entry("active", true))
 *     .format();
 * // "enabled"
 * }</pre>
 * <p>
 * Format the elements of an entry set individually:
 * <pre>{@code
 * var limits = new TreeMap<>(Map.of("cpu", 4, "memory", 16));
 *
 * messageSupport
 *     .message("%{limits,list-value:'%{value,entry:key} (%{value,entry:value})'}")
 *     .with("limits", limits.entrySet())
 *     .format();
 * // "cpu (4), memory (16)"
 * }</pre>
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
   * <p>
   * Adds the {@code map-entry} classifier. If the {@code entry} configuration key is set to {@code key} or
   * {@code value}, classifier resolution is delegated to the entry's key or value respectively.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object entry)
  {
    context.addClassifier("map-entry");

    switch(context.getConfigValueString("entry").orElse(""))
    {
      case "key" -> context.updateClassifiers(((Entry<?,?>)entry).getKey());
      case "value" -> context.updateClassifiers(((Entry<?,?>)entry).getValue());

      default -> {
        return false;
      }
    }

    return true;
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
