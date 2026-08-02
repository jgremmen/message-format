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
import de.sayayi.lib.message.internal.*;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.util.ParameterValueHelper;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.Charset;
import java.util.*;

import static java.util.Collections.unmodifiableMap;
import static java.util.Locale.ROOT;


/**
 * This interface describes a message in its most generic form. A message is the parsed representation of a message
 * format string and is composed of one or more {@linkplain MessagePart message parts}
 * (literal text, parameter references, etc.).
 * <p>
 * A message can be {@linkplain #format(MessageAccessor, Parameters) formatted} by supplying a {@link Parameters}
 * instance that provides the locale and parameter values. The formatted result is the concatenation of all evaluated
 * message parts. A message can also be {@linkplain #asFormatString(Charset) serialized} back into its message format
 * string representation.
 * <p>
 * Several sub-interfaces refine the message contract:
 * <ul>
 *   <li>{@link WithSpaces} – exposes leading and trailing space information.</li>
 *   <li>{@link WithCode} – associates a unique code with the message.</li>
 *   <li>{@link LocaleAware} – holds multiple locale-specific messages and selects the best match at format time.</li>
 * </ul>
 * <p>
 * Messages are immutable and thread safe.
 *
 * @see MessageFactory
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
@SuppressWarnings("DuplicatedCode")
public sealed interface Message extends FormatStringSerializer
{
  /**
   * Formats the message based on the message parameters provided.
   *
   * @param messageAccessor  message accessor providing formatting information, not {@code null}
   * @param parameters       message parameters, not {@code null}
   *
   * @return  formatted message without leading/trailing spaces, never {@code null}
   *
   * @throws MessageFormatException  in case a formatting error occurred
   */
  @Contract(pure = true)
  default @NotNull String format(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException {
    return formatAsText(messageAccessor, parameters).getTextNotNull();
  }


  /**
   * Formats the message based on the message parameters provided.
   *
   * @param messageAccessor   message accessor providing formatting information, not {@code null}
   * @param parameterValues   message parameter values, not {@code null}
   *
   * @return  formatted message, never {@code null}
   *
   * @throws MessageFormatException  in case a formatting error occurred
   */
  @Contract(pure = true)
  default @NotNull String format(@NotNull MessageAccessor messageAccessor, @NotNull Map<String,Object> parameterValues)
      throws MessageFormatException
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
      public @Unmodifiable @NotNull Map<String,Object> asParameterMap() {
        return unmodifiableMap(parameterValues);
      }

      @Override
      public String toString() {
        return "Parameters(locale=" + messageAccessor.getLocale() + ',' + parameterValues + ')';
      }
    });
  }


  /**
   * Formats the message based on the message parameters provided.
   *
   * @param messageAccessor  message accessor providing formatting information, not {@code null}
   * @param parameters       message parameters, not {@code null}
   *
   * @return  formatted message as text optionally with leading/trailing spaces, never {@code null}
   *
   * @throws MessageFormatException  in case a formatting error occurred
   *
   * @since 0.9.1
   */
  @Contract(pure = true)
  @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException;


  /**
   * Formats the message based on the given parameter values map. The locale is obtained from the
   * {@code messageAccessor}. This is a convenience method that wraps the parameter values in a {@link Parameters}
   * instance and delegates to {@link #formatAsText(MessageAccessor, Parameters)}.
   *
   * @param messageAccessor   message accessor providing formatting information, not {@code null}
   * @param parameterValues   message parameter values, not {@code null}
   *
   * @return  formatted message as text optionally with leading/trailing spaces, never {@code null}
   *
   * @throws MessageFormatException  in case a formatting error occurred
   *
   * @since 0.24.0
   */
  @Contract(pure = true)
  default @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor,
                                     @NotNull Map<String,Object> parameterValues)
      throws MessageFormatException
  {
    return formatAsText(messageAccessor, new Parameters() {
      @Override
      public @NotNull Locale getLocale() {
        return messageAccessor.getLocale();
      }

      @Override
      public Object getParameterValue(@NotNull String parameter) {
        return parameterValues.get(parameter);
      }

      @Override
      public @Unmodifiable @NotNull Map<String,Object> asParameterMap() {
        return unmodifiableMap(parameterValues);
      }

      @Override
      public String toString() {
        return "Parameters(locale=" + messageAccessor.getLocale() + ',' + parameterValues + ')';
      }
    });
  }


  /**
   * Returns the message parts that, when concatenated, are equivalent to this message.
   * <p>
   * Messages that implement ({@link LocaleAware LocaleAware}) are not required to return a sensible value. It is even
   * valid to throw an exception like {@code UnsupportedOperationException}.
   *
   * @return  message parts, never empty or {@code null}
   *
   * @since 0.8.0
   */
  @Contract(value = "-> new", pure = true)
  @NotNull MessagePart[] getMessageParts();


  /**
   * Returns a set with all templates names in use by this message.
   *
   * @return  unmodifiable set of template names, never {@code null}
   *
   * @since 0.8.0
   */
  @Contract(pure = true)
  @Unmodifiable
  default @NotNull Set<String> getTemplateNames() {
    return Set.of();
  }


  /**
   * Checks whether this message is the same as the given {@code message}. Messages are considered "the same" when
   * the message parts of both messages are identical.
   * <p>
   * {@link LocaleAware LocaleAware} messages are "the same" when both locale and associated message are identical for
   * all locales provided by this message. Locale aware messages must be properly handled by overriding methods.
   * <p>
   * Identical messages with different codes ({@link WithCode WithCode}) are still considered the same as only the
   * message part is compared.
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
  default boolean isSame(@NotNull Message message) {
    return MessageFactory.isSame(this, message);
  }


  /**
   * Serializes this message into its message format string representation using the given charset for encoding checks.
   * <p>
   * Characters that cannot be encoded by the specified charset are serialized as Unicode escape sequences. The 
   * resulting string is equivalent to the original format string from which this message was parsed.
   *
   * @param charset  charset used to determine character encodability, not {@code null}
   *
   * @return  the message format string representation of this message, never {@code null}
   *
   * @see FormatStringSerializer#serialize(FormatStringSerializer.Context)
   *
   * @since 0.21.0
   */
  @Contract(pure = true)
  default @NotNull @Language("MessageFormat") String asFormatString(@NotNull Charset charset)
  {
    final var serializerContext = new FormatStringSerializer.Context(charset);

    serialize(serializerContext);

    return serializerContext.textJoiner().asSpacedText().getTextNotNull();
  }


  /**
   * {@inheritDoc}
   * <p>
   * This default implementation serializes the message by iterating over all
   * {@linkplain #getMessageParts() message parts} and delegating to each part's
   * {@link MessagePart#serialize(Context) serialize} method.
   */
  @Override
  default void serialize(@NotNull Context context)
  {
    for(var messagePart: getMessageParts())
      messagePart.serialize(context);
  }


  /**
   * Returns a shared empty message instance that produces an empty string when formatted.
   *
   * @return  empty message, never {@code null}
   *
   * @since 0.24.0
   */
  @Contract(pure = true)
  static @NotNull Message.WithSpaces empty() {
    return EmptyMessage.INSTANCE;
  }




  /**
   * A message that exposes leading and trailing space information. This is important for proper whitespace handling
   * when message parts are concatenated during formatting, ensuring that spaces between adjacent parts are preserved
   * correctly.
   *
   * @since 0.1.0
   */
  sealed interface WithSpaces extends Message, SpacesAware permits CompoundMessage, EmptyMessage, TextMessage
  {
    /**
     * Tells if the message has a leading space.
     *
     * @return  {@code true} if the message has a leading space, {@code false} otherwise
     */
    @Override
    default boolean isSpaceBefore() {
      return getMessageParts()[0].isSpaceBefore();
    }


    /**
     * Tells if the message has a trailing space.
     *
     * @return  {@code true} if the message has a trailing space, {@code false} otherwise
     */
    @Override
    default boolean isSpaceAfter()
    {
      final var messageParts = getMessageParts();
      return messageParts[messageParts.length - 1].isSpaceAfter();
    }
  }




  /**
   * A message that is identified by a unique code. Coded messages are typically registered in a {@link MessageSupport}
   * and can be looked up and formatted by their code. The code is not part of the formatted output but serves as a
   * stable identifier for the message.
   *
   * @since 0.1.0
   */
  sealed interface WithCode extends Message permits AbstractMessageWithCode
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
   * A message that holds multiple locale-specific messages and selects the best matching localized message at format
   * time. If no exact locale match is found, a fallback strategy is applied to select a reasonable default.
   * <p>
   * Because a locale-aware message delegates to different underlying messages depending on the locale, methods like
   * {@link #getMessageParts()} and {@link #asFormatString(Charset)} are not supported and will throw
   * {@link UnsupportedOperationException}. Use {@link #getLocalizedMessages()} to access the individual
   * locale-specific messages.
   *
   * @since 0.1.0
   */
  sealed interface LocaleAware extends Message permits LocalizedMessageBundleWithCode
  {
    /**
     * {@inheritDoc}
     * <p>
     * The message is formatted with respect to the locale provided by {@code parameters}. If the locale does not match
     * any of the localized messages, a default message will be selected using the following rules
     * <ol>
     *   <li>a message with the same language but a different country</li>
     *   <li>the 1st available message (this may be implementation dependant)</li>
     * </ol>
     *
     * @see Parameters#getLocale()
     */
    @Contract(pure = true)
    @Override
    @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters);


    /**
     * Returns a set of all available locales this message supports.
     *
     * @return  unmodifiable set of all available locales, never {@code null}
     */
    @Contract(pure = true)
    @Unmodifiable
    @NotNull Set<Locale> getLocales();


    /**
     * Returns a map of all messages keyed by locale.
     *
     * @return  unmodifiable map of all messages keyed by locale, never {@code null}
     */
    @Contract(pure = true)
    @Unmodifiable
    @NotNull Map<Locale,Message> getLocalizedMessages();


    /**
     * Always throws {@link UnsupportedOperationException} because a locale-aware message does not have a single
     * array of message parts. Each locale has its own associated message with its own parts.
     *
     * @return  never returns normally
     *
     * @throws UnsupportedOperationException  always
     *
     * @see #getLocalizedMessages()
     */
    @Override
    @Contract("-> fail")
    default @NotNull MessagePart[] getMessageParts() {
      throw new UnsupportedOperationException("getMessageParts");
    }


    /**
     * Always throws {@link UnsupportedOperationException} because a locale-aware message does not have a single
     * format string representation. Each locale has its own associated message with its own format string.
     *
     * @param charset  ignored
     *
     * @return  never returns normally
     *
     * @throws UnsupportedOperationException  always
     *
     * @see #getLocalizedMessages()
     */
    @Override
    @Contract("_ -> fail")
    default @NotNull String asFormatString(@NotNull Charset charset) {
      throw new UnsupportedOperationException("asFormatString");
    }
  }




  /**
   * Interface providing locale and parameters to be used for formatting a message.
   *
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
     * @param parameter  parameter name, not {@code null}
     *
     * @return  parameter value or {@code null} if no value is available for the given
     *          parameter name
     */
    @Contract(pure = true)
    Object getParameterValue(@NotNull String parameter);


    /**
     * Returns the named parameter value converted to a {@code Boolean}.
     *
     * @param parameter  parameter name, not {@code null}
     *
     * @return  an {@link Optional} containing the boolean value, or an empty optional if the value is {@code null}
     *          or not convertible to a boolean
     *
     * @see ParameterValueHelper#getBoolean(Parameters, String)
     *
     * @since 0.24.0
     */
    @Contract(pure = true)
    default @NotNull Optional<Boolean> getParameterValueAsBoolean(@NotNull String parameter) {
      return ParameterValueHelper.getBoolean(this, parameter);
    }


    /**
     * Returns the named parameter value converted to an {@code int}.
     *
     * @param parameter  parameter name, not {@code null}
     *
     * @return  an {@link OptionalInt} containing the int value, or an empty optional if the value is {@code null}
     *          or not convertible to an int
     *
     * @see ParameterValueHelper#getInt(Parameters, String)
     *
     * @since 0.24.0
     */
    @Contract(pure = true)
    default @NotNull OptionalInt getParameterValueAsInt(@NotNull String parameter) {
      return ParameterValueHelper.getInt(this, parameter);
    }


    /**
     * Returns the named parameter value converted to a {@code long}.
     *
     * @param parameter  parameter name, not {@code null}
     *
     * @return  an {@link OptionalLong} containing the long value, or an empty optional if the value is {@code null}
     *          or not convertible to a long
     *
     * @see ParameterValueHelper#getLong(Parameters, String)
     *
     * @since 0.24.0
     */
    @Contract(pure = true)
    default @NotNull OptionalLong getParameterValueAsLong(@NotNull String parameter) {
      return ParameterValueHelper.getLong(this, parameter);
    }


    /**
     * Returns the named parameter value converted to an enum constant of the specified type.
     *
     * @param parameter  parameter name, not {@code null}
     * @param enumType   the enum class to convert to, not {@code null}
     * @param <T>        the enum type
     *
     * @return  an {@link Optional} containing the matching enum constant, or an empty optional if the value is
     *          {@code null} or does not match any constant
     *
     * @see ParameterValueHelper#getEnum(Parameters, String, Class)
     *
     * @since 0.24.0
     */
    @Contract(pure = true)
    default <T extends Enum<T>> @NotNull Optional<T> getParameterValueAsEnum(@NotNull String parameter,
                                                                             @NotNull Class<T> enumType) {
      return ParameterValueHelper.getEnum(this, parameter, enumType);
    }


    /**
     * Returns the named parameter value converted to a {@code String}.
     *
     * @param parameter  parameter name, not {@code null}
     *
     * @return  an {@link Optional} containing the string value, or an empty optional if the value is {@code null}
     *          or not a {@link CharSequence}
     *
     * @see ParameterValueHelper#getString(Parameters, String)
     *
     * @since 0.24.0
     */
    @Contract(pure = true)
    default @NotNull Optional<String> getParameterValueAsString(@NotNull String parameter) {
      return ParameterValueHelper.getString(this, parameter);
    }


    /**
     * Returns a set with names for all parameters available in this context.
     *
     * @return  set with all data names, never {@code null}
     */
    @Contract(pure = true)
    default @Unmodifiable @NotNull Set<String> getParameterNames() {
      return asParameterMap().keySet();
    }


    /**
     * Returns an unmodifiable map containing all parameter names and their associated values.
     *
     * @return  unmodifiable parameter map, never {@code null}
     *
     * @since 0.24.0
     */
    @Contract(pure = true)
    @Unmodifiable @NotNull Map<String,Object> asParameterMap();


    /**
     * Returns the hash code value for this parameter's instance.
     * <p>
     * The hash code of a parameter's instance is defined to be the sum of the hash codes of the locale, the parameter
     * name strings and the parameter values. This ensures that {@code p1.equals(p2)} implies that
     * {@code p1.hashCode() == p2.hashCode()} for any two parameter instances {@code p1} and {@code p2}, as required
     * by the general contract of {@link Object#hashCode}.
     *
     * @return  the hash code value for this parameters instance
     */
    int hashCode();


    /**
     * Returns an empty parameters instance with the {@linkplain Locale#ROOT root locale} and no parameter values.
     *
     * @return  empty parameters instance, never {@code null}
     *
     * @since 0.24.0
     */
    @Contract(value = "-> new", pure = true)
    static @NotNull Parameters empty() {
      return new NoParameters(ROOT);
    }


    /**
     * Returns an empty parameters instance with the given {@code locale} and no parameter values.
     *
     * @param locale  locale for the parameters, not {@code null}
     *
     * @return  empty parameters instance, never {@code null}
     *
     * @since 0.24.0
     */
    @Contract(value = "_ -> new", pure = true)
    static @NotNull Parameters empty(Locale locale) {
      return new NoParameters(locale);
    }
  }
}
