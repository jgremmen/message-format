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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.internal.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public interface ParameterFormatter
{
  /**
   * Formats the parameter value to a string representation.
   *
   * @param value       parameter value (can be {@code null})
   * @param format      formatter name used by the parameter or {@code null}. Eg.: {@code %{val,myformat}}
   * @param parameters  parameter values available for formatting the current message. Additionally, this instance
   *                    provides access to the formatting registry as well as to the locale. This parameter is never
   *                    {@code null}
   * @param data        parameter data provided by the parameter definition or {@code null}
   *
   * @return  formatted parameter value or {@code null} if this formatter does not produce any output
   */
  @Contract(pure = true)
  Text format(Object value, String format, @NotNull Parameters parameters, Data data);


  /**
   * <p>
   *   Returns a set of java types which are supported by this formatter.
   * </p>
   * On registration {@link FormatterService.WithRegistry#addFormatter(ParameterFormatter)} existing types which are
   * also supported by this formatter will be overridden.
   *
   * @return  a set with supported java types for this formatter, not {@code null}
   */
  @Contract(pure = true)
  @NotNull Set<Class<?>> getFormattableTypes();


  interface EmptyMatcher
  {
    /**
     * Check whether the given {@code value} is empty as defined by this formatter.
     *
     * @param compareType  comparison type (either {@link CompareType#EQ} or {@link CompareType#NE}), never {@code null}
     * @param value  object to check for emptyness, never {@code null}
     *
     * @return  {@link MatchResult#TYPELESS_EXACT}, {@link MatchResult#TYPELESS_LENIENT} or {@code null}
     */
    @Contract(pure = true)
    MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value);
  }
}
