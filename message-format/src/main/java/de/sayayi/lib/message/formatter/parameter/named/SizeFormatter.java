/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.named;

import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.MapKey.NUMBER_TYPE;


/**
 * Named parameter formatter that determines and formats the size of a parameter value.
 * <p>
 * This formatter is selected by using the name {@code size} in a message parameter, e.g.
 * {@code %{myParam,format:size}}.
 * <p>
 * It delegates the size calculation to the {@link ParameterFormatterContext#size(Object) context}, which queries
 * {@link de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable SizeQueryable} formatters
 * registered for the value's type. This means it can determine the size of any type that has a size-aware formatter,
 * such as strings, collections, maps and arrays.
 * <p>
 * The resulting size value can be mapped to custom text using number map keys in the parameter configuration. If no
 * mapping is provided, the numeric size is formatted as text. If the size cannot be determined, the empty map key
 * is used. {@code null} values are handled separately using the null map key.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class SizeFormatter implements NamedParameterFormatter
{
  /**
   * {@inheritDoc}
   *
   * @return  {@code "size"}, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "size";
  }


  /**
   * {@inheritDoc}
   * <p>
   * Calculates the size of the given {@code value} and formats it. If the value is {@code null}, the null map key
   * is used. If the size cannot be determined, the empty map key is used. Otherwise, the size is matched against
   * number map keys or formatted as a number.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value)
  {
    if (value == null)
      return formatNull(context);

    var optionalSize = context.size(value);
    if (optionalSize.isPresent())
    {
      var size = optionalSize.getAsLong();

      return context
          .getMapMessage(size, NUMBER_TYPE, true)
          .map(context::format)
          .orElseGet(() -> context.format(size, long.class, null, null));
    }
    else
      return formatEmpty(context);
  }
}
