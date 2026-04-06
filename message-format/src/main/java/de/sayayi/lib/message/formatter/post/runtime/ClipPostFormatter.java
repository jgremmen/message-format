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
package de.sayayi.lib.message.formatter.post.runtime;

import de.sayayi.lib.message.formatter.post.PostFormatter;
import de.sayayi.lib.message.formatter.post.PostFormatterContext;
import org.jetbrains.annotations.NotNull;

import static java.lang.Math.max;


/**
 * Post formatter that clips a formatted string to a maximum length.
 * <p>
 * This post formatter is identified by the name {@code clip}. The maximum length is specified by the {@code clip}
 * configuration key as a numeric value. If the string exceeds this length, it is truncated.
 * <p>
 * By default, a suffix (ellipsis character {@code \u2026}) is appended to the truncated string to indicate that the
 * text has been clipped. The suffix behavior can be controlled with the following configuration keys:
 * <ul>
 *   <li>
 *     {@code clip-suffix} &ndash; set to {@code false} to disable the suffix and perform a hard truncation at the
 *     maximum length
 *   </li>
 *   <li>
 *     {@code clip-suffix-text} &ndash; a custom suffix string to use instead of the default ellipsis character
 *   </li>
 * </ul>
 * <p>
 * When a suffix is used, the string is truncated so that the total length including the suffix
 * does not exceed the configured maximum.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public final class ClipPostFormatter implements PostFormatter
{
  /**
   * {@inheritDoc}
   *
   * @return  {@code "clip"}, never {@code null}
   */
  @Override
  public @NotNull String getName() {
    return "clip";
  }


  /**
   * {@inheritDoc}
   * <p>
   * Clips the given {@code string} to the maximum length specified by the {@code clip} configuration key,
   * optionally appending a suffix to indicate truncation.
   */
  @Override
  @SuppressWarnings("UnnecessaryUnicodeEscape")
  public @NotNull String format(@NotNull String string, @NotNull PostFormatterContext context)
  {
    var maxSize = (int)context.getConfigValueNumber("clip").orElse(0);
    if (maxSize > 0)
    {
      if (context.getConfigValueBool("clip-suffix").orElse(true))
      {
        final var suffixText = context.getConfigValueString("clip-suffix-text").orElse("\u2026");
        final var suffixTextLength = suffixText.length();

        maxSize = max(maxSize, max(4, suffixTextLength + 1) + suffixTextLength);

        if (string.length() > maxSize)
          return string.substring(0, maxSize - suffixTextLength).trim() + suffixText;
      }
      else if (string.length() > maxSize)
        return string.substring(0, maxSize);
    }

    return string;
  }
}
