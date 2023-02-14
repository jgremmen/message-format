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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_LENIENT;
import static de.sayayi.lib.message.internal.SpacesUtil.isTrimmedEmpty;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Arrays.asList;


/**
 * @author Jeroen Gremmen
 */
public final class StringFormatter extends AbstractParameterFormatter implements EmptyMatcher, SizeQueryable
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value)
  {
    return value == null
        ? nullText()
        : noSpaceText(value instanceof char[] ? new String((char[])value) : String.valueOf(value));
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value)
  {
    final String s = value instanceof char[] ? new String((char[])value) : String.valueOf(value);
    final boolean empty = s.isEmpty();
    final boolean lenientEmpty = isTrimmedEmpty(s);

    if (compareType == CompareType.EQ)
      return empty ? TYPELESS_EXACT : (lenientEmpty ? TYPELESS_LENIENT : null);
    else
      return lenientEmpty ? (empty ? null : TYPELESS_LENIENT) : TYPELESS_EXACT;
  }


  @Override
  public long size(@NotNull Object value)
  {
    if (value instanceof char[])
      return ((char[])value).length;
    if (value instanceof CharSequence)
      return ((CharSequence)value).length();

    // string formatter is the default formatter so value might not be a string.
    return 0;
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return new HashSet<>(asList(
        new FormattableType(CharSequence.class),
        new FormattableType(char[].class, 125)
    ));
  }
}
