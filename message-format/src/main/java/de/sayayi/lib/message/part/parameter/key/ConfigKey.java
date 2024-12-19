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
package de.sayayi.lib.message.part.parameter.key;

import de.sayayi.lib.message.formatter.ParameterFormatter.ComparatorContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.part.parameter.ParameterConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.EQ;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.NE;


/**
 * Interface representing a typed key in a data map.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 *
 * @see ParameterConfig
 */
public interface ConfigKey
{
  /** Map key type {@code empty}. */
  Set<Type> EMPTY_TYPE = Set.of(Type.EMPTY);

  /** Map key types {@code empty} and {@code null}. */
  Set<Type> EMPTY_NULL_TYPE = Set.of(Type.EMPTY, Type.NULL);

  /** Map key type {@code string}. */
  Set<Type> STRING_TYPE = Set.of(Type.STRING);

  /** Map key type {@code number}. */
  Set<Type> NUMBER_TYPE = Set.of(Type.NUMBER);


  /**
   * Return the type for this key.
   *
   * @return  key type, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Type getType();


  /**
   * Returns the compare type for this configuration key. The compare type determines how the
   * provided value is compared to this key value.
   *
   * @return  compare type, never {@code null}
   *
   * @since 0.8.4
   */
  @Contract(pure = true)
  default @NotNull CompareType getCompareType() {
    return EQ;
  }




  /**
   * Type constants for map keys.
   */
  enum Type
  {
    /** String key type */
    STRING {
      @Override
      public @NotNull <T> MatchResult compareValueToKey(@NotNull ConfigKeyComparator<T> comparator, @NotNull T value,
                                                        @NotNull ComparatorContext context) {
        return comparator.compareToStringKey(value, context);
      }
    },

    /** Number key type */
    NUMBER {
      @Override
      public @NotNull <T> MatchResult compareValueToKey(@NotNull ConfigKeyComparator<T> comparator, @NotNull T value,
                                                        @NotNull ComparatorContext context) {
        return comparator.compareToNumberKey(value, context);
      }
    },

    /** Boolean key type */
    BOOL {
      @Override
      public @NotNull <T> MatchResult compareValueToKey(@NotNull ConfigKeyComparator<T> comparator, @NotNull T value,
                                                        @NotNull ComparatorContext context) {
        return comparator.compareToBoolKey(value, context);
      }
    },

    /** Null key type */
    NULL {
      @Override
      public @NotNull <T> MatchResult compareValueToKey(@NotNull ConfigKeyComparator<T> comparator, T value,
                                                        @NotNull ComparatorContext context) {
        return comparator.compareToNullKey(value, context);
      }
    },

    /** Empty key type */
    EMPTY {
      @Override
      public @NotNull <T> MatchResult compareValueToKey(@NotNull ConfigKeyComparator<T> comparator, T value,
                                                        @NotNull ComparatorContext context) {
        return comparator.compareToEmptyKey(value, context);
      }
    },

    /** Name key type */
    NAME {
      @Override
      public @NotNull <T> MatchResult compareValueToKey(@NotNull ConfigKeyComparator<T> comparator, @NotNull T value,
                                                        @NotNull ComparatorContext context) {
        throw new UnsupportedOperationException("compareValueToKey");
      }
    };


    @Contract(pure = true)
    public abstract <T> @NotNull MatchResult compareValueToKey(@NotNull ConfigKeyComparator<T> comparator, T value,
                                                               @NotNull ComparatorContext context);
  }




  /**
   * Comparison type for parameter configuration map keys.
   */
  enum CompareType
  {
    /** Compare for "less than". */
    LT,

    /** Compare for "less than or equal to". */
    LTE,

    /** Compare for "equal". */
    EQ,

    /** Compare for "not equal". */
    NE,

    /** Compare for "greater than". */
    GT,

    /** Compare for "greater than or equal to". */
    GTE;


    /**
     * Tells whether the comparison type matches the comparison result {@code signum}.
     *
     * @param signum  the comparison result, essentially the result of
     *                {@code value.compareTo(mapKey)}
     *
     * @return  {@code true} if the comparison type matches the comparison result,
     *          {@code false} otherwise
     */
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


    @Override
    public String toString()
    {
      switch(this)
      {
        case EQ:  return "=";
        case GT:  return ">";
        case GTE: return ">=";
        case LT:  return "<";
        case LTE: return "<=";
        case NE:  return "<>";
      }

      return name();
    }


    /**
     * Return a prefix representation for the comparison type.
     *
     * @return  comparison type as prefix, never {@code null}
     */
    public @NotNull String asPrefix() {
      return this == EQ ? "" : toString();
    }
  }




  @FunctionalInterface
  interface MatchResult
  {
    @Contract(pure = true)
    int value();


    @Contract(pure = true)
    default boolean isMismatch() {
      return value() <= 0;
    }


    @Contract(pure = true)
    static int compare(@NotNull MatchResult r1, @NotNull MatchResult r2) {
      return r1.value() - r2.value();
    }


    @Contract(pure = true)
    static @NotNull MatchResult forNullKey(@NotNull CompareType compareType, boolean isNull)
    {
      return compareType == EQ && isNull
          ? Defined.NULL
          : compareType == NE && !isNull
              ? Defined.NOT_NULL
              : Defined.MISMATCH;
    }


    @Contract(pure = true)
    static @NotNull MatchResult forEmptyKey(@NotNull CompareType compareType, boolean isEmpty)
    {
      return compareType == EQ && isEmpty
          ? Defined.EMPTY
          : compareType == NE && !isEmpty
              ? Defined.NOT_EMPTY
              : Defined.MISMATCH;
    }




    enum Defined implements MatchResult
    {
      /** no match */
      MISMATCH,

      /** not empty match */
      NOT_EMPTY,

      /** not null match */
      NOT_NULL,

      /** empty match */
      EMPTY,

      /** null match */
      NULL,

      /** same value meaning (e.g. 0 = false, "yes" = "Yes") */
      LENIENT,

      /** same value but different type (e.g. 4 = "4") */
      EQUIVALENT,

      /** exact match (type equality) */
      EXACT;


      @Override
      public int value() {
        return ordinal() * 2;
      }


      @Override
      public boolean isMismatch() {
        return this == MISMATCH;
      }


      @Override
      public String toString() {
        return name() + '(' + value() + ')';
      }
    }
  }
}
