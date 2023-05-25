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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

import static de.sayayi.lib.message.parameter.key.ConfigKey.NUMBER_TYPE;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;


/**
 * @author Jeroen Gremmen
 */
public final class SizeFormatter extends AbstractParameterFormatter
    implements NamedParameterFormatter
{
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "size";
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text format(@NotNull FormatterContext context, Object value)
  {
    final OptionalLong optionalSize = context.size(value);

    if (optionalSize.isPresent())
    {
      final long size = optionalSize.getAsLong();

      return context.getConfigMapMessage(size, NUMBER_TYPE, true)
          .map(context::format)
          .orElseGet(() -> context.format(size, long.class));
    }
    else
      return emptyText();
  }


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, Object value) {
    throw new IllegalStateException();
  }
}
