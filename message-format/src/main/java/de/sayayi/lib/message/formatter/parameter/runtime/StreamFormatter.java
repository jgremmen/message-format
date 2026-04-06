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
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.formatter.parameter.SingletonParameters;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Stream} values.
 * <p>
 * Each stream element is formatted individually and the results are joined into a single text string. Separator,
 * truncation and overflow behavior are controlled by the list configuration keys inherited from
 * {@link AbstractListFormatter}.
 * <p>
 * Note that formatting a stream consumes it. The stream cannot be reused after formatting.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public final class StreamFormatter extends AbstractListFormatter<Stream<?>>
{
  /** {@inheritDoc} */
  @Override
  protected @NotNull Iterator<Text> createIterator(@NotNull ParameterFormatterContext context, @NotNull Stream<?> stream)
  {
    final var messageAccessor = context.getMessageAccessor();
    final var valueMessage = context
        .getConfigValueMessage(CONFIG_VALUE)
        .orElse(DEFAULT_VALUE_MESSAGE);
    final var parameters = new SingletonParameters(context.getLocale(), "value");

    return stream
        .map(object -> noSpaceText(valueMessage.format(messageAccessor, parameters.setValue(object))))
        .filter(t -> !t.isEmpty())
        .iterator();
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link Stream} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Stream.class));
  }
}
