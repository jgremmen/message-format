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
package de.sayayi.lib.message.formatter.post;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * Interface for post formatters. A post formatter is used to post format a message after it has been formatted.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public interface PostFormatter
{
  /**
   * Returns the post formatter name. The name is used to identify the post formatter in the post format message
   * part and must be unique among all registered post formatters.
   * <p>
   * The post formatter name must match the kebab-case pattern and must not be empty.
   *
   * @return  post formatter name, never {@code null}
   */
  @Contract(pure = true)
  @NotNull String getName();


  /**
   * Formats the given {@code string} using the post formatter. The {@code context} can be used to access
   * the post format configuration.
   * <p>
   * The {@code string} is the trimmed result of formatting the message associated with the post format message part.
   *
   * @param string   the string to format, not {@code null}
   * @param context  the post formatter context, not {@code null}
   *
   * @return  the formatted string, never {@code null}
   */
  @Contract(pure = true)
  @NotNull String format(@NotNull String string, @NotNull PostFormatterContext context);
}
