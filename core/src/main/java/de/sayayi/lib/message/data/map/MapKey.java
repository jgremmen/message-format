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


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Locale;


/**
 * @author Jeroen Gremmen
 */
public interface MapKey
{
  EnumSet<Type> EMPTY_NULL_TYPE = EnumSet.of(Type.EMPTY, Type.NULL);
  EnumSet<Type> STRING_TYPE = EnumSet.of(Type.STRING);
  EnumSet<Type> NAME_TYPE = EnumSet.of(Type.NAME);


  @Contract(pure = true)
  @NotNull Type getType();


  @Contract(pure = true)
  @NotNull MatchResult match(@NotNull Locale locale, Object value);


  enum Type {
    STRING, NUMBER, BOOL, NULL, EMPTY, NAME
  }


  enum CompareType
  {
    LT, LTE, EQ, NE, GT, GTE;


    boolean match(int signum)
    {
      switch(this)
      {
        case EQ:   return signum == 0;
        case NE:   return signum != 0;
        case LT:   return signum < 0;
        case LTE:  return signum <= 0;
        case GT:   return signum > 0;
        case GTE:  return signum >= 0;
      }

      return false;
    }
  }


  enum MatchResult
  {
    /** no match */
    MISMATCH,

    /** empty = "  " */
    TYPELESS_LENIENT,

    /** null or empty */
    TYPELESS_EXACT,

    /** same value meaning (eg. 0 = false, "yes" = "Yes") */
    LENIENT,

    /** same value but different type (eg. 4 = "4") */
    EQUIVALENT,

    /** exact match (type equality) */
    EXACT;


    public static MatchResult max(@NotNull MatchResult m1, @NotNull MatchResult m2) {
      return m1.compareTo(m2) >= 0 ? m1 : m2;
    }
  }
}
