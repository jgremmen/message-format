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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.util.SupplierDelegate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.OptionalLong;
import java.util.function.Supplier;

import static de.sayayi.lib.message.part.TextPartFactory.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;
import static java.util.Collections.singletonList;


/**
 * @author Jeroen Gremmen
 */
public final class IterableFormatter extends AbstractSingleTypeParameterFormatter<Iterable<?>>
    implements EmptyMatcher, SizeQueryable
{
  private static final Message.WithSpaces DEFAULT_VALUE_MESSAGE =
      new CompoundMessage(singletonList(new ParameterPart("value")));


  @Override
  @Contract(pure = true)
  @SuppressWarnings({"rawtypes", "DuplicatedCode"})
  public @NotNull Text formatValue(@NotNull FormatterContext context,
                                   @NotNull Iterable<?> iterable)
  {
    final Iterator iterator = ((Iterable)iterable).iterator();
    if (!iterator.hasNext())
      return emptyText();

    final Message.WithSpaces valueMessage = context
        .getConfigValueMessage("list-value").orElse(DEFAULT_VALUE_MESSAGE);
    final String sep =
        spacedText(context.getConfigValueString("list-sep").orElse(", "))
            .getTextWithSpaces();
    final String sepLast =
        spacedText(context.getConfigValueString("list-sep-last").orElse(sep))
            .getTextWithSpaces();

    final Supplier<Text> thisText = SupplierDelegate.of(() -> thisText(context));
    final MessageAccessor messageAccessor = context.getMessageSupport();
    final ValueParameters parameters =
        new ValueParameters(context.getLocale(), "value");
    final StringBuilder s = new StringBuilder();

    while(iterator.hasNext())
    {
      final Object value = iterator.next();
      final Text text;

      if (value == iterable)
        text = thisText.get();
      else
      {
        parameters.value = value;
        text = noSpaceText(valueMessage.format(messageAccessor, parameters));
      }

      if (!text.isEmpty())
      {
        if (s.length() > 0)
          s.append(iterator.hasNext() ? sep : sepLast);

        s.append(text.getText());
      }
    }

    return noSpaceText(s.toString());
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value)
  {
    final int cmp = value instanceof Collection
        ? ((Collection<?>)value).size()
        : (((Iterable<?>)value).iterator().hasNext() ? 1 : 0);

    return compareType.match(cmp) ? TYPELESS_EXACT : null;
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value)
  {
    if (value instanceof Collection)
      return OptionalLong.of(((Collection<?>)value).size());

    long size = 0;

    for(Object ignored: (Iterable<?>)value)
      size++;

    return OptionalLong.of(size);
  }


  @Contract(pure = true)
  private @NotNull Text thisText(@NotNull FormatterContext context) {
    return noSpaceText(context.getConfigValueString("list-this").orElse("(this collection)"));
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Iterable.class);
  }
}
