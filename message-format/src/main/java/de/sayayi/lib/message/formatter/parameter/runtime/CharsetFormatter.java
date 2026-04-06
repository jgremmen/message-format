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

import java.nio.charset.Charset;
import java.util.Set;


/**
 * Parameter formatter for {@link Charset} values.
 * <p>
 * This formatter uses the {@code charset} configuration key to select the output format:
 * <ul>
 *   <li>{@code display} or {@code display-name} &ndash; formats the charset using its locale-specific display name</li>
 * </ul>
 * <p>
 * If the configuration key is absent or does not match a known option, formatting is delegated to the next available
 * formatter.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class CharsetFormatter extends AbstractMultiSelectFormatter<Charset>
{
  /**
   * Creates a new charset formatter with the configuration key {@code charset} and
   * the {@code display}/{@code display-name} selection options.
   */
  public CharsetFormatter()
  {
    super("charset");

    register(new String[] { "display", "display-name" },
        (context,charset) -> context.format(charset.displayName(context.getLocale())));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link Charset} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Charset.class));
  }
}
