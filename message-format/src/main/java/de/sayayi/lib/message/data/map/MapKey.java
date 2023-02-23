/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.data.map;

import de.sayayi.lib.message.MessageContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;


/**
 * @author Jeroen Gremmen
 */
public interface MapKey extends Serializable
{
  /** Map key types {@code empty} and {@code null}. */
  Set<Type> EMPTY_NULL_TYPE = unmodifiableSet(EnumSet.of(Type.EMPTY, Type.NULL));

  /** Map key type {@code string}. */
  Set<Type> STRING_TYPE = unmodifiableSet(EnumSet.of(Type.STRING));

  /** Map key types {@code string} and {@code empty}. */
  Set<Type> STRING_EMPTY_TYPE = unmodifiableSet(EnumSet.of(Type.STRING, Type.EMPTY));

  /** Map key type {@code number}. */
  Set<Type> NUMBER_TYPE = unmodifiableSet(EnumSet.of(Type.NUMBER));

  /** Map key type {@code name}. */
  Set<Type> NAME_TYPE = unmodifiableSet(EnumSet.of(Type.NAME));


  /**
   * Return the type for this key.
   *
   * @return  key type, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Type getType();


  /**
   * Calculates a match result for the given {@code value} with respect to this key.
   *
   * @param messageContext  message context instance, neot {@code null}
   * @param locale          formatting locale, not {@code null}
   * @param value           value to compare with this key
   *
   * @return  matching result, never {@code null}
   */
  @Contract(pure = true)
  @NotNull MatchResult match(@NotNull MessageContext messageContext, @NotNull Locale locale, Object value);




  enum Type {
    STRING, NUMBER, BOOL, NULL, EMPTY, NAME
  }




  enum CompareType
  {
    LT, LTE, EQ, NE, GT, GTE;


    public boolean match(int signum)
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
  }
}
