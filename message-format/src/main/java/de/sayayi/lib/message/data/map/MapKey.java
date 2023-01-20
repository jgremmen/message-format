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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings({"java:S1214", "java:S2386"})
public interface MapKey extends Serializable
{
  Set<Type> EMPTY_NULL_TYPE = unmodifiableSet(EnumSet.of(Type.EMPTY, Type.NULL));
  Set<Type> STRING_TYPE = unmodifiableSet(EnumSet.of(Type.STRING));
  Set<Type> NUMBER_TYPE = unmodifiableSet(EnumSet.of(Type.NUMBER));
  Set<Type> NAME_TYPE = unmodifiableSet(EnumSet.of(Type.NAME));


  @Contract(pure = true)
  @NotNull Type getType();


  @Contract(pure = true)
  @NotNull MatchResult match(@NotNull MessageContext messageContext, @NotNull Parameters parameters, Object value);




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