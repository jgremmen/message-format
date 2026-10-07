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
package de.sayayi.lib.message.internal.message.builder;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageBuilder;
import de.sayayi.lib.message.MessageBuilder.ParameterBuilder;
import de.sayayi.lib.message.internal.part.config.MessagePartConfig;
import de.sayayi.lib.message.internal.part.map.MessagePartMap;
import de.sayayi.lib.message.internal.part.map.key.*;
import de.sayayi.lib.message.internal.part.parameter.ParameterPart;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.TypedValue;
import de.sayayi.lib.message.template.Template;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static de.sayayi.lib.message.part.MapKey.CompareType.EQ;
import static de.sayayi.lib.message.util.MessageUtil.isKebabCaseName;
import static de.sayayi.lib.message.util.MessageUtil.isKebabOrLowerCamelCaseName;
import static java.util.Objects.requireNonNull;


/**
 * Default implementation of {@link ParameterBuilder}.
 * <p>
 * Configures a parameter reference, including its optional format, configuration values and mapped message values.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@ApiStatus.Internal
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
   * @param builder  enclosing message builder, not {@code null}
   * @param name     parameter name (must follow kebab-case or lower camel-case convention), not {@code null}
   *
   * @throws IllegalArgumentException if {@code name} does not match the expected naming convention
   */
  ParameterBuilderImpl(@NotNull FluentMessageBuilder builder, @NotNull String name)
  {
    super(builder);

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
  @NotNull ParameterBuilder addMapEntry(@NotNull MapKey key, @NotNull Message.WithSpaces message)
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
  @NotNull ParameterBuilder addMapEntry(@NotNull MapKey key, @NotNull String message) {
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
  @NotNull ParameterBuilder addMapEntry(@NotNull MapKey key,
                                                @NotNull Consumer<MessageBuilder> messageConfigurer)
  {
    final var nestedBuilder = new FluentMessageBuilder(messageFactory);

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

    return builder.text(text);
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

    return builder.parameter(name);
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

    return builder.postFormatter(name);
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

    return builder.template(name);
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

    return builder.build();
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

    return builder.buildWithCode(code);
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

    return builder.buildAsTemplate();
  }


  /**
   * Completes this parameter part in the enclosing message.
   */
  void flush()
  {
    if (!flushed)
    {
      flushed = true;
      finalized = true;
      builder.activePartFlusher = null;

      builder.parts.add(new ParameterPart(name, format, spaceBefore, spaceAfter,
          new MessagePartConfig(config), new MessagePartMap(map)));
    }
  }
}
