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
import de.sayayi.lib.message.part.parameter.ParameterConfigAccessor;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.EMPTY_NULL_TYPE;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.EMPTY_TYPE;


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
   * Format a {@code null} value, optionally using a mapped message from the parameter
   * configuration using key type {@code null}.
   *
   * @param context  message context providing formatting information, never {@code null}
   *
   * @return  formatted null text, never {@code null}
   *
   * @since 0.8.4
   */
  @Contract(pure = true)
  default @NotNull Text formatNull(@NotNull FormatterContext context)
  {
    return context
        .getConfigMapMessage(null, EMPTY_NULL_TYPE)
        .map(context::format)
        .orElse(nullText());
  }


  /**
   * Format an empty value, optionally using a mapped message from the parameter
   * configuration using key type {@code empty}.
   *
   * @param context  message context providing formatting information, never {@code null}
   *
   * @return  formatted empty text, never {@code null}
   *
   * @since 0.8.4
   */
  @Contract(pure = true)
  default @NotNull Text formatEmpty(@NotNull FormatterContext context)
  {
    return context
        .getConfigMapMessage("", EMPTY_TYPE)
        .map(context::format)
        .orElse(emptyText());
  }


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
   * This interface marks a parameter formatter as being capable of calculating the size of the
   * formattable type.
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




  /**
   * This interface allows formatters to match a value against parameter configuration map keys.
   *
   * @param <T>  type of the value this comparator is capable of comparing
   *
   * @since 0.8.4
   */
  interface ConfigKeyComparator<T>
  {
    /**
     *
     * @param value    value to compare against the configuration key, not {@code null}
     * @param context  comparator context instance, not {@code null}
     *
     * @return  comparison match result, never {@code null}
     */
    @Contract(pure = true)
    @NotNull MatchResult compareToConfigKey(@NotNull T value, @NotNull ComparatorContext context);
  }




  /**
   * @since 0.8.4
   */
  interface ComparatorContext extends ParameterConfigAccessor
  {
    /**
     * Returns the comparison type for the current configuration key.
     *
     * @return  comparison type, never {@code null}
     */
    @Contract(pure = true)
    @NotNull CompareType getCompareType();


    /**
     * Returns the configuration key type.
     *
     * @return  configuration key type, never {@code null}, {@code NAME} or {@code NULL}
     */
    @Contract(pure = true)
    @NotNull ConfigKey.Type getKeyType();


    /**
     * Returns the boolean value of a bool configuration key.
     *
     * @return  boolean key value
     *
     * @throws ClassCastException  if the configuration key is not a bool type
     */
    @Contract(pure = true)
    boolean getBoolKeyValue();


    /**
     * Returns the number value of a number configuration key.
     *
     * @return  number key value
     *
     * @throws ClassCastException  if the configuration key is not a number type
     */
    @Contract(pure = true)
    long getNumberKeyValue();


    /**
     * Returns the string value of a string configuration key.
     *
     * @return  string key value, never {@code null}
     *
     * @throws ClassCastException  if the configuration key is not a string type
     */
    @Contract(pure = true)
    @NotNull String getStringKeyValue();


    @Contract(pure = true)
    @NotNull Locale getLocale();


    @Contract(pure = true)
    @NotNull MatchResult matchForObject(Object value);


    @Contract(pure = true)
    @NotNull <T> MatchResult matchForObject(@NotNull T value, @NotNull Class<T> valueType);
  }




  /**
   * Qualifies a parameter formatter for being assigned to type {@code Object}.
   *
   * @see GenericFormatterService#addFormatterForType(FormattableType, ParameterFormatter)
   * @since 0.8.4
   */
  interface DefaultFormatter {
  }
}
