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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractMultiSelectFormatter;
import org.jetbrains.annotations.NotNull;

import java.security.Key;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Key} (cryptographic key) values.
 * <p>
 * This formatter uses the {@code key} configuration key to select which aspect of the cryptographic key to format:
 * <ul>
 *   <li>{@code algorithm} &ndash; the key's algorithm name</li>
 *   <li>{@code format} &ndash; the key's encoding format name</li>
 *   <li>{@code encoded} &ndash; the key's encoded form (as a byte array)</li>
 * </ul>
 * <p>
 * If the configuration key is absent or does not match a known option, formatting is delegated to the next available
 * formatter.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class KeyFormatter extends AbstractMultiSelectFormatter<Key>
{
  /**
   * Creates a new cryptographic key formatter with the configuration key {@code key} and
   * selection options for algorithm, format and encoded form.
   */
  public KeyFormatter()
  {
    super("key");

    register("algorithm", (context,key) -> noSpaceText(key.getAlgorithm()));
    register("format", (context,key) -> noSpaceText(key.getFormat()));
    register("encoded", (context,key) -> context.format(key.getEncoded(), byte[].class));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link Key} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Key.class));
  }
}
