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
package de.sayayi.lib.message.part;

import de.sayayi.lib.message.FormatStringSerializer;
import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * Interface representing a typed value in a configuration or map.
 * <p>
 * Instances of this type (and its permitted subtypes) are constructed exclusively by internals of this module. This
 * type is {@code sealed} by design and is not meant to be implemented by custom formatters or other adopters; it is
 * intentionally read-only from the outside. Custom formatters can, however, freely receive and inspect instances of
 * this type (e.g. via {@code ConfigAccessor}/{@code MapAccessor}) to read configuration or map values.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 *
 * @see MessagePart.Config
 * @see MessagePart.Map
 */
public sealed interface TypedValue<T> extends FormatStringSerializer
    permits TypedValue.BoolValue, TypedValue.StringValue, TypedValue.NumberValue, TypedValue.MessageValue
{
  /**
   * Returns the underlying raw value object.
   *
   * @return  raw value object, never {@code null}
   */
  @Contract(pure = true)
  @NotNull T asObject();




  /**
   * This interface represents a boolean typed value.
   *
   * @since 0.21.0
   */
  sealed interface BoolValue extends TypedValue<Boolean> permits TypedValueBool
  {
    /**
     * Returns the number as a boolean.
     *
     * @return  number as boolean
     *
     * @since 0.21.0
     */
    @Contract(pure = true)
    boolean booleanValue();
  }




  /**
   * This interface represents a string typed value.
   *
   * @since 0.21.0
   */
  sealed interface StringValue extends TypedValue<String> permits TypedValueString
  {
    /**
     * Returns the string value.
     *
     * @return  string value, never {@code null}
     */
    @Contract(pure = true)
    @NotNull String stringValue();


    /**
     * Returns the string value parsed as a message, allowing it to be used wherever a message is expected (e.g. as
     * a template reference or a formatted parameter value).
     *
     * @return  string value parsed as a message, never {@code null}
     *
     * @since 0.25.0
     */
    @NotNull Message.WithSpaces asMessage();
  }




  /**
   * This interface represents a numeric typed value.
   *
   * @since 0.21.0
   */
  sealed interface NumberValue extends TypedValue<Long> permits TypedValueNumber
  {
    /**
     * Returns the number as an int.
     * <p>
     * If the number is outside the integer range, the returned value is saturated to
     * {@link Integer#MAX_VALUE} for positive values or {@link Integer#MIN_VALUE} for
     * negative values.
     *
     * @return  number as int
     */
    @Contract(pure = true)
    int intValue();


    /**
     * Returns the number as a long.
     *
     * @return  number as long
     */
    @Contract(pure = true)
    long longValue();
  }




  /**
   * This interface represents a message typed value.
   *
   * @since 0.21.0
   */
  sealed interface MessageValue extends TypedValue<Message.WithSpaces> permits TypedValueMessage
  {
    /**
     * Returns the message with spaces.
     *
     * @return  message with spaces, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Message.WithSpaces messageValue();
  }
}
