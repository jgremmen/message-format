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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
public final class MapKeyNumber implements MapKey
{
  private CompareType compareType;
  @Getter private final long number;


  @NotNull
  @Override
  public Type getType() {
    return Type.NUMBER;
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
      if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte)
      {
        cmp = Long.signum(((Number)value).longValue() - number);
        break doMatch;
      }

      if (value instanceof BigInteger)
      {
        cmp = ((BigInteger)value).compareTo(BigInteger.valueOf(number));
        break doMatch;
      }

      if (value instanceof CharSequence || value instanceof Character)
      {
        try {
          value = new BigDecimal(value.toString());
          result = MatchResult.LENIENT;
        } catch(Exception ignore) {
        }
      }

      if (value instanceof BigDecimal)
      {
        cmp = ((BigDecimal)value).compareTo(BigDecimal.valueOf(number));
        break doMatch;
      }

      if (value instanceof Float)
      {
        cmp = Float.compare((Float)value, number);
        break doMatch;
      }

      if (value instanceof Number)
      {
        cmp = Double.compare(((Number)value).doubleValue(), number);
        break doMatch;
      }

      return MatchResult.MISMATCH;
    }

    return compareType.match(cmp) ? result : MatchResult.MISMATCH;
  }
}
