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
import de.sayayi.lib.message.part.TextJoiner;
import de.sayayi.lib.message.part.TypedValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static de.sayayi.lib.message.part.MapKey.CompareType.EQ;
import static de.sayayi.lib.message.util.MessageUtil.isKebabCaseName;
import static de.sayayi.lib.message.util.MessageUtil.isKebabOrLowerCamelCaseName;
import static java.util.Objects.requireNonNull;


/**
 * Default implementation of the {@link MessageBuilder} interface.
 * <p>
 * This class is <strong>not thread-safe</strong>. A builder instance must only be used from a single thread and must
 * not be reused after calling {@link #build()} or {@link #buildWithCode(String)}.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public final class InternalMessageBuilder implements MessageBuilder
{
  private final @NotNull MessageFactory messageFactory;
  private final @NotNull List<MessagePart> parts;
  private @NotNull TextJoiner pendingText;


  /**
   * Construct a new builder using the given {@code messageFactory}.
   *
   * @param messageFactory  message factory, not {@code null}
   */
  public InternalMessageBuilder(@NotNull MessageFactory messageFactory)
  {
    this.messageFactory = requireNonNull(messageFactory, "messageFactory must not be null");
    this.parts = new ArrayList<>();
    this.pendingText = new TextJoiner();
  }


  /**
   * Flushes any accumulated pending text as a text part into the parts list, resetting the text joiner.
   */
  private void flushText()
  {
    var text = pendingText.asSpacedText();
    if (!text.isEmpty() || text.isSpaceBefore() || text.isSpaceAfter())
    {
      parts.add(text);
      pendingText = new TextJoiner();
    }
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull TextBuilder text(@NotNull String text) {
    return new TextBuilderImpl(requireNonNull(text, "text must not be null"));
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull ParameterBuilder parameter(@NotNull String name)
  {
    flushText();
    return new ParameterBuilderImpl(name);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
  {
    flushText();
    return new PostFormatterBuilderImpl(name);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull TemplateBuilder template(@NotNull String name)
  {
    flushText();
    return new TemplateBuilderImpl(name);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Message.WithSpaces build()
  {
    flushText();

    if (parts.isEmpty())
      return EmptyMessage.INSTANCE;

    if (parts.size() == 1 && parts.getFirst() instanceof MessagePart.Text textPart)
      return new TextMessage(textPart);

    return new CompoundMessage(parts);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull Message.WithCode buildWithCode(@NotNull String code) {
    return messageFactory.withCode(code, build());
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
    protected boolean spaceBefore;
    protected boolean spaceAfter;


    /** {@inheritDoc} */
    @Override
    @Contract("-> this")
    @SuppressWarnings("unchecked")
    public @NotNull S spaceBefore()
    {
      spaceBefore = true;
      return (S)this;
    }


    /** {@inheritDoc} */
    @Override
    @Contract("-> this")
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
   * Accumulates literal text and flushes it into the enclosing builder's pending text joiner when another part is
   * started or when the message is built.
   *
   * @since 0.21.0
   */
  public final class TextBuilderImpl
      extends AbstractSpacedBuilder<TextBuilder>
      implements TextBuilder
  {
    private final @NotNull String text;


    /**
     * Construct a new text builder for the given literal text.
     *
     * @param text  literal text, not {@code null}
     */
    private TextBuilderImpl(@NotNull String text) {
      this.text = text;
    }


    /**
     * Flushes the accumulated text (including any space-before/space-after settings) into the enclosing builder's
     * pending text joiner.
     */
    private void flush()
    {
      if (spaceBefore)
        pendingText.add(' ');

      pendingText.addWithSpace(text);

      if (spaceAfter)
        pendingText.add(' ');
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();
      return new TextBuilderImpl(requireNonNull(text, "text must not be null"));
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();
      flushText();
      return new ParameterBuilderImpl(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();
      flushText();
      return new PostFormatterBuilderImpl(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();
      flushText();
      return new TemplateBuilderImpl(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();
      return InternalMessageBuilder.this.build();
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();
      return InternalMessageBuilder.this.buildWithCode(code);
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
    protected final @NotNull Map<String,TypedValue<?>> config;


    /**
     * Construct a new configurable builder with an empty configuration map.
     */
    protected AbstractConfigurableBuilder() {
      this.config = new LinkedHashMap<>();
    }


    /**
     * Adds a typed configuration value with the given name.
     *
     * @param name   configuration name (must follow kebab-case convention), not {@code null}
     * @param value  typed value, not {@code null}
     *
     * @return  this builder, never {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not match the kebab-case naming convention
     */
    @Contract("_, _ -> this")
    @SuppressWarnings("unchecked")
    private @NotNull S withConfig(@NotNull String name, @NotNull TypedValue<?> value)
    {
      if (!isKebabCaseName(requireNonNull(name, "name must not be null")))
        throw new IllegalArgumentException("config name '" + name + "' must match the kebab-case naming convention");

      config.put(name, requireNonNull(value, "value must not be null"));

      return (S)this;
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_, _ -> this")
    public @NotNull S configString(@NotNull String name, @NotNull String value) {
      return withConfig(name, new TypedValueString(value));
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_, _ -> this")
    public @NotNull S configBool(@NotNull String name, boolean value) {
      return withConfig(name, value ? TypedValueBool.TRUE : TypedValueBool.FALSE);
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_, _ -> this")
    public @NotNull S configNumber(@NotNull String name, long value) {
      return withConfig(name, new TypedValueNumber(value));
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_, _ -> this")
    public @NotNull S configMessage(@NotNull String name, @NotNull Message.WithSpaces message) {
      return withConfig(name, new TypedValueMessage(message));
    }
  }




  /**
   * Default implementation of {@link ParameterBuilder}.
   * <p>
   * Collects the parameter name, optional format, configuration values and map entries, and flushes them as a
   * {@link ParameterPart} when the next part is started or the message is built.
   *
   * @since 0.21.0
   */
  public final class ParameterBuilderImpl
      extends AbstractConfigurableBuilder<ParameterBuilder>
      implements ParameterBuilder
  {
    private final @NotNull String name;
    private String format;
    private final @NotNull Map<MapKey,TypedValue<?>> map;


    /**
     * Construct a new parameter builder for the given parameter name.
     *
     * @param name  parameter name (must follow kebab-case or lower camel-case convention), not {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not match the expected naming convention
     */
    private ParameterBuilderImpl(@NotNull String name)
    {
      if (!isKebabOrLowerCamelCaseName(requireNonNull(name, "name must not be null")))
      {
        throw new IllegalArgumentException("parameter name '" + name +
            "' must match the kebab-case or lower camel-case naming convention");
      }

      this.name = name;
      this.map = new LinkedHashMap<>();
    }


    /**
     * Flushes the parameter configuration as a {@link ParameterPart} into the enclosing builder's parts list.
     */
    private void flush()
    {
      parts.add(new ParameterPart(name, format, spaceBefore, spaceAfter,
          new MessagePartConfig(config), new MessagePartMap(map)));
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_ -> this")
    public @NotNull ParameterBuilder withFormat(@NotNull String format)
    {
      if (!isKebabCaseName(requireNonNull(format, "format must not be null")))
        throw new IllegalArgumentException("format name '" + format + "' must match the kebab-case naming convention");

      this.format = format;
      return this;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull MapValueBuilder mapBool(boolean key) {
      return new MapValueBuilderImpl(this, key ? MapKeyBool.TRUE : MapKeyBool.FALSE);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull MapEqualityBuilder mapEmpty()
    {
      return new MapEqualityBuilderImpl(this,
          compareType -> compareType == EQ ? MapKeyEmpty.EQ : MapKeyEmpty.NE);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull MapEqualityBuilder mapNull()
    {
      return new MapEqualityBuilderImpl(this,
          compareType -> compareType == EQ ? MapKeyNull.EQ : MapKeyNull.NE);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull MapRelationalBuilder mapNumber(long number)
    {
      return new MapRelationalBuilderImpl(this,
          compareType -> new MapKeyNumber(compareType, number));
    }


    /** {@inheritDoc} */
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
    private @NotNull ParameterBuilder addMapEntry(@NotNull MapKey key, @NotNull String message)
    {
      map.put(key, new TypedValueMessage(
          messageFactory.parseMessage(requireNonNull(message, "message must not be null"))));
      return this;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull MapValueBuilder mapDefault() {
      return new MapValueBuilderImpl(this, null);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();
      return InternalMessageBuilder.this.text(text);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();
      return InternalMessageBuilder.this.parameter(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();
      return InternalMessageBuilder.this.postFormatter(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();
      return InternalMessageBuilder.this.template(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();
      return InternalMessageBuilder.this.build();
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();
      return InternalMessageBuilder.this.buildWithCode(code);
    }
  }




  /**
   * Default implementation of {@link PostFormatterBuilder}.
   * <p>
   * Collects the post-formatter name, inner message and configuration values, and flushes them as a
   * {@link PostFormatterPart} when the next part is started or the message is built.
   *
   * @since 0.21.0
   */
  public final class PostFormatterBuilderImpl
      extends AbstractConfigurableBuilder<PostFormatterBuilder>
      implements PostFormatterBuilder
  {
    private final @NotNull String name;
    private @NotNull Message.WithSpaces innerMessage;


    /**
     * Construct a new post-formatter builder for the given formatter name.
     *
     * @param name  post-formatter name (must follow kebab-case convention), not {@code null}
     *
     * @throws IllegalArgumentException if {@code name} does not match the kebab-case naming convention
     */
    private PostFormatterBuilderImpl(@NotNull String name)
    {
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
    private void flush() {
      parts.add(new PostFormatterPart(name, innerMessage, spaceBefore, spaceAfter, new MessagePartConfig(config)));
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_ -> this")
    public @NotNull PostFormatterBuilder withMessage(@NotNull Consumer<MessageBuilder> messageConfigurer)
    {
      requireNonNull(messageConfigurer, "messageConfigurer must not be null");

      final var nestedBuilder = new InternalMessageBuilder(messageFactory);
      messageConfigurer.accept(nestedBuilder);
      innerMessage = nestedBuilder.build();

      return this;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();
      return InternalMessageBuilder.this.text(text);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();
      return InternalMessageBuilder.this.parameter(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();
      return InternalMessageBuilder.this.postFormatter(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();
      return InternalMessageBuilder.this.template(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();
      return InternalMessageBuilder.this.build();
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();
      return InternalMessageBuilder.this.buildWithCode(code);
    }
  }




  /**
   * Default implementation of {@link TemplateBuilder}.
   * <p>
   * Collects the template name, default parameter values and parameter delegate mappings, and flushes them as a
   * {@link TemplatePart} when the next part is started or the message is built.
   *
   * @since 0.21.0
   */
  public final class TemplateBuilderImpl
      extends AbstractSpacedBuilder<TemplateBuilder>
      implements TemplateBuilder
  {
    private final @NotNull String name;
    private final @NotNull Map<String,TypedValue<?>> defaultParameters;
    private final @NotNull Map<String,String> parameterDelegates;


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
    private void flush() {
      parts.add(new TemplatePart(name, spaceBefore, spaceAfter, defaultParameters, parameterDelegates));
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

      defaultParameters.put(name, requireNonNull(value, "value must not be null"));
      return this;
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_, _ -> this")
    public @NotNull TemplateBuilder withDefaultParameterString(@NotNull String name, @NotNull String value) {
      return withDefaultParameter(name, new TypedValueString(value));
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_, _ -> this")
    public @NotNull TemplateBuilder withDefaultParameterBool(@NotNull String name, boolean value) {
      return withDefaultParameter(name, value ? TypedValueBool.TRUE : TypedValueBool.FALSE);
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_, _ -> this")
    public @NotNull TemplateBuilder withDefaultParameterNumber(@NotNull String name, long value) {
      return withDefaultParameter(name, new TypedValueNumber(value));
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_, _ -> this")
    public @NotNull TemplateBuilder withDefaultParameterMessage(@NotNull String name, @NotNull Message.WithSpaces message) {
      return withDefaultParameter(name, new TypedValueMessage(message));
    }


    /** {@inheritDoc} */
    @Override
    @Contract("_, _ -> this")
    public @NotNull TemplateBuilder withParameterDelegate(@NotNull String templateParam, @NotNull String messageParam)
    {
      parameterDelegates.put(
          requireNonNull(templateParam, "templateParam must not be null"),
          requireNonNull(messageParam, "messageParam must not be null"));
      return this;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();
      return InternalMessageBuilder.this.text(text);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();
      return InternalMessageBuilder.this.parameter(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();
      return InternalMessageBuilder.this.postFormatter(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();
      return InternalMessageBuilder.this.template(name);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();
      return InternalMessageBuilder.this.build();
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();
      return InternalMessageBuilder.this.buildWithCode(code);
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
    private final @NotNull ParameterBuilderImpl parameterBuilder;
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


    /** {@inheritDoc} */
    @Override
    public @NotNull ParameterBuilder message(@NotNull Message.WithSpaces message) {
      return parameterBuilder.addMapEntry(key, message);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull ParameterBuilder message(@NotNull String message) {
      return parameterBuilder.addMapEntry(key, message);
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
    private final @NotNull ParameterBuilderImpl parameterBuilder;
    private final @NotNull Function<CompareType,MapKey> keyFactory;
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


    /** {@inheritDoc} */
    @Override
    @Contract("-> this")
    public @NotNull MapEqualityBuilder eq()
    {
      compareType = EQ;
      return this;
    }


    /** {@inheritDoc} */
    @Override
    @Contract("-> this")
    public @NotNull MapEqualityBuilder ne()
    {
      compareType = CompareType.NE;
      return this;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull ParameterBuilder message(@NotNull Message.WithSpaces message) {
      return parameterBuilder.addMapEntry(keyFactory.apply(compareType), message);
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull ParameterBuilder message(@NotNull String message) {
      return parameterBuilder.addMapEntry(keyFactory.apply(compareType), message);
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


    /** {@inheritDoc} */
    @Override
    @Contract("-> this")
    public @NotNull MapRelationalBuilder lt()
    {
      compareType = CompareType.LT;
      return this;
    }


    /** {@inheritDoc} */
    @Override
    @Contract("-> this")
    public @NotNull MapRelationalBuilder lte()
    {
      compareType = CompareType.LTE;
      return this;
    }


    /** {@inheritDoc} */
    @Override
    @Contract("-> this")
    public @NotNull MapRelationalBuilder gt()
    {
      compareType = CompareType.GT;
      return this;
    }


    /** {@inheritDoc} */
    @Override
    @Contract("-> this")
    public @NotNull MapRelationalBuilder gte()
    {
      compareType = CompareType.GTE;
      return this;
    }
  }
}
