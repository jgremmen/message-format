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

import de.sayayi.lib.message.formatter.ParameterFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Messages are thread safe.
 *
 * @author Jeroen Gremmen
 */
public interface Message extends Serializable
{
  /**
   * Formats the message based on the message parameters provided.
   *
   * @param parameters  message parameters
   *
   * @return  formatted message
   */
  @Contract(pure = true)
  String format(@NotNull Parameters parameters);


  /**
   * Tells whether this message contains one or more parameters.
   *
   * @return  {@code true} if this message contains parameters, {@code false} otherwise
   */
  @Contract(pure = true)
  boolean hasParameters();




  interface Parameters
  {
    /**
     * Tells for which locale the message must be formatted. If no locale is provided ({@code null}) or if no message is available for the given locale,
     * the formatter will look for a reasonable default message.
     *
     * @return  locale, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Locale getLocale();


    ParameterFormatter getFormatter(String format, Class<?> type);


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
    @NotNull Set<String> getParameterNames();
  }


  interface ParameterBuilder extends Parameters
  {
    @SuppressWarnings("unused")
    @Contract("-> this")
    ParameterBuilder clear();


    @Contract("_, _ -> this")
    ParameterBuilder with(@NotNull String parameter, boolean value);


    @Contract("_, _ -> this")
    ParameterBuilder with(@NotNull String parameter, int value);


    @Contract("_, _ -> this")
    ParameterBuilder with(@NotNull String parameter, long value);


    @Contract("_, _ -> this")
    ParameterBuilder with(@NotNull String parameter, float value);


    @Contract("_, _ -> this")
    ParameterBuilder with(@NotNull String parameter, double value);


    @Contract("_, _ -> this")
    ParameterBuilder with(@NotNull String parameter, Object value);


    @Contract("_ -> this")
    ParameterBuilder with(@NotNull Map<String,Object> parameterValues);


    @Contract("_, _, _ -> this")
    ParameterBuilder withNotNull(@NotNull String parameter, Object value, @NotNull Object notNullValue);


    @Contract("_, _, _ -> this")
    ParameterBuilder withNotEmpty(@NotNull String parameter, Object value, @NotNull Object notEmptyValue);


    @Contract("_ -> this")
    ParameterBuilder withLocale(Locale locale);


    @Contract("_ -> this")
    ParameterBuilder withLocale(String locale);
  }


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


  interface LocaleAware extends Message
  {
    @Contract(pure = true)
    @NotNull Set<Locale> getLocales();
  }
}
