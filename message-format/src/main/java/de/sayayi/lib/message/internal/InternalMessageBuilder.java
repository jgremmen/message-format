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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageBuilder;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.part.config.MessagePartConfig;
import de.sayayi.lib.message.internal.part.map.MessagePartMap;
import de.sayayi.lib.message.internal.part.map.key.*;
import de.sayayi.lib.message.internal.part.parameter.ParameterPart;
import de.sayayi.lib.message.internal.part.post.PostFormatterPart;
import de.sayayi.lib.message.internal.part.template.TemplatePart;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MapKey.CompareType;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextJoiner;
import de.sayayi.lib.message.part.TypedValue;
import de.sayayi.lib.message.template.Template;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static de.sayayi.lib.message.part.MapKey.CompareType.*;
import static de.sayayi.lib.message.part.TextPartFactory.addSpaces;
import static de.sayayi.lib.message.part.TextPartFactory.spacedText;
import static de.sayayi.lib.message.util.MessageUtil.isKebabCaseName;
import static de.sayayi.lib.message.util.MessageUtil.isKebabOrLowerCamelCaseName;
import static java.util.Objects.requireNonNull;


/**
 * Internal {@link MessageBuilder} implementation for programmatically constructing message-format messages.
 * <p>
 * The builder collects literal text, parameter references, post-formatters and template references and turns them
 * into the internal {@link Message} and {@link Template} implementations used by this package. Nested builder types
 * in this class capture the state for individual parts until they are flushed into the resulting message structure.
 * <p>
 * This class is <strong>not thread-safe</strong>. A builder instance must only be used from a single thread and must
 * not be reused after calling {@link #build()} or {@link #buildWithCode(String)}.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 *
 * @see MessageBuilder
 * @see MessageFactory#messageBuilder()
 */
public final class InternalMessageBuilder implements MessageBuilder
{
  /** Message factory used to parse nested messages and create coded messages. */
  private final @NotNull MessageFactory messageFactory;

  /** Collected parts of the message currently being assembled. */
  private final @NotNull List<MessagePart> parts;

  /** Flush callback for the currently active part builder, if any. */
  private Runnable activePartFlusher;

  /** Indicates whether this builder has already produced its final message. */
  private boolean built;


  /**
   * Construct a new builder using the given {@code messageFactory}.
   *
   * @param messageFactory  message factory, not {@code null}
   */
  public InternalMessageBuilder(@NotNull MessageFactory messageFactory)
  {
    this.messageFactory = requireNonNull(messageFactory, "messageFactory must not be null");

    parts = new ArrayList<>();
  }


  /**
   * Starts a literal text part and makes it the active part of this message.
   *
   * @param text  literal text to append, not {@code null}
   *
   * @return  builder for configuring the new text part, never {@code null}
   *
   * @throws IllegalStateException if this builder has already been built
   */
  @Override
  public @NotNull TextBuilder text(@NotNull String text)
  {
    checkNotBuilt();
    flushActivePart();

    final var builder = new TextBuilderImpl(requireNonNull(text, "text must not be null"));

    activePartFlusher = builder::flush;

    return builder;
  }


  /**
   * Starts a parameter part and makes it the active part of this message.
   *
   * @param name  parameter name, not {@code null}
   *
   * @return  builder for configuring the new parameter part, never {@code null}
   *
   * @throws IllegalArgumentException if {@code name} does not use a supported naming convention
   * @throws IllegalStateException if this builder has already been built
   */
  @Override
  public @NotNull ParameterBuilder parameter(@NotNull String name)
  {
    checkNotBuilt();
    flushActivePart();

    final var builder = new ParameterBuilderImpl(messageFactory, name);

    activePartFlusher = builder::flush;

    return builder;
  }


  /**
   * Starts a post-formatter part and makes it the active part of this message.
   *
   * @param name  post-formatter name, not {@code null}
   *
   * @return  builder for configuring the new post-formatter part, never {@code null}
   *
   * @throws IllegalArgumentException if {@code name} does not use kebab-case
   * @throws IllegalStateException if this builder has already been built
   */
  @Override
  public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
  {
    checkNotBuilt();
    flushActivePart();

    final var builder = new PostFormatterBuilderImpl(messageFactory, name);

    activePartFlusher = builder::flush;

    return builder;
  }


  /**
   * Starts a template reference part and makes it the active part of this message.
   *
   * @param name  template name, not {@code null}
   *
   * @return  builder for configuring the new template part, never {@code null}
   *
   * @throws IllegalArgumentException if {@code name} does not use kebab-case
   * @throws IllegalStateException if this builder has already been built
   */
  @Override
  public @NotNull TemplateBuilder template(@NotNull String name)
  {
    checkNotBuilt();
    flushActivePart();

    final var builder = new TemplateBuilderImpl(name);

    activePartFlusher = builder::flush;

    return builder;
  }


  /**
   * Builds the configured message.
   *
   * @return  message assembled from the configured parts, never {@code null}
   *
   * @throws IllegalStateException if this builder has already been built
   */
  @Override
  public @NotNull Message.WithSpaces build()
  {
    checkNotBuilt();
    built = true;

    flushActivePart();

    if (parts.isEmpty())
      return EmptyMessage.INSTANCE;

    for(var i = 0; i < parts.size() - 1; i++)
      if (parts.get(i) instanceof Text first && parts.get(i + 1) instanceof Text)
      {
        final var joiner = new TextJoiner();

        joiner.add(first);

        var j = i + 1;
        for(var l = parts.size(); j < l && parts.get(j) instanceof Text next; j++)
          joiner.add(next);

        parts.subList(i, j).clear();
        parts.add(i, joiner.asSpacedText());
      }

    return parts.size() == 1 && parts.getFirst() instanceof Text textPart
        ? new TextMessage(textPart)
        : new CompoundMessage(parts);
  }


  /**
   * Builds the configured message and wraps it with the supplied code.
   *
   * @param code  message code to associate with the built message, not {@code null}
   *
   * @return  coded message, never {@code null}
   *
   * @throws IllegalStateException if this builder has already been built
   */
  @Override
  public @NotNull Message.WithCode buildWithCode(@NotNull String code) {
    return messageFactory.withCode(code, build());
  }


  /**
   * Builds the configured message and exposes it as a {@link Template}.
   *
   * @return  template backed by the built message, never {@code null}
   *
   * @throws IllegalStateException if this builder has already been built
   */
  @Override
  public @NotNull Template buildAsTemplate() {
    return new MessageTemplate(build());
  }


  /**
   * Flushes any active non-text sub-builder into the parts list.
   */
  private void flushActivePart()
  {
    if (activePartFlusher != null)
    {
      activePartFlusher.run();
      activePartFlusher = null;
    }
  }


  /**
   * Ensures this builder has not already been finalized by a call to {@link #build()} or
   * {@link #buildWithCode(String)}.
   *
   * @throws IllegalStateException  if this builder has already been used to build a message
   */
  private void checkNotBuilt()
  {
    if (built)
      throw new IllegalStateException("builder must not be reused after calling build() or buildWithCode()");
  }




  /**
   * Abstract base class for sub-builders that implement {@link SpacedBuilder}, providing common space configuration
   * fields and methods.
   *
   * @param <S>  the self type of the sub-builder
   *
   * @since 0.21.0
   */
  public abstract static non-sealed class AbstractSpacedBuilder<S extends SpacedBuilder<S>>
      implements SpacedBuilder<S>
  {
    /** Whether a space should be inserted before the part produced by this builder. */
    protected boolean spaceBefore;

    /** Whether a space should be inserted after the part produced by this builder. */
    protected boolean spaceAfter;


    /**
     * Marks the part produced by this builder to include a leading space when it is flushed.
     *
     * @return  this builder, never {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public @NotNull S spaceBefore()
    {
      spaceBefore = true;

      return (S)this;
    }


    /**
     * Marks the part produced by this builder to include a trailing space when it is flushed.
     *
     * @return  this builder, never {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public @NotNull S spaceAfter()
    {
      spaceAfter = true;

      return (S)this;
    }
  }




  /**
   * Default implementation of {@link TextBuilder}.
   * <p>
   * Adds a text part directly to the enclosing builder's parts list.
   *
   * @since 0.21.0
   */
  public final class TextBuilderImpl extends AbstractSpacedBuilder<TextBuilder> implements TextBuilder
  {
    /** Literal text contributed by this builder. */
    private final @NotNull String text;

    /** Indicates whether this text part has already been written to the enclosing builder. */
    private boolean flushed;


    /**
     * Construct a new text builder for the given literal text.
     *
     * @param text  literal text, not {@code null}
     */
    private TextBuilderImpl(@NotNull String text) {
      this.text = text;
    }


    /**
     * Adds this text part (including any space-before/space-after settings) to the enclosing builder's parts list.
     */
    private void flush()
    {
      if (!flushed)
      {
        flushed = true;
        parts.add(addSpaces(spacedText(text), spaceBefore, spaceAfter));
      }
    }


    /**
     * Flushes this part and starts a text part in the enclosing message builder.
     *
     * @param text  literal text for the next part, not {@code null}
     *
     * @return  builder for the next text part, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();

      return InternalMessageBuilder.this.text(requireNonNull(text, "text must not be null"));
    }


    /**
     * Flushes this part and starts a parameter part in the enclosing message builder.
     *
     * @param name  parameter name, not {@code null}
     *
     * @return  builder for the parameter part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use a supported naming convention
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.parameter(name);
    }


    /**
     * Flushes this part and starts a post-formatter part in the enclosing message builder.
     *
     * @param name  post-formatter name, not {@code null}
     *
     * @return  builder for the post-formatter part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.postFormatter(name);
    }


    /**
     * Flushes this part and starts a template part in the enclosing message builder.
     *
     * @param name  template name, not {@code null}
     *
     * @return  builder for the template part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.template(name);
    }


    /**
     * Flushes this part and builds the enclosing message.
     *
     * @return  built message, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();

      return InternalMessageBuilder.this.build();
    }


    /**
     * Flushes this part and builds a coded message from the enclosing builder.
     *
     * @param code  message code to associate with the built message, not {@code null}
     *
     * @return  coded message, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();

      return InternalMessageBuilder.this.buildWithCode(code);
    }


    /**
     * Flushes this part and builds the enclosing message as a template.
     *
     * @return  built template, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Template buildAsTemplate()
    {
      flush();

      return InternalMessageBuilder.this.buildAsTemplate();
    }
  }




  /**
   * Abstract base class for sub-builders that implement {@link ConfigurableBuilder}, providing common configuration
   * methods.
   *
   * @param <S>  the self type of the sub-builder
   *
   * @since 0.21.0
   */
  public abstract static non-sealed class AbstractConfigurableBuilder<S extends ConfigurableBuilder<S> & SpacedBuilder<S>>
      extends AbstractSpacedBuilder<S>
      implements ConfigurableBuilder<S>
  {
    /** Message factory used to convert string values into parsed message values. */
    protected final MessageFactory messageFactory;

    /** Configuration values collected for the part being built. */
    protected final @NotNull Map<String,TypedValue<?>> config;

    /** Indicates whether this part's configuration has been finalized. */
    protected boolean finalized;


    /**
     * Construct a new configurable builder with an empty configuration map.
     *
     * @param messageFactory  message factory used for parsing nested message values, not {@code null}
     */
    protected AbstractConfigurableBuilder(@NotNull MessageFactory messageFactory)
    {
      this.messageFactory = messageFactory;
      this.config = new LinkedHashMap<>();
    }


    /**
     * Stores a string configuration value for the current part.
     *
     * @param name   configuration name, not {@code null}
     * @param value  string value, not {@code null}
     *
     * @return  this builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     */
    @Override
    public @NotNull S configString(@NotNull String name, @NotNull String value) {
      return withConfig(name, new TypedValueString(messageFactory, value));
    }


    /**
     * Stores a boolean configuration value for the current part.
     *
     * @param name   configuration name, not {@code null}
     * @param value  boolean value to store
     *
     * @return  this builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     */
    @Override
    public @NotNull S configBool(@NotNull String name, boolean value) {
      return withConfig(name, value ? TypedValueBool.TRUE : TypedValueBool.FALSE);
    }


    /**
     * Stores a numeric configuration value for the current part.
     *
     * @param name   configuration name, not {@code null}
     * @param value  numeric value to store
     *
     * @return  this builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     */
    @Override
    public @NotNull S configNumber(@NotNull String name, long value) {
      return withConfig(name, new TypedValueNumber(value));
    }


    /**
     * Stores a nested message configuration value for the current part.
     *
     * @param name     configuration name, not {@code null}
     * @param message  message value, not {@code null}
     *
     * @return  this builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     */
    @Override
    public @NotNull S configMessage(@NotNull String name, @NotNull Message.WithSpaces message) {
      return withConfig(name, new TypedValueMessage(message));
    }


    /**
     * Adds a typed configuration value with the given name.
     *
     * @param name   configuration name (must follow kebab-case convention), not {@code null}
     * @param value  typed value, not {@code null}
     *
     * @return  this builder, never {@code null}
     *
     * @throws IllegalArgumentException  if {@code name} does not match the kebab-case naming convention
     * @throws IllegalStateException     if this part's configuration has already been finalized
     */
    @Contract("_, _ -> this")
    @SuppressWarnings("unchecked")
    private @NotNull S withConfig(@NotNull String name, @NotNull TypedValue<?> value)
    {
      if (finalized)
        throw new IllegalStateException("configuration builder has already been finalized");

      if (!isKebabCaseName(requireNonNull(name, "name must not be null")))
        throw new IllegalArgumentException("config name '" + name + "' must match the kebab-case naming convention");

      config.put(name, requireNonNull(value, "value must not be null"));

      return (S)this;
    }
  }




  /**
   * Default implementation of {@link ParameterBuilder}.
   * <p>
   * Collects the parameter name, optional format, configuration values and map entries and flushes them as a
   * {@link ParameterPart} when the next part is started or the message is built.
   *
   * @since 0.21.0
   */
  public final class ParameterBuilderImpl
      extends AbstractConfigurableBuilder<ParameterBuilder>
      implements ParameterBuilder
  {
    /** Name of the parameter represented by this builder. */
    private final @NotNull String name;

    /** Optional map entries keyed by value conditions for this parameter. */
    private final @NotNull Map<MapKey,TypedValue.MessageValue> map;

    /** Optional format name associated with the parameter. */
    private String format;

    /** Indicates whether this parameter part has already been written to the enclosing builder. */
    private boolean flushed;


    /**
     * Construct a new parameter builder for the given parameter name.
     *
     * @param messageFactory  message factory used to parse nested map messages, not {@code null}
     * @param name            parameter name (must follow kebab-case or lower camel-case convention), not {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not match the expected naming convention
     */
    private ParameterBuilderImpl(@NotNull MessageFactory messageFactory, @NotNull String name)
    {
      super(messageFactory);

      if (!isKebabOrLowerCamelCaseName(requireNonNull(name, "name must not be null")))
      {
        throw new IllegalArgumentException("parameter name '" + name +
            "' must match the kebab-case or lower camel-case naming convention");
      }

      this.name = name;
      this.map = new LinkedHashMap<>();
    }


    /**
     * Sets the optional format name for this parameter part.
     *
     * @param format  format name, not {@code null}
     *
     * @return  this parameter builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code format} does not use kebab-case
     */
    @Override
    public @NotNull ParameterBuilder withFormat(@NotNull String format)
    {
      if (!isKebabCaseName(requireNonNull(format, "format must not be null")))
        throw new IllegalArgumentException("format name '" + format + "' must match the kebab-case naming convention");

      this.format = format;

      return this;
    }


    /**
     * Starts a boolean keyed map entry for this parameter.
     *
     * @param key  boolean key for the entry
     *
     * @return  builder for supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapValueBuilder mapBool(boolean key) {
      return new MapValueBuilderImpl(this, key ? MapKeyBool.TRUE : MapKeyBool.FALSE);
    }


    /**
     * Starts a map entry keyed by an empty-value comparison for this parameter.
     *
     * @return  builder for choosing the comparison and supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapEqualityBuilder mapEmpty()
    {
      return new MapEqualityBuilderImpl(this,
          compareType -> compareType == EQ ? MapKeyEmpty.EQ : MapKeyEmpty.NE);
    }


    /**
     * Starts a map entry keyed by a null-value comparison for this parameter.
     *
     * @return  builder for choosing the comparison and supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapEqualityBuilder mapNull()
    {
      return new MapEqualityBuilderImpl(this,
          compareType -> compareType == EQ ? MapKeyNull.EQ : MapKeyNull.NE);
    }


    /**
     * Starts a numeric map entry for this parameter.
     *
     * @param number  numeric key value to compare against
     *
     * @return  builder for choosing the comparison and supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapRelationalBuilder mapNumber(long number)
    {
      return new MapRelationalBuilderImpl(this,
          compareType -> new MapKeyNumber(compareType, number));
    }


    /**
     * Starts a string keyed map entry for this parameter.
     *
     * @param string  string key value to compare against, not {@code null}
     *
     * @return  builder for choosing the comparison and supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapRelationalBuilder mapString(@NotNull String string)
    {
      requireNonNull(string, "string must not be null");

      return new MapRelationalBuilderImpl(this,
          compareType -> new MapKeyString(compareType, string));
    }


    /**
     * Adds a map entry with the given key and pre-built message value.
     *
     * @param key      map key, not {@code null}
     * @param message  message value, not {@code null}
     *
     * @return  this parameter builder, never {@code null}
     */
    private @NotNull ParameterBuilder addMapEntry(@NotNull MapKey key, @NotNull Message.WithSpaces message)
    {
      map.put(key, new TypedValueMessage(requireNonNull(message, "message must not be null")));

      return this;
    }


    /**
     * Adds a map entry with the given key and a message format string that will be parsed.
     *
     * @param key      map key, not {@code null}
     * @param message  message format string to parse, not {@code null}
     *
     * @return  this parameter builder, never {@code null}
     */
    private @NotNull ParameterBuilder addMapEntry(@NotNull MapKey key, @NotNull String message) {
      return addMapEntry(key, messageFactory.parseMessage(requireNonNull(message, "message must not be null")));
    }


    /**
     * Adds a map entry with the given key and a message built using a nested builder callback.
     *
     * @param key                map key, not {@code null}
     * @param messageConfigurer  callback that receives a nested {@link MessageBuilder}, not {@code null}
     *
     * @return  this parameter builder, never {@code null}
     */
    private @NotNull ParameterBuilder addMapEntry(@NotNull MapKey key,
                                                  @NotNull Consumer<MessageBuilder> messageConfigurer)
    {
      final var nestedBuilder = new InternalMessageBuilder(messageFactory);

      messageConfigurer.accept(nestedBuilder);

      return addMapEntry(key, nestedBuilder.build());
    }


    /**
     * Starts the default map entry used when no keyed entry matches.
     *
     * @return  builder for supplying the default entry value, never {@code null}
     */
    @Override
    public @NotNull MapValueBuilder mapDefault() {
      return new MapValueBuilderImpl(this, null);
    }


    /**
     * Flushes this part and starts a text part in the enclosing message builder.
     *
     * @param text  literal text for the next part, not {@code null}
     *
     * @return  builder for the next text part, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();

      return InternalMessageBuilder.this.text(text);
    }


    /**
     * Flushes this part and starts a parameter part in the enclosing message builder.
     *
     * @param name  parameter name, not {@code null}
     *
     * @return  builder for the parameter part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use a supported naming convention
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.parameter(name);
    }


    /**
     * Flushes this part and starts a post-formatter part in the enclosing message builder.
     *
     * @param name  post-formatter name, not {@code null}
     *
     * @return  builder for the post-formatter part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.postFormatter(name);
    }


    /**
     * Flushes this part and starts a template part in the enclosing message builder.
     *
     * @param name  template name, not {@code null}
     *
     * @return  builder for the template part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.template(name);
    }


    /**
     * Flushes this part and builds the enclosing message.
     *
     * @return  built message, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();

      return InternalMessageBuilder.this.build();
    }


    /**
     * Flushes this part and builds a coded message from the enclosing builder.
     *
     * @param code  message code to associate with the built message, not {@code null}
     *
     * @return  coded message, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();

      return InternalMessageBuilder.this.buildWithCode(code);
    }


    /**
     * Flushes this part and builds the enclosing message as a template.
     *
     * @return  built template, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Template buildAsTemplate()
    {
      flush();

      return InternalMessageBuilder.this.buildAsTemplate();
    }


    /**
     * Flushes the parameter configuration as a {@link ParameterPart} into the enclosing builder's parts list.
     */
    private void flush()
    {
      if (!flushed)
      {
        flushed = true;
        finalized = true;
        activePartFlusher = null;

        parts.add(new ParameterPart(name, format, spaceBefore, spaceAfter,
            new MessagePartConfig(config), new MessagePartMap(map)));
      }
    }
  }




  /**
   * Default implementation of {@link PostFormatterBuilder}.
   * <p>
   * Collects the post-formatter name, inner message and configuration values and flushes them as a
   * {@link PostFormatterPart} when the next part is started or the message is built.
   *
   * @since 0.21.0
   */
  public final class PostFormatterBuilderImpl
      extends AbstractConfigurableBuilder<PostFormatterBuilder>
      implements PostFormatterBuilder
  {
    /** Name of the post-formatter represented by this builder. */
    private final @NotNull String name;

    /** Message that will be passed to the post-formatter. */
    private @NotNull Message.WithSpaces innerMessage;

    /** Indicates whether this post-formatter part has already been written to the enclosing builder. */
    private boolean flushed;


    /**
     * Construct a new post-formatter builder for the given formatter name.
     *
     * @param messageFactory  message factory used to parse nested message values, not {@code null}
     * @param name            post-formatter name (must follow kebab-case convention), not {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not match the kebab-case naming convention
     */
    private PostFormatterBuilderImpl(@NotNull MessageFactory messageFactory, @NotNull String name)
    {
      super(messageFactory);

      if (!isKebabCaseName(requireNonNull(name, "name must not be null")))
      {
        throw new IllegalArgumentException("post-formatter name '" + name +
            "' must match the kebab-case naming convention");
      }

      this.name = name;
      this.innerMessage = EmptyMessage.INSTANCE;
    }


    /**
     * Flushes the post-formatter configuration as a {@link PostFormatterPart} into the enclosing builder's parts list.
     */
    private void flush()
    {
      if (!flushed)
      {
        flushed = true;
        finalized = true;
        activePartFlusher = null;

        parts.add(new PostFormatterPart(name, innerMessage, spaceBefore, spaceAfter, new MessagePartConfig(config)));
      }
    }


    /**
     * Sets the post-formatter input from a message-format string.
     *
     * @param message  message-format string to parse, not {@code null}
     *
     * @return  this post-formatter builder, never {@code null}
     */
    @Override
    public @NotNull PostFormatterBuilder withMessage(@NotNull String message) {
      return withMessage(messageFactory.parseMessage(requireNonNull(message, "message must not be null")));
    }


    /**
     * Sets the post-formatter input from a pre-built message.
     *
     * @param message  message to pass to the post-formatter, not {@code null}
     *
     * @return  this post-formatter builder, never {@code null}
     */
    @Override
    public @NotNull PostFormatterBuilder withMessage(@NotNull Message.WithSpaces message)
    {
      innerMessage = requireNonNull(message, "message must not be null");

      return this;
    }


    /**
     * Sets the post-formatter input by configuring a nested message builder.
     *
     * @param messageConfigurer  callback that configures the nested message, not {@code null}
     *
     * @return  this post-formatter builder, never {@code null}
     */
    @Override
    public @NotNull PostFormatterBuilder withMessage(@NotNull Consumer<MessageBuilder> messageConfigurer)
    {
      requireNonNull(messageConfigurer, "messageConfigurer must not be null");

      final var nestedBuilder = new InternalMessageBuilder(messageFactory);

      messageConfigurer.accept(nestedBuilder);

      innerMessage = nestedBuilder.build();

      return this;
    }


    /**
     * Flushes this part and starts a text part in the enclosing message builder.
     *
     * @param text  literal text for the next part, not {@code null}
     *
     * @return  builder for the next text part, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();

      return InternalMessageBuilder.this.text(text);
    }


    /**
     * Flushes this part and starts a parameter part in the enclosing message builder.
     *
     * @param name  parameter name, not {@code null}
     *
     * @return  builder for the parameter part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use a supported naming convention
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.parameter(name);
    }


    /**
     * Flushes this part and starts a post-formatter part in the enclosing message builder.
     *
     * @param name  post-formatter name, not {@code null}
     *
     * @return  builder for the post-formatter part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.postFormatter(name);
    }


    /**
     * Flushes this part and starts a template part in the enclosing message builder.
     *
     * @param name  template name, not {@code null}
     *
     * @return  builder for the template part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.template(name);
    }


    /**
     * Flushes this part and builds the enclosing message.
     *
     * @return  built message, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();

      return InternalMessageBuilder.this.build();
    }


    /**
     * Flushes this part and builds a coded message from the enclosing builder.
     *
     * @param code  message code to associate with the built message, not {@code null}
     *
     * @return  coded message, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();

      return InternalMessageBuilder.this.buildWithCode(code);
    }


    /**
     * Flushes this part and builds the enclosing message as a template.
     *
     * @return  built template, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Template buildAsTemplate()
    {
      flush();

      return InternalMessageBuilder.this.buildAsTemplate();
    }
  }




  /**
   * Default implementation of {@link TemplateBuilder}.
   * <p>
   * Collects the template name, default parameter values and parameter delegate mappings and flushes them as a
   * {@link TemplatePart} when the next part is started or the message is built.
   *
   * @since 0.21.0
   */
  public final class TemplateBuilderImpl
      extends AbstractSpacedBuilder<TemplateBuilder>
      implements TemplateBuilder
  {
    /** Name of the template referenced by this builder. */
    private final @NotNull String name;

    /** Default values exposed to the referenced template. */
    private final @NotNull Map<String,TypedValue<?>> defaultParameters;

    /** Mapping from template parameter names to message parameter names. */
    private final @NotNull Map<String,String> parameterDelegates;

    /** Indicates whether this template part has already been written to the enclosing builder. */
    private boolean flushed;


    /**
     * Construct a new template builder for the given template name.
     *
     * @param name  template name (must follow kebab-case convention), not {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not match the kebab-case naming convention
     */
    private TemplateBuilderImpl(@NotNull String name)
    {
      if (!isKebabCaseName(requireNonNull(name, "name must not be null")))
        throw new IllegalArgumentException("template name '" + name + "' must match the kebab-case naming convention");

      this.name = name;
      this.defaultParameters = new LinkedHashMap<>();
      this.parameterDelegates = new LinkedHashMap<>();
    }


    /**
     * Flushes the template configuration as a {@link TemplatePart} into the enclosing builder's parts list.
     */
    private void flush()
    {
      if (!flushed)
      {
        flushed = true;
        activePartFlusher = null;

        parts.add(new TemplatePart(name, spaceBefore, spaceAfter, defaultParameters, parameterDelegates));
      }
    }


    /**
     * Adds a string default parameter for the referenced template.
     *
     * @param name   template parameter name, not {@code null}
     * @param value  default string value, not {@code null}
     *
     * @return  this template builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use a supported naming convention
     */
    @Override
    public @NotNull TemplateBuilder withDefaultParameter(@NotNull String name, @NotNull String value) {
      return withDefaultParameter(name, new TypedValueString(messageFactory, value));
    }


    /**
     * Adds a boolean default parameter for the referenced template.
     *
     * @param name   template parameter name, not {@code null}
     * @param value  default boolean value
     *
     * @return  this template builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use a supported naming convention
     */
    @Override
    public @NotNull TemplateBuilder withDefaultParameter(@NotNull String name, boolean value) {
      return withDefaultParameter(name, value ? TypedValueBool.TRUE : TypedValueBool.FALSE);
    }


    /**
     * Adds a numeric default parameter for the referenced template.
     *
     * @param name   template parameter name, not {@code null}
     * @param value  default numeric value
     *
     * @return  this template builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use a supported naming convention
     */
    @Override
    public @NotNull TemplateBuilder withDefaultParameter(@NotNull String name, long value) {
      return withDefaultParameter(name, new TypedValueNumber(value));
    }


    /**
     * Adds a message default parameter for the referenced template.
     *
     * @param name     template parameter name, not {@code null}
     * @param message  default message value, not {@code null}
     *
     * @return  this template builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use a supported naming convention
     */
    @Override
    public @NotNull TemplateBuilder withDefaultParameter(@NotNull String name, @NotNull Message.WithSpaces message) {
      return withDefaultParameter(name, new TypedValueMessage(message));
    }


    /**
     * Adds a message default parameter by configuring a nested message builder.
     *
     * @param name               template parameter name, not {@code null}
     * @param messageConfigurer  callback that configures the nested message, not {@code null}
     *
     * @return  this template builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use a supported naming convention
     */
    @Override
    public @NotNull TemplateBuilder withDefaultParameter(@NotNull String name,
                                                         @NotNull Consumer<MessageBuilder> messageConfigurer)
    {
      requireNonNull(messageConfigurer, "messageConfigurer must not be null");

      final var nestedBuilder = new InternalMessageBuilder(messageFactory);

      messageConfigurer.accept(nestedBuilder);

      return withDefaultParameter(name, nestedBuilder.build());
    }


    /**
     * Adds a typed default parameter value with the given name.
     *
     * @param name   parameter name (must follow kebab-case or lower camel-case convention), not {@code null}
     * @param value  typed value, not {@code null}
     *
     * @return  this template builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not match the expected naming convention
     */
    @Contract("_, _ -> this")
    private @NotNull TemplateBuilder withDefaultParameter(@NotNull String name, @NotNull TypedValue<?> value)
    {
      if (!isKebabOrLowerCamelCaseName(requireNonNull(name, "name must not be null")))
      {
        throw new IllegalArgumentException("default parameter name '" + name +
            "' must match the kebab-case or lower camel-case naming convention");
      }

      defaultParameters.put(name, value);

      return this;
    }


    /**
     * Delegates a template parameter to a parameter from the enclosing message.
     *
     * @param templateParam  template parameter name, not {@code null}
     * @param messageParam   enclosing message parameter name, not {@code null}
     *
     * @return  this template builder, never {@code null}
     *
     * @throws IllegalArgumentException if either parameter name does not use a supported naming convention
     */
    @Override
    public @NotNull TemplateBuilder withParameterDelegate(@NotNull String templateParam, @NotNull String messageParam)
    {
      if (!isKebabOrLowerCamelCaseName(requireNonNull(templateParam, "templateParam must not be null")))
      {
        throw new IllegalArgumentException("template parameter name '" + templateParam +
            "' must match the kebab-case or lower camel-case naming convention");
      }

      if (!isKebabOrLowerCamelCaseName(requireNonNull(messageParam, "messageParam must not be null")))
      {
        throw new IllegalArgumentException("message parameter name '" + messageParam +
            "' must match the kebab-case or lower camel-case naming convention");
      }

      parameterDelegates.put(templateParam, messageParam);

      return this;
    }


    /**
     * Flushes this part and starts a text part in the enclosing message builder.
     *
     * @param text  literal text for the next part, not {@code null}
     *
     * @return  builder for the next text part, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();

      return InternalMessageBuilder.this.text(text);
    }


    /**
     * Flushes this part and starts a parameter part in the enclosing message builder.
     *
     * @param name  parameter name, not {@code null}
     *
     * @return  builder for the parameter part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use a supported naming convention
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.parameter(name);
    }


    /**
     * Flushes this part and starts a post-formatter part in the enclosing message builder.
     *
     * @param name  post-formatter name, not {@code null}
     *
     * @return  builder for the post-formatter part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.postFormatter(name);
    }


    /**
     * Flushes this part and starts a template part in the enclosing message builder.
     *
     * @param name  template name, not {@code null}
     *
     * @return  builder for the template part, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not use kebab-case
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();

      return InternalMessageBuilder.this.template(name);
    }


    /**
     * Flushes this part and builds the enclosing message.
     *
     * @return  built message, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();

      return InternalMessageBuilder.this.build();
    }


    /**
     * Flushes this part and builds a coded message from the enclosing builder.
     *
     * @param code  message code to associate with the built message, not {@code null}
     *
     * @return  coded message, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();

      return InternalMessageBuilder.this.buildWithCode(code);
    }


    /**
     * Flushes this part and builds the enclosing message as a template.
     *
     * @return  built template, never {@code null}
     *
     * @throws IllegalStateException if the enclosing builder has already been built
     */
    @Override
    public @NotNull Template buildAsTemplate()
    {
      flush();

      return InternalMessageBuilder.this.buildAsTemplate();
    }
  }




  /**
   * Default implementation of {@link MapValueBuilder}.
   * <p>
   * Receives a map key and delegates to the enclosing {@link ParameterBuilderImpl} to add the map entry when a
   * message value is provided.
   *
   * @since 0.21.0
   */
  public static final class MapValueBuilderImpl implements MapValueBuilder
  {
    /** Parameter builder that receives the completed map entry. */
    private final @NotNull ParameterBuilderImpl parameterBuilder;

    /** Key associated with the map entry, or {@code null} for the default entry. */
    private final MapKey key;


    /**
     * Construct a new map value builder.
     *
     * @param parameterBuilder  the enclosing parameter builder, not {@code null}
     * @param key               the map key, or {@code null} for the default entry
     */
    MapValueBuilderImpl(@NotNull ParameterBuilderImpl parameterBuilder, MapKey key)
    {
      this.parameterBuilder = parameterBuilder;
      this.key = key;
    }


    /**
     * Completes the map entry with a value parsed from a message-format string.
     *
     * @param message  message-format string for the entry value, not {@code null}
     *
     * @return  parameter builder that owns the map, never {@code null}
     */
    @Override
    public @NotNull ParameterBuilder message(@NotNull String message) {
      return parameterBuilder.addMapEntry(key, message);
    }


    /**
     * Completes the map entry with a pre-built message value.
     *
     * @param message  message value for the entry, not {@code null}
     *
     * @return  parameter builder that owns the map, never {@code null}
     */
    @Override
    public @NotNull ParameterBuilder message(@NotNull Message.WithSpaces message) {
      return parameterBuilder.addMapEntry(key, message);
    }


    /**
     * Completes the map entry by configuring a nested message builder.
     *
     * @param messageConfigurer  callback that configures the nested message, not {@code null}
     *
     * @return  parameter builder that owns the map, never {@code null}
     */
    @Override
    public @NotNull ParameterBuilder message(@NotNull Consumer<MessageBuilder> messageConfigurer) {
      return parameterBuilder.addMapEntry(key, messageConfigurer);
    }
  }




  /**
   * Default implementation of {@link MapEqualityBuilder}.
   * <p>
   * Manages an equality comparison type ({@code eq}/{@code ne}) and uses a key factory to create the appropriate
   * {@link MapKey} when the map value is provided.
   *
   * @since 0.21.0
   */
  public static non-sealed class MapEqualityBuilderImpl implements MapEqualityBuilder
  {
    /** Parameter builder that receives the completed map entry. */
    private final @NotNull ParameterBuilderImpl parameterBuilder;

    /** Factory that turns the selected comparison type into a concrete map key. */
    private final @NotNull Function<CompareType,MapKey> keyFactory;

    /** Comparison type currently selected for the entry being built. */
    protected @NotNull CompareType compareType;


    /**
     * Construct a new equality map entry builder.
     *
     * @param parameterBuilder  the enclosing parameter builder, not {@code null}
     * @param keyFactory        factory that creates a {@link MapKey} for the given comparison type, not {@code null}
     */
    MapEqualityBuilderImpl(@NotNull ParameterBuilderImpl parameterBuilder,
                           @NotNull Function<CompareType,MapKey> keyFactory)
    {
      this.parameterBuilder = parameterBuilder;
      this.keyFactory = keyFactory;
      this.compareType = EQ;
    }


    /**
     * Selects equality comparison for the map entry.
     *
     * @return  builder for supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapValueBuilder eq()
    {
      compareType = EQ;

      return this;
    }


    /**
     * Selects inequality comparison for the map entry.
     *
     * @return  builder for supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapValueBuilder ne()
    {
      compareType = NE;

      return this;
    }


    /**
     * Completes the map entry with a pre-built message value.
     *
     * @param message  message value for the entry, not {@code null}
     *
     * @return  parameter builder that owns the map, never {@code null}
     */
    @Override
    public @NotNull ParameterBuilder message(@NotNull Message.WithSpaces message) {
      return parameterBuilder.addMapEntry(keyFactory.apply(compareType), message);
    }


    /**
     * Completes the map entry with a value parsed from a message-format string.
     *
     * @param message  message-format string for the entry value, not {@code null}
     *
     * @return  parameter builder that owns the map, never {@code null}
     */
    @Override
    public @NotNull ParameterBuilder message(@NotNull String message) {
      return parameterBuilder.addMapEntry(keyFactory.apply(compareType), message);
    }


    /**
     * Completes the map entry by configuring a nested message builder.
     *
     * @param messageConfigurer  callback that configures the nested message, not {@code null}
     *
     * @return  parameter builder that owns the map, never {@code null}
     */
    @Override
    public @NotNull ParameterBuilder message(@NotNull Consumer<MessageBuilder> messageConfigurer) {
      return parameterBuilder.addMapEntry(keyFactory.apply(compareType), messageConfigurer);
    }
  }




  /**
   * Default implementation of {@link MapRelationalBuilder}.
   * <p>
   * Extends {@link MapEqualityBuilderImpl} with additional relational comparison types ({@code lt}, {@code lte},
   * {@code gt}, {@code gte}).
   *
   * @since 0.21.0
   */
  public static final class MapRelationalBuilderImpl extends MapEqualityBuilderImpl implements MapRelationalBuilder
  {
    /**
     * Construct a new relational map entry builder.
     *
     * @param parameterBuilder  the enclosing parameter builder, not {@code null}
     * @param keyFactory        factory that creates a {@link MapKey} for the given comparison type, not {@code null}
     */
    MapRelationalBuilderImpl(@NotNull ParameterBuilderImpl parameterBuilder,
                             @NotNull Function<CompareType,MapKey> keyFactory) {
      super(parameterBuilder, keyFactory);
    }


    /**
     * Selects a less-than comparison for the map entry.
     *
     * @return  builder for supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapValueBuilder lt()
    {
      compareType = LT;

      return this;
    }


    /**
     * Selects a less-than-or-equal comparison for the map entry.
     *
     * @return  builder for supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapValueBuilder lte()
    {
      compareType = LTE;

      return this;
    }


    /**
     * Selects a greater-than comparison for the map entry.
     *
     * @return  builder for supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapValueBuilder gt()
    {
      compareType = GT;

      return this;
    }


    /**
     * Selects a greater-than-or-equal comparison for the map entry.
     *
     * @return  builder for supplying the entry value, never {@code null}
     */
    @Override
    public @NotNull MapValueBuilder gte()
    {
      compareType = GTE;

      return this;
    }
  }
}
