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

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.util.SupplierDelegate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;

import static de.sayayi.lib.message.part.TextPartFactory.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class IterableFormatter extends AbstractParameterFormatter
    implements EmptyMatcher, SizeQueryable
{
  @Override
  @Contract(pure = true)
  @SuppressWarnings({"rawtypes", "DuplicatedCode"})
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object value)
  {
    if (value == null)
      return nullText();

    final Iterator iterator = ((Iterable)value).iterator();
    if (!iterator.hasNext())
      return emptyText();

    final String sep =
        spacedText(context.getConfigValueString("list-sep").orElse(", "))
            .getTextWithSpaces();
    final String sepLast =
        spacedText(context.getConfigValueString("list-sep-last").orElse(sep))
            .getTextWithSpaces();

    final Supplier<Text> nullText = SupplierDelegate.of(() ->
        noSpaceText(context.getConfigValueString("list-null").orElse("")));
    final Supplier<Text> thisText = SupplierDelegate.of(() ->
        noSpaceText(context.getConfigValueString("list-this")
            .orElse("(this collection)")));

    final StringBuilder s = new StringBuilder();

    while(iterator.hasNext())
    {
      final Object element = iterator.next();
      final Text text = element == value
          ? thisText.get()
          : element == null ? nullText.get() : context.format(element, true);

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


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(Iterable.class));
  }
}
