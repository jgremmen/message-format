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
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class AtomicIntegerArrayFormatter extends AbstractParameterFormatter
    implements EmptyMatcher, SizeQueryable
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object value)
  {
    final int[] array;

    if (value != null)
    {
      final AtomicIntegerArray aia = (AtomicIntegerArray)value;
      final int length = aia.length();

      array = new int[length];
      for(int n = 0; n < length; n++)
        array[n] = aia.get(n);
    }
    else
      array = null;

    return context.format(array, int[].class, true);
  }


  @Override
  public ConfigKey.MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value) {
    return compareType.match(((AtomicIntegerArray)value).length()) ? TYPELESS_EXACT : null;
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value) {
    return OptionalLong.of(((AtomicIntegerArray)value).length());
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(AtomicIntegerArray.class));
  }
}
