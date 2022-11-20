/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
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
import static java.util.Collections.unmodifiableSet;


/**
 * @author Jeroen Gremmen
 */
public final class StringFormatter extends AbstractParameterFormatter
    implements NamedParameterFormatter, EmptyMatcher, SizeQueryable
{
  private static final Set<Class<?>> FORMATTABLE_TYPES;


  static
  {
    final Set<Class<?>> formattableTypes = new HashSet<>(4);

    formattableTypes.add(CharSequence.class);
    formattableTypes.add(char[].class);

    FORMATTABLE_TYPES = unmodifiableSet(formattableTypes);
  }


  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "string";
  }


  @Override
  @Contract(pure = true)
  @SuppressWarnings({"squid:S3358", "squid:S3776"})
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, DataMap map)
  {
    return value == null
        ? nullText()
        : noSpaceText(value instanceof char[] ? new String((char[])value) : String.valueOf(value));
  }


  @Override
  @SuppressWarnings("java:S3358")
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
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return FORMATTABLE_TYPES;
  }
}