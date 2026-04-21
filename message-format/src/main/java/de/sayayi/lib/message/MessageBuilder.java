/*
 * Copyright 2026 Jeroen Gremmen
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

import de.sayayi.lib.message.internal.InternalMessageBuilder;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;


/**
 * Fluent builder interface for constructing {@link Message} instances programmatically.
 * <p>
 * The builder provides methods for the four message part types: literal text, parameter references, post-formatter
 * invocations and template references. Sub-builders ({@link ParameterBuilder}, {@link PostFormatterBuilder},
 * {@link TemplateBuilder}) extend this interface, so the common message-building methods ({@link #text(String)},
 * {@link #parameter(String)}, {@link #postFormatter(String)}, {@link #template(String)}, {@link #build()},
 * {@link #buildWithCode(String)}) are available at every level. Calling a message-building method on a sub-builder
 * finalizes the current part before continuing.
 * <p>
 * Consecutive {@link #text(String)} calls are automatically merged into a single text part.
 * <p>
 * Instances are <strong>not thread-safe</strong>. A builder must only be used from a single thread and must not be
 * reused after calling {@link #build()} or {@link #buildWithCode(String)}.
 * <p>
 * Example usage:
 * <pre>{@code
 *   Message.WithSpaces message = MessageBuilder.create()
 *       .text("Hello")
 *       .parameter("name")
 *           .withFormat("string")
 *           .spaceBefore()
 *       .text("!")
 *       .build();
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 *
 * @see Message
 * @see MessageFactory#messageBuilder()
 */
public sealed interface MessageBuilder
    permits InternalMessageBuilder,
            MessageBuilder.TextBuilder,
            MessageBuilder.ParameterBuilder,
            MessageBuilder.PostFormatterBuilder,
            MessageBuilder.TemplateBuilder
{
  /**
   * Appends literal text to the message being built. Consecutive calls to this method are automatically merged into
   * a single text part.
   * <p>
   * When called on a sub-builder, the current part is finalized first.
   *
   * @param text  literal text to append, not {@code null}
   *
   * @return  text builder for optional space configuration, never {@code null}
   */
  @NotNull TextBuilder text(@NotNull String text);


  /**
   * Starts building a parameter part with the given {@code name}.
   * <p>
   * When called on a sub-builder, the current part is finalized first.
   *
   * @param name  parameter name, not {@code null} or empty
   *
   * @return  parameter builder for further configuration, never {@code null}
   */
  @NotNull ParameterBuilder parameter(@NotNull String name);


  /**
   * Starts building a post-formatter part with the given formatter {@code name}.
   * <p>
   * When called on a sub-builder, the current part is finalized first.
   *
   * @param name  post-formatter name, not {@code null} or empty
   *
   * @return  post-formatter builder for further configuration, never {@code null}
   */
  @NotNull PostFormatterBuilder postFormatter(@NotNull String name);


  /**
   * Starts building a template reference part with the given template {@code name}.
   * <p>
   * When called on a sub-builder, the current part is finalized first.
   *
   * @param name  template name, not {@code null} or empty
   *
   * @return  template builder for further configuration, never {@code null}
   */
  @NotNull TemplateBuilder template(@NotNull String name);


  /**
   * Builds and returns the message.
   * <p>
   * When called on a sub-builder, the current part is finalized first.
   *
   * @return  the constructed message, never {@code null}
   */
  @NotNull Message.WithSpaces build();


  /**
   * Builds and returns the message with the given {@code code}.
   * <p>
   * When called on a sub-builder, the current part is finalized first.
   *
   * @param code  message code, not {@code null} or empty
   *
   * @return  the constructed message with code, never {@code null}
   */
  @NotNull Message.WithCode buildWithCode(@NotNull String code);


  /**
   * Creates a new message builder using the
   * {@linkplain MessageFactory#NO_CACHE_INSTANCE shared no-cache message factory}.
   *
   * @return  new message builder, never {@code null}
   */
  @Contract("-> new")
  static @NotNull MessageBuilder create() {
    return new InternalMessageBuilder(NO_CACHE_INSTANCE);
  }


  /**
   * Creates a new message builder using the given {@code messageFactory}.
   *
   * @param messageFactory  message factory to use, not {@code null}
   *
   * @return  new message builder, never {@code null}
   */
  @Contract("_ -> new")
  static @NotNull MessageBuilder create(@NotNull MessageFactory messageFactory) {
    return new InternalMessageBuilder(messageFactory);
  }




  /**
   * Mixin interface for sub-builders that support leading and trailing space configuration.
   *
   * @param <S>  the self type of the sub-builder
   *
   * @since 0.21.0
   */
  @SuppressWarnings("UnusedReturnValue")
  sealed interface SpacedBuilder<S extends SpacedBuilder<S>>
      permits TextBuilder, ParameterBuilder, PostFormatterBuilder, TemplateBuilder,
              InternalMessageBuilder.AbstractSpacedBuilder
  {
    /**
     * Adds a leading space to this message part.
     *
     * @return  this builder, never {@code null}
     */
    @Contract("-> this")
    @NotNull S spaceBefore();


    /**
     * Adds a trailing space to this message part.
     *
     * @return  this builder, never {@code null}
     */
    @Contract("-> this")
    @NotNull S spaceAfter();


    /**
     * Adds both a leading and trailing space to this message part.
     *
     * @return  this builder, never {@code null}
     */
    @Contract("-> this")
    default @NotNull S spacesAround()
    {
      spaceBefore();
      return spaceAfter();
    }
  }




  /**
   * Sub-builder returned by {@link #text(String)} that allows optional leading and trailing space configuration on
   * the text part. Extends {@link MessageBuilder} so that calling any message-building method finalizes the current
   * text part before continuing.
   *
   * @since 0.21.0
   */
  sealed interface TextBuilder
      extends MessageBuilder, SpacedBuilder<TextBuilder>
      permits InternalMessageBuilder.TextBuilderImpl {
  }




  /**
   * Mixin interface for sub-builders that support typed configuration values.
   *
   * @param <S>  the self type of the sub-builder
   *
   * @since 0.21.0
   */
  sealed interface ConfigurableBuilder<S extends ConfigurableBuilder<S>>
      permits ParameterBuilder, PostFormatterBuilder, InternalMessageBuilder.AbstractConfigurableBuilder
  {
    /**
     * Adds a string configuration value.
     *
     * @param name   config name, not {@code null}
     * @param value  string value, not {@code null}
     *
     * @return  this builder, never {@code null}
     */
    @Contract("_, _ -> this")
    @NotNull S configString(@NotNull String name, @NotNull String value);


    /**
     * Adds a boolean configuration value.
     *
     * @param name   config name, not {@code null}
     * @param value  boolean value
     *
     * @return  this builder, never {@code null}
     */
    @Contract("_, _ -> this")
    @NotNull S configBool(@NotNull String name, boolean value);


    /**
     * Adds a numeric configuration value.
     *
     * @param name   config name, not {@code null}
     * @param value  numeric value
     *
     * @return  this builder, never {@code null}
     */
    @Contract("_, _ -> this")
    @NotNull S configNumber(@NotNull String name, long value);


    /**
     * Adds a message configuration value.
     *
     * @param name     config name, not {@code null}
     * @param message  message value, not {@code null}
     *
     * @return  this builder, never {@code null}
     */
    @Contract("_, _ -> this")
    @NotNull S configMessage(@NotNull String name, @NotNull Message.WithSpaces message);
  }




  /**
   * Sub-builder for configuring a parameter message part.
   * <p>
   * Inherits all message-building methods from {@link MessageBuilder}, configuration methods from
   * {@link ConfigurableBuilder} and space methods from {@link SpacedBuilder}. Calling any inherited
   * {@code MessageBuilder} method finalizes the current parameter part before continuing.
   *
   * @since 0.21.0
   */
  sealed interface ParameterBuilder
      extends MessageBuilder, SpacedBuilder<ParameterBuilder>, ConfigurableBuilder<ParameterBuilder>
      permits InternalMessageBuilder.ParameterBuilderImpl
  {
    /**
     * Sets the formatter name for this parameter.
     *
     * @param format  formatter name, not {@code null}
     *
     * @return  this parameter builder, never {@code null}
     */
    @Contract("_ -> this")
    @NotNull ParameterBuilder withFormat(@NotNull String format);


    /**
     * Starts building a boolean map entry for this parameter. The returned builder allows specifying the map value
     * message. Boolean keys always use equality comparison.
     *
     * @param key  boolean key value
     *
     * @return  map value builder, never {@code null}
     */
    @NotNull MapValueBuilder mapBool(boolean key);


    /**
     * Starts building an empty map entry for this parameter. The returned builder allows optionally specifying the
     * equality operator ({@link MapEqualityBuilder#ne() ne}) before providing the map value. The default operator is
     * {@code eq}.
     *
     * @return  equality map entry builder, never {@code null}
     */
    @NotNull MapEqualityBuilder mapEmpty();


    /**
     * Starts building a null map entry for this parameter. The returned builder allows optionally specifying the
     * equality operator ({@link MapEqualityBuilder#ne() ne}) before providing the map value. The default operator
     * is {@code eq}.
     *
     * @return  equality map entry builder, never {@code null}
     */
    @NotNull MapEqualityBuilder mapNull();


    /**
     * Starts building a numeric map entry for this parameter. The returned builder allows optionally specifying a
     * relational operator before providing the map value. The default operator is {@code eq}.
     *
     * @param number  numeric key value
     *
     * @return  relational map entry builder, never {@code null}
     */
    @NotNull MapRelationalBuilder mapNumber(long number);


    /**
     * Starts building a string map entry for this parameter. The returned builder allows optionally specifying a
     * relational operator before providing the map value. The default operator is {@code eq}.
     *
     * @param string  string key value, not {@code null}
     *
     * @return  relational map entry builder, never {@code null}
     */
    @NotNull MapRelationalBuilder mapString(@NotNull String string);


    /**
     * Starts building the default map entry for this parameter. The returned builder allows specifying the map
     * value message.
     *
     * @return  map value builder, never {@code null}
     */
    @NotNull MapValueBuilder mapDefault();
  }




  /**
   * Builder for providing the map value message. This is the terminal step in the map entry builder chain.
   *
   * @since 0.21.0
   */
  sealed interface MapValueBuilder
      permits MapEqualityBuilder, InternalMessageBuilder.MapValueBuilderImpl
  {
    /**
     * Sets the map value message and returns to the enclosing parameter builder.
     *
     * @param message  map value message, not {@code null}
     *
     * @return  the enclosing parameter builder, never {@code null}
     */
    @NotNull ParameterBuilder message(@NotNull Message.WithSpaces message);


    /**
     * Sets the map value by parsing the given message format string and returns to the enclosing parameter builder.
     *
     * @param message  message format string, not {@code null}
     *
     * @return  the enclosing parameter builder, never {@code null}
     */
    @NotNull ParameterBuilder message(@NotNull @Language("MessageFormat") String message);
  }




  /**
   * Builder for map entries that support equality operators ({@code eq}/{@code ne}). Used for {@code empty} and
   * {@code null} map keys. The default operator is {@code eq}.
   * <p>
   * The operator methods ({@link #eq()}, {@link #ne()}) are optional modifiers that return this builder. Call
   * {@link #message(Message.WithSpaces)} or {@link #message(String)} to finalize the entry.
   *
   * @since 0.21.0
   */
  sealed interface MapEqualityBuilder extends MapValueBuilder
      permits MapRelationalBuilder, InternalMessageBuilder.MapEqualityBuilderImpl
  {
    /**
     * Sets the key operator to "equal". This is the default and does not need to be called explicitly.
     *
     * @return  this builder, never {@code null}
     */
    @Contract("-> this")
    @NotNull MapEqualityBuilder eq();


    /**
     * Sets the key operator to "not equal".
     *
     * @return  this builder, never {@code null}
     */
    @Contract("-> this")
    @NotNull MapEqualityBuilder ne();
  }




  /**
   * Builder for map entries that support relational operators ({@code eq}, {@code ne}, {@code lt}, {@code lte},
   * {@code gt}, {@code gte}). Used for {@code number} and {@code string} map keys. The default operator is {@code eq}.
   * <p>
   * The operator methods are optional modifiers that return this builder. Call {@link #message(Message.WithSpaces)}
   * or {@link #message(String)} to finalize the entry.
   *
   * @since 0.21.0
   */
  sealed interface MapRelationalBuilder extends MapEqualityBuilder
      permits InternalMessageBuilder.MapRelationalBuilderImpl
  {
    /**
     * Sets the key operator to "less than".
     *
     * @return  this builder, never {@code null}
     */
    @Contract("-> this")
    @NotNull MapRelationalBuilder lt();


    /**
     * Sets the key operator to "less than or equal".
     *
     * @return  this builder, never {@code null}
     */
    @Contract("-> this")
    @NotNull MapRelationalBuilder lte();


    /**
     * Sets the key operator to "greater than".
     *
     * @return  this builder, never {@code null}
     */
    @Contract("-> this")
    @NotNull MapRelationalBuilder gt();


    /**
     * Sets the key operator to "greater than or equal".
     *
     * @return  this builder, never {@code null}
     */
    @Contract("-> this")
    @NotNull MapRelationalBuilder gte();
  }




  /**
   * Sub-builder for configuring a post-formatter message part.
   * <p>
   * The inner message to be post-formatted is configured via {@link #withMessage(Consumer)} which provides a nested
   * {@link MessageBuilder} callback. Inherits all message-building methods from {@link MessageBuilder}, configuration
   * methods from {@link ConfigurableBuilder} and space methods from {@link SpacedBuilder}.
   *
   * @since 0.21.0
   */
  sealed interface PostFormatterBuilder
      extends MessageBuilder, SpacedBuilder<PostFormatterBuilder>, ConfigurableBuilder<PostFormatterBuilder>
      permits InternalMessageBuilder.PostFormatterBuilderImpl
  {
    /**
     * Configures the inner message to be post-formatted using a nested builder callback.
     *
     * @param messageConfigurer  callback that receives a nested {@link MessageBuilder} for constructing the inner
     *                           message, not {@code null}
     *
     * @return  this post-formatter builder, never {@code null}
     */
    @Contract("_ -> this")
    @NotNull PostFormatterBuilder withMessage(@NotNull Consumer<MessageBuilder> messageConfigurer);
  }




  /**
   * Sub-builder for configuring a template reference message part.
   * <p>
   * Inherits all message-building methods from {@link MessageBuilder} and space methods from {@link SpacedBuilder}.
   * Calling any inherited {@code MessageBuilder} method finalizes the current template part before continuing.
   *
   * @since 0.21.0
   */
  sealed interface TemplateBuilder
      extends MessageBuilder, SpacedBuilder<TemplateBuilder>
      permits InternalMessageBuilder.TemplateBuilderImpl
  {
    /**
     * Adds a default string parameter value for the template.
     *
     * @param name   parameter name, not {@code null}
     * @param value  default string value, not {@code null}
     *
     * @return  this template builder, never {@code null}
     */
    @Contract("_, _ -> this")
    @NotNull TemplateBuilder withDefaultParameterString(@NotNull String name, @NotNull String value);


    /**
     * Adds a default boolean parameter value for the template.
     *
     * @param name   parameter name, not {@code null}
     * @param value  default boolean value
     *
     * @return  this template builder, never {@code null}
     */
    @Contract("_, _ -> this")
    @NotNull TemplateBuilder withDefaultParameterBool(@NotNull String name, boolean value);


    /**
     * Adds a default numeric parameter value for the template.
     *
     * @param name   parameter name, not {@code null}
     * @param value  default numeric value
     *
     * @return  this template builder, never {@code null}
     */
    @Contract("_, _ -> this")
    @NotNull TemplateBuilder withDefaultParameterNumber(@NotNull String name, long value);


    /**
     * Adds a default message parameter value for the template.
     *
     * @param name     parameter name, not {@code null}
     * @param message  default message value, not {@code null}
     *
     * @return  this template builder, never {@code null}
     */
    @Contract("_, _ -> this")
    @NotNull TemplateBuilder withDefaultParameterMessage(@NotNull String name, @NotNull Message.WithSpaces message);


    /**
     * Adds a parameter delegate mapping. When the template references the parameter named {@code templateParam}, it
     * will be delegated to the enclosing message's parameter named {@code messageParam}.
     *
     * @param templateParam  parameter name used in the template, not {@code null}
     * @param messageParam   parameter name in the enclosing message, not {@code null}
     *
     * @return  this template builder, never {@code null}
     */
    @Contract("_, _ -> this")
    @NotNull TemplateBuilder withParameterDelegate(@NotNull String templateParam, @NotNull String messageParam);
  }
}
