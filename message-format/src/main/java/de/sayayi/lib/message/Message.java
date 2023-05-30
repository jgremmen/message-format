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

import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.part.MessagePart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;


/**
 * This interface describes a message in its most generic form.
 * <p>
 * Messages are immutable and thread safe.
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
   * @param messageAccessor  message context providing formatting information, never {@code null}
   * @param parameters       message parameters, never {@code null}
   *
   * @return  formatted message, never {@code null}
   *
   * @throws MessageFormatException  in case a formatting error occurred
   */
  @Contract(pure = true)
  @NotNull String format(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException;


  /**
   * Formats the message based on the message parameters provided.
   *
   * @param messageAccessor   message context providing formatting information, never {@code null}
   * @param parameterValues   message parameter values, never {@code null}
   *
   * @return  formatted message
   *
   * @throws MessageFormatException  in case a formatting error occurred
   */
  @Contract(pure = true)
  default String format(@NotNull MessageAccessor messageAccessor,
                        @NotNull Map<String,Object> parameterValues) throws MessageFormatException
  {
    return format(messageAccessor, new Parameters() {
      @Override
      public @NotNull Locale getLocale() {
        return messageAccessor.getLocale();
      }

      @Override
      public Object getParameterValue(@NotNull String parameter) {
        return parameterValues.get(parameter);
      }

      @Override
      public @NotNull Set<String> getParameterNames() {
        return unmodifiableSet(parameterValues.keySet());
      }
    });
  }


  /**
   * Returns the message parts that when concatenated are equivalent to this message.
   * <p>
   * Messages that implement ({@link LocaleAware LocaleAware}) are not required to return a
   * sensible value. It is even valid to throw an exception like
   * {@code UnsupportedOperationException}.
   *
   * @return  message parts, never {@code null}
   *
   * @since 0.8.0
   */
  @Contract(pure = true)
  @NotNull MessagePart[] getMessageParts();


  /**
   * Returns a set with all templates names in use by this message.
   *
   * @return  unmodifiable set of template names, never {@code null}
   *
   * @since 0.8.0
   */
  @Contract(pure = true)
  @NotNull Set<String> getTemplateNames();


  /**
   * Checks whether this message is the same as the given {@code message}. Messages are
   * considered "the same" when the message parts of both messages are identical.
   * <p>
   * {@link LocaleAware LocaleAware} messages are "the same" when both locale and associated
   * message are identical for all locales provided by this message. Locale aware messages must be
   * properly handled by overriding methods.
   * <p>
   * Identical messages with different codes ({@link WithCode WithCode}) are still considered the
   * same.
   *
   * @param message  message to compare with this message, not {@code null}
   *
   * @return  {@code true} if both messages are identical, {@code false} otherwise
   *
   * @see Message#getMessageParts()
   *
   * @since 0.8.0
   */
  @Contract(pure = true)
  @SuppressWarnings("DuplicatedCode")
  default boolean isSame(@NotNull Message message)
  {
    final boolean localeAware = this instanceof LocaleAware;

    if (localeAware != (message instanceof LocaleAware))
      return false;
    else if (localeAware)
    {
      final Map<Locale,Message> lmm1 = ((LocaleAware)this).getLocalizedMessages();
      final Map<Locale,Message> lmm2 = ((LocaleAware)message).getLocalizedMessages();

      if (!lmm1.keySet().equals(lmm2.keySet()))
        return false;

      for(final Entry<Locale,Message> entry: lmm1.entrySet())
        if (!entry.getValue().isSame(lmm2.get(entry.getKey())))
          return false;

      return true;
    }
    else
      return Arrays.equals(getMessageParts(), message.getMessageParts());
  }




  /**
   * A message class implementing this interface provides information about leading/trailing
   * spaces.
   */
  interface WithSpaces extends Message, SpacesAware {
  }




  /**
   * A message class implementing this interface provides an additional code uniquely
   * identifying the message.
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
   * Message classes implementing this interface are capable of formatting messages for one or
   * more locales.
   */
  interface LocaleAware extends Message
  {
    /**
     * {@inheritDoc}
     * <p>
     * The message is formatted with respect to the locale provided by {@code parameters}. If the
     * locale does not match any of the localized messages, a default message will be selected
     * using the following rules
     * <ol>
     *   <li>a message with the same language but a different country</li>
     *   <li>the 1st available message (this may be implementation dependant)</li>
     * </ol>
     *
     * @see Parameters#getLocale()
     */
    @Contract(pure = true)
    @Override
    @NotNull String format(@NotNull MessageAccessor messageAccessor,
                           @NotNull Parameters parameters);


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
    /**
     * Tells for which locale the message must be formatted. If no locale is provided or if no
     * message is available for the given locale, the formatter will look for a reasonable
     * default message.
     *
     * @return  locale, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Locale getLocale();


    /**
     * Returns the value for the named {@code parameter}.
     *
     * @param parameter  parameter name
     *
     * @return  parameter value or {@code null} if no value is available for the given
     *          parameter name
     */
    @Contract(pure = true)
    Object getParameterValue(@NotNull String parameter);


    /**
     * Returns a set with names for all parameters available in this context.
     *
     * @return  set with all data names, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Set<String> getParameterNames();
  }
}
