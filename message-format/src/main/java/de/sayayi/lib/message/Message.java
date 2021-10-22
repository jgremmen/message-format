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
package de.sayayi.lib.message;

import de.sayayi.lib.message.MessageContext.Parameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;


/**
 * <p>
 *   This interface describes a message in its most generic form.
 * </p>
 * <p>
 *   Messages are thread safe.
 * </p>
 *
 * @see LocaleAware
 * @see WithCode
 *
 * @author Jeroen Gremmen
 */
public interface Message extends Serializable
{
  /**
   * <p>
   *   Formats the message based on the message parameters provided.
   * </p>
   *
   * @param messageContext  message context providing formatting information, never {@code null}
   * @param parameters      message parameters, never {@code null}
   *
   * @return  formatted message
   */
  @Contract(pure = true)
  String format(@NotNull MessageContext messageContext, @NotNull Parameters parameters);


  /**
   * <p>
   *   Formats the message based on the message parameters provided.
   * </p>
   *
   * @param messageContext   message context providing formatting information, never {@code null}
   * @param parameterValues  message parameter values, never {@code null}
   *
   * @return  formatted message
   */
  @Contract(pure = true)
  default String format(@NotNull MessageContext messageContext, @NotNull Map<String,Object> parameterValues) {
    return format(messageContext, messageContext.parameters(parameterValues));
  }


  /**
   * Tells whether this message contains one or more parameters.
   *
   * @return  {@code true} if this message contains parameters, {@code false} otherwise
   */
  @Contract(pure = true)
  boolean hasParameters();


  @Contract(pure = true)
  @NotNull SortedSet<String> getParameterNames();




  interface WithSpaces extends Message, SpacesAware {
  }




  /**
   * <p>
   *   A message class implementing this interface provides an additional code uniquely identifying the message.
   * </p>
   */
  interface WithCode extends Message
  {
    /**
     * Returns a unique message code.
     *
     * @return  message code, never {@code null}
     */
    @Contract(pure = true)
    @NotNull String getCode();
  }




  /**
   * <p>
   *   Message classes implementing this interface are capable of formatting messages for one or more locales.
   * </p>
   */
  interface LocaleAware extends Message
  {
    /**
     * {@inheritDoc}
     * <p>
     *   The message is formatted with respect to the locale provided by {@code parameters}. If the locale does not
     *   match any of the localized messages, a default message will be selected using the following rules
     * </p>
     * <ol>
     *   <li>a message with the same language but a different country</li>
     *   <li>the 1st available message (this may be implementation dependant)</li>
     * </ol>
     *
     * @see Parameters#getLocale()
     */
    @Contract(pure = true)
    @Override
    String format(@NotNull MessageContext messageContext, @NotNull Parameters parameters);


    /**
     * Returns a set of all available locales this message supports.
     *
     * @return  all available locales, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Set<Locale> getLocales();


    @Contract(pure = true)
    @NotNull Map<Locale,Message> getLocalizedMessages();
  }
}
