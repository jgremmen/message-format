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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;
import java.util.Set;


/**
 * A parameter formatter takes care of formatting a parameter value.
 * <p>
 * If {@link #getFormattableTypes()} returns a non-empty collection, parameter values that match
 * one of the types in the collection will be formatted using this parameter formatter. If the
 * returned collection is empty, the formatter is selected only if it implements
 * {@link NamedParameterFormatter} and is referenced by name.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public interface ParameterFormatter
{
  /**
   * This type represents the type for a {@code null} value.
   *
   * @see NamedParameterFormatter#canFormat(Class)
   */
  Class<?> NULL_TYPE = new Object() {}.getClass();


  /**
   * Formats the parameter value to a string representation.
   *
   * @param context  message context providing formatting information, never {@code null}
   * @param value    parameter value (can be {@code null})
   *
   * @return  formatted parameter value, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Text format(@NotNull FormatterContext context, Object value);


  /**
   * Returns a set of java types which are supported by this formatter.
   * <p>
   * On registration {@link FormatterService.WithRegistry#addFormatter(ParameterFormatter)}
   * existing types which are also supported by this formatter will co-exist with each other.
   * The order attribute determines which formatter is preferred before the other. If different
   * formatters have both the same type and order, the formatter precedence is determined by
   * the class name. The behavior is deterministic but it is encouraged to select different
   * order values for those cases.
   *
   * @return  a set with supported java types for this formatter, not {@code null}
   *
   * @see FormattableType#compareTo(FormattableType)
   */
  @Contract(pure = true)
  @NotNull Set<FormattableType> getFormattableTypes();




  /**
   * This interface marks a parameter formatter as being capable of determining whether a
   * formattable type is empty.
   *
   * @see SizeQueryable
   */
  interface EmptyMatcher
  {
    /**
     * Check whether the given {@code value} is empty as defined by this formatter.
     *
     * @param compareType  comparison type (either {@link CompareType#EQ CompareType#EQ} or
     *                     {@link CompareType#NE CompareType#NE}), never {@code null}
     * @param value        object to check for emptyness, never {@code null}
     *
     * @return  {@link MatchResult#TYPELESS_EXACT MatchResult#TYPELESS_EXACT},
     *          {@link MatchResult#TYPELESS_LENIENT MatchResult#TYPELESS_LENIENT} or
     *          {@code null}
     */
    @Contract(pure = true)
    MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value);
  }




  /**
   * This interface marks a parameter formatter as being capable of calculating the size of the
   * formattable type.
   *
   * @see EmptyMatcher
   */
  interface SizeQueryable
  {
    /**
     * Returns the size of the given {@code value}.
     *
     * @param context  formatter context, not {@code null}
     * @param value    object to calculate the size of, not {@code null}
     *
     * @return  value size (&gt;= 0) or {@link OptionalLong#empty()} if this formatter is not
     *          capable of determining the size, never {@code null}
     */
    @Contract(pure = true)
    @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value);
  }
}
