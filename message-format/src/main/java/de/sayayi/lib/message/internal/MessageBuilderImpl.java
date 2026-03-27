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

import static de.sayayi.lib.message.util.MessageUtil.isKebabCaseName;
import static de.sayayi.lib.message.util.MessageUtil.isKebabOrLowerCamelCaseName;
import static java.util.Objects.requireNonNull;


/**
 * Default implementation of the {@link MessageBuilder} interface.
 * <p>
 * This class is <strong>not thread-safe</strong>. A builder instance must only be used from a
 * single thread and must not be reused after calling {@link #build()} or
 * {@link #buildWithCode(String)}.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public final class MessageBuilderImpl implements MessageBuilder
{
  private final @NotNull MessageFactory messageFactory;
  private final @NotNull List<MessagePart> parts;
  private @NotNull TextJoiner pendingText;


  /**
   * Construct a new builder using the given {@code messageFactory}.
   *
   * @param messageFactory  message factory, not {@code null}
   */
  public MessageBuilderImpl(@NotNull MessageFactory messageFactory)
  {
    this.messageFactory = requireNonNull(messageFactory, "messageFactory must not be null");
    this.parts = new ArrayList<>();
    this.pendingText = new TextJoiner();
  }


  private void flushText()
  {
    var text = pendingText.asSpacedText();
    if (!text.isEmpty() || text.isSpaceBefore() || text.isSpaceAfter())
    {
      parts.add(text);
      pendingText = new TextJoiner();
    }
  }


  @Override
  public @NotNull TextBuilder text(@NotNull String text) {
    return new TextBuilderImpl(requireNonNull(text, "text must not be null"));
  }


  @Override
  public @NotNull ParameterBuilder parameter(@NotNull String name)
  {
    flushText();
    return new ParameterBuilderImpl(name);
  }


  @Override
  public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
  {
    flushText();
    return new PostFormatterBuilderImpl(name);
  }


  @Override
  public @NotNull TemplateBuilder template(@NotNull String name)
  {
    flushText();
    return new TemplateBuilderImpl(name);
  }


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


  @Override
  public @NotNull Message.WithCode buildWithCode(@NotNull String code) {
    return messageFactory.withCode(code, build());
  }




  /**
   * Default implementation of {@link TextBuilder}.
   *
   * @since 0.21.0
   */
  public final class TextBuilderImpl implements TextBuilder
  {
    private final @NotNull String text;
    private boolean spaceBefore;
    private boolean spaceAfter;


    private TextBuilderImpl(@NotNull String text) {
      this.text = text;
    }


    private void flush()
    {
      if (spaceBefore)
        pendingText.add(' ');

      pendingText.addWithSpace(text);

      if (spaceAfter)
        pendingText.add(' ');
    }


    @Override
    @Contract("-> this")
    public @NotNull TextBuilder spaceBefore()
    {
      spaceBefore = true;
      return this;
    }


    @Override
    @Contract("-> this")
    public @NotNull TextBuilder spaceAfter()
    {
      spaceAfter = true;
      return this;
    }


    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();
      return new TextBuilderImpl(requireNonNull(text, "text must not be null"));
    }


    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();
      flushText();
      return new ParameterBuilderImpl(name);
    }


    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();
      flushText();
      return new PostFormatterBuilderImpl(name);
    }


    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();
      flushText();
      return new TemplateBuilderImpl(name);
    }


    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();
      return MessageBuilderImpl.this.build();
    }


    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();
      return MessageBuilderImpl.this.buildWithCode(code);
    }
  }




  /**
   * Default implementation of {@link ParameterBuilder}.
   *
   * @since 0.21.0
   */
  public final class ParameterBuilderImpl implements ParameterBuilder
  {
    private final @NotNull String name;
    private String format;
    private boolean spaceBefore;
    private boolean spaceAfter;
    private final @NotNull Map<String,TypedValue<?>> config;
    private final @NotNull Map<MapKey,TypedValue<?>> map;


    private ParameterBuilderImpl(@NotNull String name)
    {
      if (!isKebabOrLowerCamelCaseName(requireNonNull(name, "name must not be null")))
        throw new IllegalArgumentException("parameter name '" + name + "' must match the kebab-case or lower camel-case naming convention");

      this.name = name;
      this.config = new LinkedHashMap<>();
      this.map = new LinkedHashMap<>();
    }


    private void flush()
    {
      parts.add(new ParameterPart(name, format, spaceBefore, spaceAfter,
          new MessagePartConfig(config), new MessagePartMap(map)));
    }


    @Override
    @Contract("_ -> this")
    public @NotNull ParameterBuilder withFormat(@NotNull String format)
    {
      if (!isKebabCaseName(requireNonNull(format, "format must not be null")))
        throw new IllegalArgumentException("format name '" + format + "' must match the kebab-case naming convention");

      this.format = format;
      return this;
    }


    @Contract("_, _ -> this")
    private @NotNull ParameterBuilder withConfig(@NotNull String name, @NotNull TypedValue<?> value)
    {
      if (!isKebabCaseName(requireNonNull(name, "name must not be null")))
        throw new IllegalArgumentException("config name '" + name + "' must match the kebab-case naming convention");

      config.put(name, requireNonNull(value, "value must not be null"));
      return this;
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull ParameterBuilder configString(@NotNull String name, @NotNull String value) {
      return withConfig(name, new TypedValueString(value));
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull ParameterBuilder configBool(@NotNull String name, boolean value) {
      return withConfig(name, value ? TypedValueBool.TRUE : TypedValueBool.FALSE);
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull ParameterBuilder configNumber(@NotNull String name, long value) {
      return withConfig(name, new TypedValueNumber(value));
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull ParameterBuilder configMessage(@NotNull String name, @NotNull Message.WithSpaces message) {
      return withConfig(name, new TypedValueMessage(message));
    }


    @Override
    public @NotNull MapValueBuilder mapBool(boolean key) {
      return new MapValueBuilderImpl(this, key ? MapKeyBool.TRUE : MapKeyBool.FALSE);
    }


    @Override
    public @NotNull MapEqualityBuilder mapEmpty()
    {
      return new MapEqualityBuilderImpl(this,
          compareType -> compareType == CompareType.EQ ? MapKeyEmpty.EQ : MapKeyEmpty.NE);
    }


    @Override
    public @NotNull MapEqualityBuilder mapNull()
    {
      return new MapEqualityBuilderImpl(this,
          compareType -> compareType == CompareType.EQ ? MapKeyNull.EQ : MapKeyNull.NE);
    }


    @Override
    public @NotNull MapRelationalBuilder mapNumber(long number)
    {
      return new MapRelationalBuilderImpl(this,
          compareType -> new MapKeyNumber(compareType, number));
    }


    @Override
    public @NotNull MapRelationalBuilder mapString(@NotNull String string)
    {
      requireNonNull(string, "string must not be null");

      return new MapRelationalBuilderImpl(this,
          compareType -> new MapKeyString(compareType, string));
    }


    private @NotNull ParameterBuilder addMapEntry(@NotNull MapKey key, @NotNull Message.WithSpaces message)
    {
      map.put(key, new TypedValueMessage(requireNonNull(message, "message must not be null")));
      return this;
    }


    private @NotNull ParameterBuilder addMapEntry(@NotNull MapKey key, @NotNull String message)
    {
      map.put(key, new TypedValueMessage(
          messageFactory.parseMessage(requireNonNull(message, "message must not be null"))));
      return this;
    }


    @Override
    public @NotNull MapValueBuilder mapDefault() {
      return new MapValueBuilderImpl(this, null);
    }


    @Override
    @Contract("-> this")
    public @NotNull ParameterBuilder spaceBefore()
    {
      spaceBefore = true;
      return this;
    }


    @Override
    @Contract("-> this")
    public @NotNull ParameterBuilder spaceAfter()
    {
      spaceAfter = true;
      return this;
    }


    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();
      return MessageBuilderImpl.this.text(text);
    }


    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();
      return MessageBuilderImpl.this.parameter(name);
    }


    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();
      return MessageBuilderImpl.this.postFormatter(name);
    }


    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();
      return MessageBuilderImpl.this.template(name);
    }


    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();
      return MessageBuilderImpl.this.build();
    }


    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();
      return MessageBuilderImpl.this.buildWithCode(code);
    }
  }




  /**
   * Default implementation of {@link PostFormatterBuilder}.
   *
   * @since 0.21.0
   */
  public final class PostFormatterBuilderImpl implements PostFormatterBuilder
  {
    private final @NotNull String name;
    private @NotNull Message.WithSpaces innerMessage;
    private boolean spaceBefore;
    private boolean spaceAfter;
    private final @NotNull Map<String,TypedValue<?>> config;


    private PostFormatterBuilderImpl(@NotNull String name)
    {
      if (!isKebabCaseName(requireNonNull(name, "name must not be null")))
        throw new IllegalArgumentException("post-formatter name '" + name + "' must match the kebab-case naming convention");

      this.name = name;
      this.innerMessage = EmptyMessage.INSTANCE;
      this.config = new LinkedHashMap<>();
    }


    private void flush()
    {
      parts.add(new PostFormatterPart(name, innerMessage, spaceBefore, spaceAfter,
          new MessagePartConfig(config)));
    }


    @Override
    @Contract("_ -> this")
    public @NotNull PostFormatterBuilder withMessage(@NotNull Consumer<MessageBuilder> messageConfigurer)
    {
      requireNonNull(messageConfigurer, "messageConfigurer must not be null");

      final var nestedBuilder = new MessageBuilderImpl(messageFactory);
      messageConfigurer.accept(nestedBuilder);
      innerMessage = nestedBuilder.build();

      return this;
    }


    @Contract("_, _ -> this")
    private @NotNull PostFormatterBuilder withConfig(@NotNull String name, @NotNull TypedValue<?> value)
    {
      requireNonNull(name, "name must not be null");
      if (!isKebabCaseName(name))
        throw new IllegalArgumentException("config name '" + name + "' must match the kebab-case naming convention");

      config.put(name, requireNonNull(value, "value must not be null"));
      return this;
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull PostFormatterBuilder configString(@NotNull String name, @NotNull String value) {
      return withConfig(name, new TypedValueString(value));
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull PostFormatterBuilder configBool(@NotNull String name, boolean value) {
      return withConfig(name, value ? TypedValueBool.TRUE : TypedValueBool.FALSE);
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull PostFormatterBuilder configNumber(@NotNull String name, long value) {
      return withConfig(name, new TypedValueNumber(value));
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull PostFormatterBuilder configMessage(@NotNull String name, @NotNull Message.WithSpaces message) {
      return withConfig(name, new TypedValueMessage(message));
    }


    @Override
    @Contract("-> this")
    public @NotNull PostFormatterBuilder spaceBefore()
    {
      spaceBefore = true;
      return this;
    }


    @Override
    @Contract("-> this")
    public @NotNull PostFormatterBuilder spaceAfter()
    {
      spaceAfter = true;
      return this;
    }


    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();
      return MessageBuilderImpl.this.text(text);
    }


    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();
      return MessageBuilderImpl.this.parameter(name);
    }


    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();
      return MessageBuilderImpl.this.postFormatter(name);
    }


    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();
      return MessageBuilderImpl.this.template(name);
    }


    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();
      return MessageBuilderImpl.this.build();
    }


    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();
      return MessageBuilderImpl.this.buildWithCode(code);
    }
  }




  /**
   * Default implementation of {@link TemplateBuilder}.
   *
   * @since 0.21.0
   */
  public final class TemplateBuilderImpl implements TemplateBuilder
  {
    private final @NotNull String name;
    private boolean spaceBefore;
    private boolean spaceAfter;
    private final @NotNull Map<String,TypedValue<?>> defaultParameters;
    private final @NotNull Map<String,String> parameterDelegates;


    private TemplateBuilderImpl(@NotNull String name)
    {
      if (!isKebabCaseName(requireNonNull(name, "name must not be null")))
        throw new IllegalArgumentException("template name '" + name + "' must match the kebab-case naming convention");

      this.name = name;
      this.defaultParameters = new LinkedHashMap<>();
      this.parameterDelegates = new LinkedHashMap<>();
    }


    private void flush() {
      parts.add(new TemplatePart(name, spaceBefore, spaceAfter, defaultParameters, parameterDelegates));
    }


    @Contract("_, _ -> this")
    private @NotNull TemplateBuilder withDefaultParameter(@NotNull String name, @NotNull TypedValue<?> value)
    {
      if (!isKebabOrLowerCamelCaseName(requireNonNull(name, "name must not be null")))
        throw new IllegalArgumentException("default parameter name '" + name + "' must match the kebab-case or lower camel-case naming convention");

      defaultParameters.put(name, requireNonNull(value, "value must not be null"));
      return this;
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull TemplateBuilder withDefaultParameterString(@NotNull String name, @NotNull String value) {
      return withDefaultParameter(name, new TypedValueString(value));
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull TemplateBuilder withDefaultParameterBool(@NotNull String name, boolean value) {
      return withDefaultParameter(name, value ? TypedValueBool.TRUE : TypedValueBool.FALSE);
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull TemplateBuilder withDefaultParameterNumber(@NotNull String name, long value) {
      return withDefaultParameter(name, new TypedValueNumber(value));
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull TemplateBuilder withDefaultParameterMessage(@NotNull String name, @NotNull Message.WithSpaces message) {
      return withDefaultParameter(name, new TypedValueMessage(message));
    }


    @Override
    @Contract("_, _ -> this")
    public @NotNull TemplateBuilder withParameterDelegate(@NotNull String templateParam, @NotNull String messageParam)
    {
      parameterDelegates.put(
          requireNonNull(templateParam, "templateParam must not be null"),
          requireNonNull(messageParam, "messageParam must not be null"));
      return this;
    }


    @Override
    @Contract("-> this")
    public @NotNull TemplateBuilder spaceBefore()
    {
      spaceBefore = true;
      return this;
    }


    @Override
    @Contract("-> this")
    public @NotNull TemplateBuilder spaceAfter()
    {
      spaceAfter = true;
      return this;
    }


    @Override
    public @NotNull TextBuilder text(@NotNull String text)
    {
      flush();
      return MessageBuilderImpl.this.text(text);
    }


    @Override
    public @NotNull ParameterBuilder parameter(@NotNull String name)
    {
      flush();
      return MessageBuilderImpl.this.parameter(name);
    }


    @Override
    public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
    {
      flush();
      return MessageBuilderImpl.this.postFormatter(name);
    }


    @Override
    public @NotNull TemplateBuilder template(@NotNull String name)
    {
      flush();
      return MessageBuilderImpl.this.template(name);
    }


    @Override
    public @NotNull Message.WithSpaces build()
    {
      flush();
      return MessageBuilderImpl.this.build();
    }


    @Override
    public @NotNull Message.WithCode buildWithCode(@NotNull String code)
    {
      flush();
      return MessageBuilderImpl.this.buildWithCode(code);
    }
  }




  /**
   * Default implementation of {@link MapValueBuilder}.
   *
   * @since 0.21.0
   */
  public static final class MapValueBuilderImpl implements MapValueBuilder
  {
    private final @NotNull ParameterBuilderImpl parameterBuilder;
    private final MapKey key;


    MapValueBuilderImpl(@NotNull ParameterBuilderImpl parameterBuilder, MapKey key)
    {
      this.parameterBuilder = parameterBuilder;
      this.key = key;
    }


    @Override
    public @NotNull ParameterBuilder message(@NotNull Message.WithSpaces message) {
      return parameterBuilder.addMapEntry(key, message);
    }


    @Override
    public @NotNull ParameterBuilder message(@NotNull String message) {
      return parameterBuilder.addMapEntry(key, message);
    }
  }




  /**
   * Default implementation of {@link MapEqualityBuilder}.
   *
   * @since 0.21.0
   */
  public static non-sealed class MapEqualityBuilderImpl implements MapEqualityBuilder
  {
    private final @NotNull ParameterBuilderImpl parameterBuilder;
    private final @NotNull Function<CompareType,MapKey> keyFactory;
    protected @NotNull CompareType compareType;


    MapEqualityBuilderImpl(@NotNull ParameterBuilderImpl parameterBuilder,
                           @NotNull Function<CompareType,MapKey> keyFactory)
    {
      this.parameterBuilder = parameterBuilder;
      this.keyFactory = keyFactory;
      this.compareType = CompareType.EQ;
    }


    @Override
    @Contract("-> this")
    public @NotNull MapEqualityBuilder eq()
    {
      compareType = CompareType.EQ;
      return this;
    }


    @Override
    @Contract("-> this")
    public @NotNull MapEqualityBuilder ne()
    {
      compareType = CompareType.NE;
      return this;
    }


    @Override
    public @NotNull ParameterBuilder message(@NotNull Message.WithSpaces message) {
      return parameterBuilder.addMapEntry(keyFactory.apply(compareType), message);
    }


    @Override
    public @NotNull ParameterBuilder message(@NotNull String message) {
      return parameterBuilder.addMapEntry(keyFactory.apply(compareType), message);
    }
  }




  /**
   * Default implementation of {@link MapRelationalBuilder}.
   *
   * @since 0.21.0
   */
  public static final class MapRelationalBuilderImpl extends MapEqualityBuilderImpl implements MapRelationalBuilder
  {
    MapRelationalBuilderImpl(@NotNull ParameterBuilderImpl parameterBuilder,
                             @NotNull Function<CompareType,MapKey> keyFactory) {
      super(parameterBuilder, keyFactory);
    }


    @Override
    @Contract("-> this")
    public @NotNull MapRelationalBuilder lt()
    {
      compareType = CompareType.LT;
      return this;
    }


    @Override
    @Contract("-> this")
    public @NotNull MapRelationalBuilder lte()
    {
      compareType = CompareType.LTE;
      return this;
    }


    @Override
    @Contract("-> this")
    public @NotNull MapRelationalBuilder gt()
    {
      compareType = CompareType.GT;
      return this;
    }


    @Override
    @Contract("-> this")
    public @NotNull MapRelationalBuilder gte()
    {
      compareType = CompareType.GTE;
      return this;
    }
  }
}
