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

import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.*;
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;


/**
 * @author Jeroen Gremmen
 */
public final class ArrayFormatter extends AbstractParameterFormatter implements EmptyMatcher, SizeQueryable
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object array)
  {
    if (array == null)
      return nullText();

    final int length = getLength(array);
    if (length == 0)
      return emptyText();

    final StringBuilder s = new StringBuilder();
    final Class<?> arrayType = array.getClass();
    final Class<?> arrayElementType = arrayType.isPrimitive() ? arrayType.getComponentType() : null;
    final String sep = context.getConfigValueString("list-sep").orElse(", ");
    final String sepLast = context.getConfigValueString("list-sep-last").orElse(sep);
    final String thisObject = context.getConfigValueString("list-this").orElse("(this array)");

    for(int i = 0; i < length; i++)
    {
      final Object value = get(array, i);
      Text text = null;

      if (value == array)
        text = noSpaceText(thisObject);
      else if (value != null)
        text = context.format(value, arrayElementType, true);

      if (text != null && !text.isEmpty())
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
  public long size(@NotNull Object value) {
    return getLength(value);
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return new HashSet<>(Arrays.asList(
        new FormattableType(Object[].class, 125),
        new FormattableType(short[].class, 125),
        new FormattableType(int[].class, 125),
        new FormattableType(long[].class, 125),
        new FormattableType(float[].class, 125),
        new FormattableType(double[].class, 125),
        new FormattableType(boolean[].class, 125)
    ));
  }
}
