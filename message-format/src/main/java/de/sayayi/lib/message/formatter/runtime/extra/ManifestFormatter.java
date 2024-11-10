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
package de.sayayi.lib.message.formatter.runtime.extra;

import de.sayayi.lib.message.formatter.AbstractMultiSelectFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.jar.Manifest;


/**
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class ManifestFormatter extends AbstractMultiSelectFormatter<Manifest>
{
  public ManifestFormatter()
  {
    super("manifest", "main-attrs" , true);

    register(new String[] { "main-attrs", "main-attributes" },
        (context,manifest) -> context.format(manifest.getMainAttributes(), true));
    register("entries",
        (context,manifest) -> context.format(manifest.getEntries(), true));
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Manifest.class));
  }
}
