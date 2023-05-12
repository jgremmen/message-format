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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static de.sayayi.lib.message.parameter.key.ConfigKey.NUMBER_TYPE;


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
  public @NotNull Text format(@NotNull FormatterContext formatterContext, Object value)
  {
    long size = 0;

    if (value != null)
    {
      final MessageAccessor messageAccessor = formatterContext.getMessageSupport();

      for(ParameterFormatter formatter: messageAccessor.getFormatters(value.getClass()))
        if (formatter instanceof SizeQueryable)
        {
          size = ((SizeQueryable)formatter).size(value);
          break;
        }
    }

    final Optional<Message.WithSpaces> mappedMessage =
        formatterContext.getConfigValueMessage(size, NUMBER_TYPE, true);

    return mappedMessage.isPresent()
        ? formatterContext.format(mappedMessage.get())
        : formatterContext.format(size, long.class);
  }


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value) {
    throw new IllegalStateException();
  }
}
