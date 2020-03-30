/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.data.map;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;


/**
 * @author Jeroen Gremmen
 */
public final class MapKeyEmpty implements MapKey
{
  private final CompareType compareType;


  public MapKeyEmpty(CompareType compareType)
  {
    if (compareType != CompareType.EQ && compareType != CompareType.NE)
      throw new IllegalArgumentException("compareType must be EQ or NE");

    this.compareType = compareType;
  }


  @NotNull
  @Override
  public Type getType() {
    return Type.EMPTY;
  }


  @NotNull
  @Override
  public MatchResult match(@NotNull Locale locale, Object value)
  {
    MatchResult result = MatchResult.EXACT;
    boolean empty =
        value == null ||
        (value instanceof String && ((String)value).isEmpty()) ||
        (value instanceof CharSequence && ((CharSequence)value).length() == 0) ||
        (value instanceof Collection && ((Collection<?>)value).isEmpty()) ||
        (value instanceof Map && ((Map<?,?>)value).isEmpty()) ||
        (value.getClass().isArray() && Array.getLength(value) == 0) ||
        (value instanceof Iterable && !((Iterable<?>)value).iterator().hasNext()) ||
        (value instanceof Iterator && !((Iterator<?>)value).hasNext());

    if (!empty)
    {
      result = MatchResult.LENIENT;
      empty =
          (value instanceof String && ((String)value).trim().isEmpty()) ||
          (value instanceof CharSequence && ((CharSequence)value).toString().trim().isEmpty()) ||
          (value instanceof Character && Character.isWhitespace((Character)value));
    }

    return compareType.match(empty ? 0 : 1) ? result : MatchResult.MISMATCH;
  }
}
