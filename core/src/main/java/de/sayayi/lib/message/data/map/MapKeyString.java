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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Locale;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
public final class MapKeyString implements MapKey
{
  private final CompareType compareType;
  @Getter private final String string;


  @NotNull
  @Override
  public Type getType() {
    return Type.STRING;
  }


  @NotNull
  @Override
  public MatchResult match(@NotNull Locale locale, Serializable value)
  {
    if (value == null)
      return MatchResult.MISMATCH;

    MatchResult result = MatchResult.EXACT;
    int cmp = 0;

    doMatch: {
      if (!(value instanceof CharSequence || value instanceof Character))
        result = MatchResult.LENIENT;

      String text = value.toString();

      if (compareType == CompareType.EQ)
      {
        if (text.equals(string))
          break doMatch;

        if (text.toLowerCase(locale).equals(string.toLowerCase(locale)))
        {
          result = MatchResult.LENIENT;
          break doMatch;
        }

        cmp = 1;
        break doMatch;
      }

      if (compareType == CompareType.NE && !text.toLowerCase(locale).equals(string.toLowerCase(locale)))
      {
        result = MatchResult.LENIENT;
        cmp = 1;
        break doMatch;
      }

      cmp = text.compareTo(string);
    }

    return compareType.match(cmp) ? result : MatchResult.MISMATCH;
  }
}
