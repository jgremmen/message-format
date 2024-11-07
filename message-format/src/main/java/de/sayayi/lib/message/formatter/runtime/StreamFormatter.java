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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class StreamFormatter extends AbstractListFormatter<Stream<?>>
{
  @Override
  protected @NotNull Iterator<Text> createIterator(@NotNull FormatterContext context, @NotNull Stream<?> stream)
  {
    final MessageAccessor messageAccessor = context.getMessageAccessor();
    final Message.WithSpaces valueMessage = context
        .getConfigValueMessage(CONFIG_VALUE)
        .orElse(DEFAULT_VALUE_MESSAGE);
    final ValueParameters parameters = new ValueParameters(context.getLocale(), "value");

    return stream
        .map(object -> {
          parameters.value = object;
          return noSpaceText(valueMessage.format(messageAccessor, parameters));
        })
        .filter(t -> !t.isEmpty())
        .iterator();
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object stream) {
    return OptionalLong.of(((Stream<?>)stream).count());
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Stream.class));
  }
}
