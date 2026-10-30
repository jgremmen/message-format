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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractMultiSelectFormatter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.jar.Manifest;


/**
 * Parameter formatter for {@link Manifest} values.
 * <p>
 * This formatter uses the {@code manifest} configuration key to select which part of the manifest to format.
 * <p>
 * If the configuration value does not match a known option, formatting is delegated to the next available formatter.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the manifest formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code manifest}</td>
 *     <td>{@code main-attrs}, {@code main-attributes}, {@code entries}</td>
 *     <td>{@code main-attrs}</td>
 *     <td>
 *       Formats the main attributes or the entries of the manifest, both of which are maps. Any other value
 *       delegates to the next formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * The configuration keys of the {@link de.sayayi.lib.message.formatter.parameter.runtime.MapFormatter MapFormatter}
 * apply to the selected part.
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is not part of the {@link de.sayayi.lib.message.formatter.DefaultFormatterService
 * DefaultFormatterService} and must be registered explicitly:
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new ManifestFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * Format the main attributes, using the configuration keys of the map formatter:
 * <pre>{@code
 * var manifest = new Manifest();
 * manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
 * manifest.getMainAttributes().putValue("Implementation-Title", "demo");
 *
 * messageSupport
 *     .message("%{m,map-kv:'%{key}: %{value}',list-sep:'; '}")
 *     .with("m", manifest)
 *     .format();
 * // "Manifest-Version: 1.0; Implementation-Title: demo"
 * }</pre>
 * <p>
 * List the names of the manifest entries:
 * <pre>{@code
 * var manifest = new Manifest();
 * manifest.getEntries().put("de/sayayi/Main.class", new Attributes());
 *
 * messageSupport
 *     .message("Signed: %{m,manifest:entries,map-kv:'%{key}'}")
 *     .with("m", manifest)
 *     .format();
 * // "Signed: de/sayayi/Main.class"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class ManifestFormatter extends AbstractMultiSelectFormatter<Manifest>
{
  /**
   * Creates a new manifest formatter with the configuration key {@code manifest} and selection options for main
   * attributes and entries.
   */
  public ManifestFormatter()
  {
    super("manifest", "main-attrs" , true);

    register(new String[] { "main-attrs", "main-attributes" },
        (context,manifest) -> context.format(manifest.getMainAttributes()));
    register("entries",
        (context,manifest) -> context.format(manifest.getEntries()));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link Manifest} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Manifest.class));
  }
}
