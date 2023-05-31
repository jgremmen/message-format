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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import static de.sayayi.lib.message.part.TextPartFactory.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class PrimitiveArrayFormatter extends AbstractParameterFormatter
    implements EmptyMatcher, SizeQueryable
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object array)
  {
    if (array == null)
      return nullText();

    final int length = getLength(array);
    if (length == 0)
      return emptyText();

    final Class<?> arrayElementType = getArrayElementType(array);
    final String sep =
        spacedText(context.getConfigValueString("list-sep").orElse(", "))
            .getTextWithSpaces();
    final String sepLast =
        spacedText(context.getConfigValueString("list-sep-last").orElse(sep))
            .getTextWithSpaces();
    final StringBuilder s = new StringBuilder();
    final Getter getter = createGetter(array);

    for(int i = 0; i < length; i++)
    {
      final Text text = context.format(getter.get(i), arrayElementType, true);

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
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return new HashSet<>(Arrays.asList(
        new FormattableType(boolean[].class),
        new FormattableType(byte[].class),
        new FormattableType(short[].class),
        new FormattableType(int[].class),
        new FormattableType(long[].class),
        new FormattableType(float[].class),
        new FormattableType(double[].class),
        new FormattableType(AtomicIntegerArray.class),
        new FormattableType(AtomicLongArray.class)
    ));
  }


  @Contract(pure = true)
  private int getLength(@NotNull Object value)
  {
    if (value instanceof AtomicIntegerArray)
      return ((AtomicIntegerArray)value).length();
    else if (value instanceof AtomicLongArray)
      return ((AtomicLongArray)value).length();
    else
      return Array.getLength(value);
  }


  @Contract(pure = true)
  private @NotNull Class<?> getArrayElementType(@NotNull Object array)
  {
    return array instanceof AtomicIntegerArray
        ? int.class
        : array instanceof AtomicLongArray
            ? long.class
            : array.getClass().getComponentType();
  }


  @Contract(pure = true)
  private @NotNull Getter createGetter(@NotNull Object value)
  {
    if (value instanceof AtomicIntegerArray)
      return ((AtomicIntegerArray)value)::get;

    if (value instanceof AtomicLongArray)
      return ((AtomicLongArray)value)::get;

    return index -> Array.get(value, index);
  }




  private interface Getter
  {
    Object get(int index);
  }
}
