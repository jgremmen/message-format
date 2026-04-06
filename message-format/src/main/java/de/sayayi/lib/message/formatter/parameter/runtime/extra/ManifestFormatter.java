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
 * This formatter uses the {@code manifest} configuration key to select which part of the manifest to format:
 * <ul>
 *   <li>
 *     {@code main-attrs} or {@code main-attributes} (default) &ndash; formats the main attributes of the manifest
 *   </li>
 *   <li>{@code entries} &ndash; formats the manifest entries map</li>
 * </ul>
 * <p>
 * If the configuration value does not match a known option, formatting is delegated to the next available formatter.
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
