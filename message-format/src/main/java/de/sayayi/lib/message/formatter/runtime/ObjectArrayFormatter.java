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
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;

import static de.sayayi.lib.message.part.TextPartFactory.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ObjectArrayFormatter extends AbstractParameterFormatter
    implements EmptyMatcher, SizeQueryable
{
  @Override
  @SuppressWarnings("DuplicatedCode")
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object array)
  {
    if (array == null)
      return nullText();

    final int length = getLength(array);
    if (length == 0)
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
        noSpaceText(context.getConfigValueString("list-this").orElse("(this array)")));
    final StringBuilder s = new StringBuilder();

    for(int i = 0; i < length; i++)
    {
      final Object value = get(array, i);
      final Text text = value == array
          ? thisText.get() : value == null
              ? nullText.get() : context.format(value, true);

      if (!text.isEmpty())
      {
        if (s.length() > 0)
          s.append((i + 1) < length ? sep : sepLast);

        s.append(text.getText());
      }
    }

    return noSpaceText(s.toString());
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value) {
    return compareType.match(getLength(value)) ? TYPELESS_EXACT : null;
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value) {
    return OptionalLong.of(getLength(value));
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(Object[].class));
  }
}
