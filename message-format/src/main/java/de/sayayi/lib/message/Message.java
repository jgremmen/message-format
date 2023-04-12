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
package de.sayayi.lib.message;

import de.sayayi.lib.message.MessageSupport.MessageSupportAccessor;
import de.sayayi.lib.message.internal.NoParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

import static java.util.Locale.ROOT;


/**
 * This interface describes a message in its most generic form.
 * <p>
 * Messages are thread safe.
 *
 * @see LocaleAware
 * @see WithCode
 *
 * @author Jeroen Gremmen
 */
public interface Message extends Serializable
{
  /**
   * Formats the message based on the message parameters provided.
   *
   * @param messageSupport  message context providing formatting information, never {@code null}
   * @param parameters      message parameters, never {@code null}
   *
   * @return  formatted message, never {@code null}
   */
  @Contract(pure = true)
  @NotNull String format(@NotNull MessageSupportAccessor messageSupport, @NotNull Parameters parameters);


  /**
   * Formats the message based on the message parameters provided.
   *
   * @param messageSupport   message context providing formatting information, never {@code null}
   * @param parameterValues  message parameter values, never {@code null}
   *
   * @return  formatted message
   */
  @Contract(pure = true)
  default String format(@NotNull MessageSupportAccessor messageSupport,
                        @NotNull Map<String,Object> parameterValues)
  {
    return format(messageSupport, new Parameters() {
      @Override
      public @NotNull Locale getLocale() {
        return messageSupport.getLocale();
      }

      @Override
      public Object getParameterValue(@NotNull String parameter) {
        return parameterValues.get(parameter);
      }

      @Override
      public @NotNull SortedSet<String> getParameterNames() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(parameterValues.keySet()));
      }
    });
  }


  /**
   * Returns a set with all templates names in use by this message.
   *
   * @return  template names, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Set<String> getTemplateNames();




  interface WithSpaces extends Message, SpacesAware {
  }




  /**
   * A message class implementing this interface provides an additional code uniquely identifying the message.
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
   * Message classes implementing this interface are capable of formatting messages for one or more locales.
   */
  interface LocaleAware extends Message
  {
    /**
     * {@inheritDoc}
     * <p>
     * The message is formatted with respect to the locale provided by {@code parameters}. If the locale does not
     * match any of the localized messages, a default message will be selected using the following rules
     * <ol>
     *   <li>a message with the same language but a different country</li>
     *   <li>the 1st available message (this may be implementation dependant)</li>
     * </ol>
     *
     * @see Parameters#getLocale()
     */
    @Contract(pure = true)
    @Override
    @NotNull String format(@NotNull MessageSupportAccessor messageSupport, @NotNull Parameters parameters);


    /**
     * Returns a set of all available locales this message supports.
     *
     * @return  unmodifiable set of all available locales, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Set<Locale> getLocales();


    /**
     * Returns a map of all messages keyed by locale.
     *
     * @return  unmodifiable map of all messages keyed by locale, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Map<Locale,Message> getLocalizedMessages();
  }




  /**
   * @since 0.8.0
   */
  interface Parameters
  {
    Parameters EMPTY = new NoParameters(ROOT);


    /**
     * Tells for which locale the message must be formatted. If no locale is provided or if no message
     * is available for the given locale, the formatter will look for a reasonable default message.
     *
     * @return  locale, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Locale getLocale();


    /**
     * Returns the value for the named {@code data}.
     *
     * @param parameter  data name
     *
     * @return  data value or {@code null} if no value is available for the given data name
     */
    @Contract(pure = true)
    Object getParameterValue(@NotNull String parameter);


    /**
     * Returns a set with names for all parameters available in this context.
     *
     * @return  set with all data names
     */
    @SuppressWarnings("unused")
    @Contract(pure = true)
    @NotNull SortedSet<String> getParameterNames();
  }
}
