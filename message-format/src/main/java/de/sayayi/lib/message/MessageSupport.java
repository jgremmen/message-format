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

import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.parameter.value.ConfigValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public interface MessageSupport
{
  /**
   * Returns the message support accessor instance.
   *
   * @return  message support accessor, never {@code null}
   */
  @Contract(pure = true)
  @NotNull MessageSupportAccessor getAccessor();


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


  @Contract(value = "_ -> new", pure = true)
  @NotNull MessageConfigurer<Message> message(@NotNull String message);


  @Contract(value = "_ -> new", pure = true)
  <M extends Message> @NotNull MessageConfigurer<M> message(@NotNull M message);


  /**
   * <p>
   *   Export all messages and templates from this message support to a compact binary representation.
   *   This way a message support can be prepared once and loaded very quickly by importing the packed
   *   messages at runtime.
   * </p>
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
   * <p>
   *   Pack all messages (optionally filtering them using a {@code messageCodeFilter} from this bundle into a
   *   compact binary representation. This way message bundles can be prepared once and loaded very quickly
   *   by adding the packed messages to another message bundle at runtime.
   * </p>
   * <p>
   *   Parameter {@code compress} switches GZip on/off, potentially reducing the packed size even more. For smaller
   *   bundles the compression may not be substantial as the binary representation does some extensive bit-packing
   *   already.
   * </p>
   *
   * @param stream             pack output stream, not {@code null}
   * @param compress           {@code true} compress pack, {@code false} do not compress pack
   * @param messageCodeFilter  optional predicate for selecting message codes. If {@code null} all messages from
   *                           this builder will be selected
   *
   * @throws IOException  if an I/O error occurs
   */
  void exportMessages(@NotNull OutputStream stream, boolean compress, Predicate<String> messageCodeFilter)
      throws IOException;




  @SuppressWarnings("UnstableApiUsage")
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


    @Contract(value = "_ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull Map<String,Object> parameterValues)
    {
      parameterValues.forEach(this::with);
      return this;
    }


    @Contract(value = "_ -> this", mutates = "this")
    default @NotNull MessageConfigurer<M> with(@NotNull Properties properties)
    {
      properties.forEach((key,value) -> with((String)key, value));
      return this;
    }


    @Contract(value = "_ -> this", mutates = "this")
    @NotNull MessageConfigurer<M> locale(Locale locale);


    @Contract(value = "_ -> this", mutates = "this")
    @NotNull MessageConfigurer<M> locale(String locale);


    @Contract(pure = true)
    @NotNull String format();


    @Contract("_ -> fail")
    <T extends Exception> void throwFormatted(@NotNull Function<String,T> constructor);
  }




  @SuppressWarnings("UnstableApiUsage")
  interface ConfigurableMessageSupport extends MessageSupport, MessagePublisher
  {
    /**
     * Convenience method for adding multiple pack resources.
     *
     * @param packResources  enumeration of pack resources, not {@code null}
     *
     * @throws IOException  if an I/O error occurs
     *
     * @see #importMessages(InputStream...)
     */
    @Contract(mutates = "this")
    void importMessages(@NotNull Enumeration<URL> packResources) throws IOException;


    /**
     * Convenience method for adding a single pack.
     *
     * @param packStream  pack stream, not {@code null}
     *
     * @throws IOException  if an I/O error occurs
     *
     * @see #importMessages(InputStream...)
     */
    @Contract(mutates = "this")
    default void importMessages(@NotNull InputStream packStream) throws IOException {
      importMessages(new InputStream[] { packStream });
    }


    /**
     * <p>
     *   Import multiple packs into this message builder.
     * </p>
     * <p>
     *   When importing from multiple packs, this method is preferred as it shares map key/values,
     *   message parts and messages for all packs, thus reducing the memory footprint.
     * </p>
     *
     * @param packStreams  array of pack streams, not {@code null}
     *
     * @throws IOException  if an I/O error occurs
     */
    @Contract(mutates = "this")
    void importMessages(@NotNull InputStream... packStreams) throws IOException;


    @Contract(mutates = "this")
    @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, boolean value);


    @Contract(mutates = "this")
    @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name, long value);


    @Contract(mutates = "this")
    @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                  @NotNull String value);


    @Contract(mutates = "this")
    @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                  @NotNull Message.WithSpaces value);


    @Contract(mutates = "this")
    @NotNull ConfigurableMessageSupport setLocale(@NotNull Locale locale);


    @Contract(mutates = "this")
    @NotNull ConfigurableMessageSupport setLocale(@NotNull String locale);


    @Contract(mutates = "this")
    @NotNull ConfigurableMessageSupport setMessageHandler(@NotNull Predicate<String> messageHandler);


    @Contract(mutates = "this")
    @NotNull ConfigurableMessageSupport setTemplateHandler(@NotNull Predicate<String> templateHandler);
  }




  interface MessageSupportAccessor
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
     * @return  set with all message codes, never {@code null}
     */
    @Contract(value = "-> new", pure = true)
    @UnmodifiableView
    @NotNull Set<String> getMessageCodes();


    /**
     * Returns all templates contained in this message builder.
     *
     * @return  set with all template names, never {@code null}
     */
    @Contract(value = "-> new", pure = true)
    @UnmodifiableView
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
     * Tells if this builder contains a message with {@code code}.
     *
     * @param code  message code to check, or {@code null}
     *
     * @return  {@code true} if {@code code} is not {@code null} and the bundle contains a message with
     *          this code, {@code false} otherwise
     */
    @Contract(value = "null -> false", pure = true)
    boolean hasMessageWithCode(String code);


    /**
     * Tells if this builder contains a template with {@code name}.
     *
     * @param name  template name to check, or {@code null}
     *
     * @return  {@code true} if {@code name} is not {@code null} and this builder contains a template with
     *          this name, {@code false} otherwise
     */
    @Contract(value = "null -> false", pure = true)
    boolean hasTemplateWithName(String name);


    /**
     * Returns the best matching formatter for the given {@code type}.
     *
     * @param type  type, never {@code null}
     *
     * @return  formatter for the given {@code type}, never {@code null}
     */
    @Contract(pure = true)
    @Unmodifiable
    default @NotNull List<ParameterFormatter> getFormatters(@NotNull Class<?> type) {
      return getFormatters(null, type);
    }


    /**
     * <p>
     *   Returns a prioritized list of matching formatter for the given {@code format} and {@code type}
     * </p>
     * <p>
     *   If {@code format} matches a named formatter it always takes precedence over {@code type}.
     * </p>
     *
     * @param format  formatter name
     * @param type    type, never {@code null}
     *
     * @return  prioritized list of formatters for the given {@code format} and {@code type}, never {@code null}
     */
    @Contract(pure = true)
    @Unmodifiable
    @NotNull List<ParameterFormatter> getFormatters(String format, @NotNull Class<?> type);


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
   * Message publisher provides methods for registering messages and templates.
   * <p>
   * The publisher is used intensively by message adopters (see package {@code de.sayayi.lib.message.adopter}).
   *
   * @author Jeroen Gremmen
   * @since 0.8.0
   */
  @SuppressWarnings("UnstableApiUsage")
  interface MessagePublisher
  {
    @Contract(mutates = "this")
    void addMessage(@NotNull Message.WithCode message);


    @Contract(mutates = "this")
    void addTemplate(@NotNull String name, @NotNull Message template);
  }
}
