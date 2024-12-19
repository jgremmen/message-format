/*
 * Copyright 2023 Jeroen Gremmen
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

import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Locale.forLanguageTag;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@SuppressWarnings("UnknownLanguage")
public interface MessageSupport
{
  /**
   * Returns the message accessor instance.
   *
   * @return  message accessor, never {@code null}
   */
  @Contract(pure = true)
  @NotNull MessageAccessor getMessageAccessor();


  /**
   * Prepare a message with the given {@code code} for formatting.
   *
   * @param code  message code, not {@code null}
   *
   * @return  message configurer instance for the given {@code code}, never {@code null}
   *
   * @throws IllegalArgumentException  in case the given message {@code code} is unknown
   */
  @Contract(value = "_ -> new", pure = true)
  @NotNull MessageConfigurer<Message.WithCode> code(@NotNull String code);


  /**
   * Prepare a {@code message} for formatting.
   *
   * @param message  message, not {@code null}
   *
   * @return  message configurer instance for the given {@code message}, never {@code null}
   */
  @Contract(value = "_ -> new", pure = true)
  @NotNull MessageConfigurer<Message> message(@NotNull @Language("MessageFormat") String message);


  /**
   * Prepare a {@code message} for formatting.
   *
   * @param message  message, not {@code null}
   * @param <M>      message type
   *
   * @return  message configurer instance for the given {@code message}, never {@code null}
   */
  @Contract(value = "_ -> new", pure = true)
  <M extends Message> @NotNull MessageConfigurer<M> message(@NotNull M message);


  /**
   * Export all messages and templates from this message support to a compact binary representation.
   * This way a message support can be prepared once and loaded very quickly by importing the packed
   * messages at runtime.
   *
   * @param stream  pack output stream, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @see ConfigurableMessageSupport#importMessages(InputStream...)
   * @see ConfigurableMessageSupport#importMessages(Enumeration)
   */
  default void exportMessages(@NotNull OutputStream stream) throws IOException {
    exportMessages(stream, true, null);
  }


  /**
   * Pack all messages (optionally filtering them using a {@code messageCodeFilter}) from this
   * bundle into a compact binary representation. This way a message support can be prepared once
   * and loaded very quickly by importing the packed messages at runtime.
   * <p>
   * Parameter {@code compress} switches GZip on/off, potentially reducing the packed size even
   * more. For message support instances with a small amount of messages the compression may not be
   * substantial as the binary representation does some extensive bit-packing already.
   *
   * @param stream             pack output stream, not {@code null}
   * @param compress           {@code true} compress pack, {@code false} do not compress pack
   * @param messageCodeFilter  optional predicate for selecting message codes. If {@code null}
   *                           all messages from this message support will be selected
   *
   * @throws IOException  if an I/O error occurs
   *
   * @see ConfigurableMessageSupport#importMessages(InputStream...)
   * @see ConfigurableMessageSupport#importMessages(Enumeration)
   */
  void exportMessages(@NotNull OutputStream stream, boolean compress, Predicate<String> messageCodeFilter)
      throws IOException;




  interface MessageConfigurer<M extends Message>
  {
    /**
     * Returns the message associated with this message configurer.
     *
     * @return  message, never {@code null}
     */
    @Contract(pure = true)
    @NotNull M getMessage();


    /**
     * Returns a map with all parameters configured for this message.
     * <p>
     * The returned map is not backed by this configurer, so changes to the configurer will not reflect in the map.
     *
     * @return  unmodifiable parameter map, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Map<String,Object> getParameters();


    /**
     * Clear all message parameter values.
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "-> this", mutates = "this")
    @NotNull MessageConfigurer<M> clear();


    /**
     * Remove message parameter values with the given {@code parameter} name.
     *
     * @param parameter  parameter name, not {@code null}
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull MessageConfigurer<M> remove(@NotNull String parameter);


    /**
     * Sets a boolean value for this message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull String parameter, boolean value) {
      return with(parameter, Boolean.valueOf(value));
    }


    /**
     * Sets a byte value for this message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull String parameter, byte value) {
      return with(parameter, Byte.valueOf(value));
    }


    /**
     * Sets a character value for this message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull String parameter, char value) {
      return with(parameter, Character.valueOf(value));
    }


    /**
     * Sets a short value for this message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull String parameter, short value) {
      return with(parameter, Short.valueOf(value));
    }


    /**
     * Sets an integer value for this message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull String parameter, int value) {
      return with(parameter, Integer.valueOf(value));
    }


    /**
     * Sets a long value for this message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull String parameter, long value) {
      return with(parameter, Long.valueOf(value));
    }


    /**
     * Sets a float value for this message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull String parameter, float value) {
      return with(parameter, Float.valueOf(value));
    }


    /**
     * Sets a double value for this message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull String parameter, double value) {
      return with(parameter, Double.valueOf(value));
    }


    /**
     * Sets a value for this message.
     *
     * @param parameter  parameter name, not {@code null}
     * @param value      parameter value
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull MessageConfigurer<M> with(@NotNull String parameter, Object value);


    /**
     * Sets multiple values for this message.
     *
     * @param parameterValues  map with parameters (key = parameter name, value = parameter value),
     *                         not {@code null}
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull Map<String,Object> parameterValues)
    {
      parameterValues.forEach(this::with);
      return this;
    }


    /**
     * Sets multiple values for this message.
     *
     * @param properties  properties (key = parameter name, value = parameter value),
     *                    not {@code null}
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull Properties properties)
    {
      properties.forEach((key,value) -> with((String)key, value));
      return this;
    }


    /**
     * Change the locale.
     * <p>
     * If {@code locale} is {@code null} the default locale for this message support is used.
     *
     * @param locale  locale or {@code null}
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull MessageConfigurer<M> locale(Locale locale);


    /**
     * Change the locale.
     * <p>
     * If {@code locale} is {@code null} the default locale for this message support is used.
     *
     * @param locale  locale or {@code null}
     *
     * @return  message configurer instance for this message, never {@code null}
     */
    @Contract(value = "_ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> locale(String locale) {
      return locale(locale == null ? null : forLanguageTag(locale));
    }


    /**
     * Returns the formatted message.
     *
     * @return  formatted message, never {@code null}
     */
    @Contract(pure = true)
    @NotNull String format();


    /**
     * Returns a supplier capable of formatting the message.
     * <p>
     * Formatting the message is delayed until {@link Supplier#get()} is invoked.
     *
     * @return  format supplier, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Supplier<String> formatSupplier();


    /**
     * Create an exception with the formatted message.
     * <p>
     * Exceptions thrown by the constructor function are relayed to the caller.
     *
     * @param constructor  exception constructor, not {@code null}
     * @param cause        the throwable that caused this exception
     * @param <X>          exception type
     *
     * @return  newly created exception with the formatted message and the given root cause,
     *          never {@code null}
     *
     * @since 0.8.3
     */
    @Contract("_, _ -> new")
    @NotNull <X extends Exception> X formattedException(
        @NotNull ExceptionConstructorWithCause<X> constructor, Throwable cause);


    /**
     * Create an exception with the formatted message.
     * <p>
     * Exceptions thrown by the constructor function are relayed to the caller.
     *
     * @param constructor  exception constructor, not {@code null}
     * @param <X>          exception type
     *
     * @return  newly created exception with the formatted message, never {@code null}
     *
     * @since 0.8.3
     */
    @Contract("_ -> new")
    default @NotNull <X extends Exception> X formattedException(
        @NotNull ExceptionConstructorWithCause<X> constructor) {
      return formattedException(constructor, null);
    }


    /**
     * Throw an exception with the formatted message.
     * <p>
     * Exceptions thrown by the constructor function are relayed to the caller.
     *
     * @param constructor  exception constructor, not {@code null}
     * @param <X>          exception type
     *
     * @return  newly created exception with the formatted message, never {@code null}
     */
    @Contract("_ -> new")
    @NotNull <X extends Exception> X formattedException(@NotNull ExceptionConstructor<X> constructor);


    /**
     * Returns a supplier capable of creating an exception with the formatted message.
     * This method is useful in combination with one of the optional class methods like
     * {@link OptionalInt#orElseThrow(Supplier)}.
     * <p>
     * Formatting the message is delayed until {@link Supplier#get()} is invoked.
     * <p>
     * Exceptions thrown by the constructor function are relayed to the caller.
     *
     * @param constructor  exception constructor, not {@code null}
     * @param cause        the throwable that caused this exception
     * @param <X>          exception type
     *
     * @return  format supplier, never {@code null}
     *
     * @since 0.8.3
     */
    @Contract(pure = true)
    <X extends Exception> @NotNull Supplier<X> formattedExceptionSupplier(
        @NotNull ExceptionConstructorWithCause<X> constructor, Throwable cause);


    /**
     * Returns a supplier capable of creating an exception with the formatted message.
     * This method is useful in combination with one of the optional class methods like
     * {@link OptionalInt#orElseThrow(Supplier)}.
     * <p>
     * Formatting the message is delayed until {@link Supplier#get()} is invoked.
     * <p>
     * Exceptions thrown by the constructor function are relayed to the caller.
     *
     * @param constructor  exception constructor, not {@code null}
     * @param <X>          exception type
     *
     * @return  format supplier, never {@code null}
     *
     * @since 0.8.3
     */
    @Contract(pure = true)
    default <X extends Exception> @NotNull Supplier<X> formattedExceptionSupplier(
        @NotNull ExceptionConstructorWithCause<X> constructor) {
      return formattedExceptionSupplier(constructor, null);
    }


    /**
     * Returns a supplier capable of creating an exception with the formatted message.
     * This method is useful in combination with one of the optional class methods like
     * {@link OptionalInt#orElseThrow(Supplier)}.
     * <p>
     * Formatting the message is delayed until {@link Supplier#get()} is invoked.
     * <p>
     * Exceptions thrown by the constructor function are relayed to the caller.
     *
     * @param constructor  exception constructor, not {@code null}
     * @param <X>          exception type
     *
     * @return  format supplier, never {@code null}
     */
    @Contract(pure = true)
    <X extends Exception> @NotNull Supplier<X> formattedExceptionSupplier(
        @NotNull ExceptionConstructor<X> constructor);
  }




  /**
   * Configurable extend of message support providing methods to add/import messages, set default
   * parameter configuration values and change the default locale.
   */
  interface ConfigurableMessageSupport extends MessageSupport, MessagePublisher
  {
    /**
     * {@inheritDoc}
     *
     * @return  configurable message support instance, never {@code null}
     */
    @Override
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport addMessage(@NotNull Message.WithCode message);


    /**
     * {@inheritDoc}
     *
     * @return  configurable message support instance, never {@code null}
     */
    @Override
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport addTemplate(@NotNull String name, @NotNull Message template);


    /**
     * Adds a message with code to this message support.
     *
     * @param code     message code, not {@code null} or empty
     * @param message  message text, not {@code null}
     *
     * @return  configurable message support instance, never {@code null}
     *
     * @throws DuplicateMessageException  in case a message with the same code already exists
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull ConfigurableMessageSupport addMessage(@NotNull String code,
                                                           @NotNull @Language("MessageFormat") String message) {
      return addMessage(getMessageAccessor().getMessageFactory().parseMessage(code, message));
    }


    /**
     * Convenience method for adding multiple pack resources.
     *
     * @param packResources  enumeration of pack resources, not {@code null}
     *
     * @return  configurable message support instance, never {@code null}
     *
     * @throws IOException  if an I/O error occurs
     *
     * @see #importMessages(InputStream...)
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport importMessages(@NotNull Enumeration<URL> packResources) throws IOException;


    /**
     * Convenience method for adding a single pack.
     *
     * @param packStream  pack stream, not {@code null}
     *
     * @return  configurable message support instance, never {@code null}
     *
     * @throws IOException  if an I/O error occurs
     *
     * @see #importMessages(InputStream...)
     */
    @Contract(value = "_ -> this", mutates = "this")
    default @NotNull ConfigurableMessageSupport importMessages(@NotNull InputStream packStream) throws IOException {
      return importMessages(new InputStream[] { packStream });
    }


    /**
     * Import multiple packs into this message builder.
     * <p>
     * When importing from multiple packs, this method is preferred as it shares map key/values,
     * message parts and messages for all packs, thus reducing the memory footprint.
     *
     * @param packStreams  array of pack streams, not {@code null}
     *
     * @return  configurable message support instance, never {@code null}
     *
     * @throws IOException  if an I/O error occurs
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport importMessages(@NotNull InputStream... packStreams) throws IOException;


    /**
     * Set the default {@code value} for configuration parameter {@code name}.
     * <p>
     * If a parameter formatter is looking for a boolean configuration value, which has not been
     * provided by the message parameter, the message accessor is used to get a default value.
     *
     * @param name   configuration parameter name, not {@code null} or empty
     * @param value  default value
     *
     * @return  configurable message support instance, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, boolean value);


    /**
     * Set the default {@code value} for configuration parameter {@code name}.
     * <p>
     * If a parameter formatter is looking for a long configuration value, which has not been
     * provided by the message parameter, the message accessor is used to get a default value.
     *
     * @param name   configuration parameter name, not {@code null} or empty
     * @param value  default value
     *
     * @return  configurable message support instance, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, long value);


    /**
     * Set the default {@code value} for configuration parameter {@code name}.
     * <p>
     * If a parameter formatter is looking for a string configuration value, which has not been
     * provided by the message parameter, the message accessor is used to get a default value.
     *
     * @param name   configuration parameter name, not {@code null} or empty
     * @param value  default value
     *
     * @return  configurable message support instance, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, @NotNull String value);


    /**
     * Set the default {@code value} for configuration parameter {@code name}.
     * <p>
     * If a parameter formatter is looking for a message configuration value, which has not been
     * provided by the message parameter, the message accessor is used to get a default value.
     *
     * @param name   configuration parameter name, not {@code null} or empty
     * @param value  default value
     *
     * @return  configurable message support instance, never {@code null}
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                  @NotNull Message.WithSpaces value);


    /**
     * Sets the default locale for this message support.
     *
     * @param locale  locale, not {@code null}
     *
     * @return  configurable message support instance, never {@code null}
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport setLocale(@NotNull Locale locale);


    /**
     * Sets the default locale for this message support.
     *
     * @param locale  locale, not {@code null}
     *
     * @return  configurable message support instance, never {@code null}
     */
    @Contract(value = "_ -> this", mutates = "this")
    default @NotNull ConfigurableMessageSupport setLocale(@NotNull String locale) {
      return setLocale(forLanguageTag(locale));
    }


    /**
     * Set a {@code messageHandler} for this message support.
     * <p>
     * On adding a message the message handler is invoked with the message code. If the handler
     * returns {@code true} the message is added to the message support. If the handler returns
     * {@code false} the message is not added to the message support.
     * <p>
     * Exceptions thrown by the message filter are relayed to the caller.
     *
     * @param messageFilter  message filter, not {@code null}
     *
     * @return  configurable message support instance, never {@code null}
     *
     * @see ConfigurableMessageSupport#addMessage(Message.WithCode)
     * @see ConfigurableMessageSupport#addMessage(String, String)
     * @see DuplicateMessageException
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport setMessageFilter(@NotNull MessageFilter messageFilter);


    /**
     * Set a {@code templateFilter} for this message support.
     * <p>
     * On adding a template the template filter is invoked with the template name and template
     * message. If the filter returns {@code true} the template is added to the message support.
     * If the filter returns {@code false} the template is not added to the message support.
     * <p>
     * Exceptions thrown by the template filter are relayed to the caller.
     *
     * @param templateFilter  template filter, not {@code null}
     *
     * @return  configurable message support instance, never {@code null}
     *
     * @see ConfigurableMessageSupport#addTemplate(String, Message)
     * @see DuplicateTemplateException
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull ConfigurableMessageSupport setTemplateFilter(@NotNull TemplateFilter templateFilter);
  }




  /**
   * This interface allows access to all messages and templates published to the message support.
   */
  interface MessageAccessor extends TemplateAccessor
  {
    /**
     * Returns the locale configured for this message support instance.
     *
     * @return  locale, never {@code null}
     *
     * @see MessageSupport.ConfigurableMessageSupport#setLocale(Locale)
     */
    @Contract(pure = true)
    @NotNull Locale getLocale();


    /**
     * Returns all codes contained in this message bundle.
     *
     * @return  unmodifiable set with all message codes, never {@code null}
     */
    @Contract(value = "-> new", pure = true)
    @NotNull Set<String> getMessageCodes();


    /**
     * Tells if this builder contains a message with {@code code}.
     *
     * @param code  message code to check, or {@code null}
     *
     * @return  {@code true} if {@code code} is not {@code null} and the bundle contains a
     *          message with this code, {@code false} otherwise
     */
    @Contract(value = "null -> false", pure = true)
    boolean hasMessageWithCode(String code);


    /**
     * Returns the message associated with {@code code}.
     *
     * @param code  message code, not {@code null}
     *
     * @return  message or {@code null} if no message with this code exists
     */
    @Contract(pure = true)
    Message.WithCode getMessageByCode(@NotNull String code);


    /**
     * Returns the best matching formatter for the given {@code type}.
     *
     * @param type  type, never {@code null}
     *
     * @return  unmodifiable list of formatters for the given {@code type},
     *          never {@code null} and never empty
     */
    @Contract(value = "_ -> new", pure = true)
    default @NotNull ParameterFormatter[] getFormatters(@NotNull Class<?> type) {
      return getFormatters(null, type);
    }


    /**
     * Returns a prioritized list of matching formatter for the given {@code format} and
     * {@code type}
     * <p>
     * If {@code format} matches a named formatter it always takes precedence over {@code type}.
     *
     * @param format  formatter name
     * @param type    type, never {@code null}
     *
     * @return  unmodifiable prioritized list of formatters for the given {@code format} and
     *          {@code type}, never {@code null} and never empty
     */
    @Contract(value = "_, _ -> new", pure = true)
    @NotNull ParameterFormatter[] getFormatters(String format, @NotNull Class<?> type);


    /**
     * Returns the default configuration for the parameter identified by {@code name}.
     *
     * @param name  parameter name, not {@code null}
     *
     * @return  default configuration value, or {@code null} if no default configuration has been
     *          set for parameter {@code name}
     *
     * @see ConfigurableMessageSupport#setDefaultParameterConfig(String, boolean)
     * @see ConfigurableMessageSupport#setDefaultParameterConfig(String, long)
     * @see ConfigurableMessageSupport#setDefaultParameterConfig(String, String)
     * @see ConfigurableMessageSupport#setDefaultParameterConfig(String, Message.WithSpaces)
     */
    @Contract(pure = true)
    ConfigValue getDefaultParameterConfig(@NotNull String name);


    /**
     * Returns the message factory.
     *
     * @return  message factory, never {@code null}
     */
    @Contract(pure = true)
    @NotNull MessageFactory getMessageFactory();
  }




  /**
   * This interface allows access to all templates published to the message support.
   */
  interface TemplateAccessor
  {
    /**
     * Returns all templates contained in this message builder.
     *
     * @return  unmodifiable set with all template names, never {@code null}
     */
    @Contract(value = "-> new", pure = true)
    @NotNull Set<String> getTemplateNames();


    /**
     * Returns the template message associated with {@code name}.
     *
     * @param name  template name
     *
     * @return  template message or {@code null} if no template with this name exists
     */
    @Contract(pure = true)
    Message getTemplateByName(@NotNull String name);


    /**
     * Tells if this builder contains a template with {@code name}.
     *
     * @param name  template name to check, or {@code null}
     *
     * @return  {@code true} if {@code name} is not {@code null} and this builder contains a
     *          template with this name, {@code false} otherwise
     */
    @Contract(value = "null -> false", pure = true)
    boolean hasTemplateWithName(String name);


    /**
     * Returns a collection of template names, that are referenced from messages but have not
     * been published to this message support.
     * <p>
     * The messages that are analysed can be filtered by providing a {@code messageCodeFilter}.
     * If this parameter is {@code null} all known messages are analysed.
     *
     * @param messageCodeFilter  message code filter or {@code null} to include all messages
     *
     * @return  a collections with referenced template names, that are unknown to this
     *          message support, never {@code null}
     */
    @NotNull Set<String> findMissingTemplates(Predicate<String> messageCodeFilter);
  }




  /**
   * Message publisher provides methods for registering messages and templates.
   * <p>
   * The publisher is used intensively by message adopters (see package
   * {@code de.sayayi.lib.message.adopter}).
   *
   * @author Jeroen Gremmen
   * @since 0.8.0
   */
  interface MessagePublisher
  {
    /**
     * Adds a message with code to this publisher.
     *
     * @param message  message with code, not {@code null}
     *
     * @return  this message publisher instance, never {@code null}
     *
     * @throws DuplicateMessageException  in case a message with the same code already exists
     */
    @Contract(value = "_ -> this", mutates = "this")
    @NotNull MessagePublisher addMessage(@NotNull Message.WithCode message);


    /**
     * Adds a template identified by {@code name} to this publisher.
     *
     * @param name      template name, not {@code null}
     * @param template  template message, not {@code null}
     *
     * @return  this message publisher instance, never {@code null}
     *
     * @throws DuplicateTemplateException  in case a template with the same name already exists
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull MessagePublisher addTemplate(@NotNull String name, @NotNull Message template);
  }




  /**
   * Interface used for filtering messages.
   *
   * @author Jeroen Gremmen
   * @since 0.8.0
   */
  @FunctionalInterface
  interface MessageFilter
  {
    /**
     * Decides if {@code message} is filtered or not.
     *
     * @param message  message to analyse, not {@code null}
     *
     * @return  {@code true} if the message will be included,
     *          {@code false} if the message will be excluded
     */
    boolean filter(@NotNull Message.WithCode message);
  }




  /**
   * Interface used for filtering templates.
   *
   * @author Jeroen Gremmen
   * @since 0.8.0
   */
  @FunctionalInterface
  interface TemplateFilter
  {
    /**
     * Decides if {@code template} with {@code name} is filtered or not.
     *
     * @param name      template name, not {@code null}
     * @param template  template message to analyse, not {@code null}
     *
     * @return  {@code true} if the template will be included,
     *          {@code false} if the template will be excluded
     */
    boolean filter(@NotNull String name, @NotNull Message template);
  }




  /**
   * Interface with a single method mimicking the standard exception constructor for a message.
   *
   * @param <X>  exception type created by this constructor
   *
   * @author Jeroen Gremmen
   * @since 0.8.0
   */
  @FunctionalInterface
  interface ExceptionConstructor<X extends Exception>
  {
    /**
     * Create a new exception for the given {@code message}.
     *
     * @param message  formatted message, not {@code null}
     *
     * @return  exception instance, never {@code null}
     */
    @NotNull X construct(@NotNull String message);
  }




  /**
   * Interface with a single method mimicking the standard exception constructor for a message
   * and cause.
   *
   * @param <X>  exception type created by this constructor
   *
   * @author Jeroen Gremmen
   * @since 0.8.3
   */
  @FunctionalInterface
  interface ExceptionConstructorWithCause<X extends Exception>
  {
    /**
     * Create a new exception for the given {@code message} and {@code cause}.
     *
     * @param message  formatted message, not {@code null}
     * @param cause    root cause of the exception
     *
     * @return  exception instance, never {@code null}
     */
    @NotNull X construct(@NotNull String message, Throwable cause);
  }
}
