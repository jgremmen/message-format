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


/**
 * Post formatter that converts the case of a formatted string.
 * <p>
 * This post formatter is identified by the name {@code case}. The target case is specified by the {@code case}
 * configuration key, which accepts the following values:
 * <ul>
 *   <li>{@code upper} or {@code uppercase} &ndash; converts the string to uppercase</li>
 *   <li>{@code lower} or {@code lowercase} &ndash; converts the string to lowercase</li>
 * </ul>
 * <p>
 * The case conversion is locale-aware, using the locale from the formatting context. If the configuration value is
 * not recognized, the string is returned unchanged.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public final class CasePostFormatter implements PostFormatter
{
  /**
   * {@inheritDoc}
   *
   * @return  {@code "case"}, never {@code null}
   */
  @Override
  public @NotNull String getName() {
    return "case";
  }


  /**
   * {@inheritDoc}
   * <p>
   * Converts the given {@code string} to uppercase or lowercase based on the {@code case} configuration key. If the
   * configuration value is not recognized, the string is returned unchanged.
   */
  @Override
  public @NotNull String format(@NotNull String string, @NotNull PostFormatterContext context)
  {
    return switch(context.getConfigValueString("case").orElse("")) {
      case "upper", "uppercase" -> string.toUpperCase(context.getLocale());
      case "lower", "lowercase" -> string.toLowerCase(context.getLocale());
      default -> string;
    };
  }
}
