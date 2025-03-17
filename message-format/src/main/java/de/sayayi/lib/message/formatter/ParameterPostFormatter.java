/*
 * Copyright 2025 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * A parameter post formatter takes care of modifying the result of a formatted parameter value.
 *
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
public interface ParameterPostFormatter
{
  @Contract(pure = true)
  @NotNull String getParameterName();


  /**
   * Post formatter order. A lower order takes precedence over a higher order post formatter.
   * <p>
   * The default implementation returns 80.
   *
   * @return  post formatter order
   */
  default int getOrder() {
    return 80;
  }


  /**
   * Post format the formatted parameter value.
   * <p>
   * This method is invoked only if the value formatted by a parameter formatter is not empty.
   * <p>
   * The returned text does not need to take care of leading and trailing spaces as they're added to the
   * returned text, if required.
   *
   * @param context  message context providing formatting information, never {@code null}
   * @param text     formatted text, not empty, never {@code null}
   *
   * @return  formatted parameter value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text postFormat(@NotNull FormatterContext context, @NotNull Text text);
}
