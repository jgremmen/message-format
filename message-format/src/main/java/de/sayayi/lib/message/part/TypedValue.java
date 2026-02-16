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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * Interface representing a typed value in a configuration or map.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 *
 * @see MessagePart.Config
 * @see MessagePart.Map
 */
public sealed interface TypedValue<T>
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
   * This class represents a boolean configuration value.
   *
   * @since 0.21.0
   */
  sealed interface BoolValue extends TypedValue<Boolean> permits TypedValueBool
  {
    /**
     * Return the number as boolean.
     *
     * @return  number as boolean
     *
     * @since 0.21.0
     */
    @Contract(pure = true)
    boolean booleanValue();
  }




  /**
   * This class represents a string configuration value.
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
     * Returns the parsed string value as a message.
     *
     * @param messageFactory  message factory instance, not {@code null}
     *
     * @return  string value parsed as a message, never {@code null}
     */
    @NotNull Message.WithSpaces asMessage(@NotNull MessageFactory messageFactory);
  }




  /**
   * This class represents a numeric configuration value.
   *
   * @since 0.21.0
   */
  sealed interface NumberValue extends TypedValue<Long> permits TypedValueNumber
  {
    /**
     * Return the number as int.
     * <p>
     * If the number is larger than the integer range, the returned value is either
     * {@code 4294967295} for positive values or {@code −4294967296} for negative values.
     *
     * @return  number as int
     */
    @Contract(pure = true)
    int intValue();


    /**
     * Return the number as long.
     *
     * @return  number as long
     */
    @Contract(pure = true)
    long longValue();
  }




  /**
   * This class represents a message configuration value.
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
