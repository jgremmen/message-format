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
import de.sayayi.lib.message.MessageBuilder.PostFormatterBuilder;
import de.sayayi.lib.message.internal.message.EmptyMessage;
import de.sayayi.lib.message.internal.part.config.MessagePartConfig;
import de.sayayi.lib.message.internal.part.post.PostFormatterPart;
import de.sayayi.lib.message.template.Template;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static de.sayayi.lib.message.util.MessageUtil.isKebabCaseName;
import static java.util.Objects.requireNonNull;


/**
 * Default implementation of {@link PostFormatterBuilder}.
 * <p>
 * Configures a post formatter, including its input message and configuration values.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@ApiStatus.Internal
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
   * Constructs a new post-formatter builder for the given formatter name.
   *
   * @param builder  enclosing message builder, not {@code null}
   * @param name     post-formatter name (must follow kebab-case convention), not {@code null}
   *
   * @throws IllegalArgumentException if {@code name} does not match the kebab-case naming convention
   */
  PostFormatterBuilderImpl(@NotNull FluentMessageBuilder builder, @NotNull String name)
  {
    super(builder);

    if (!isKebabCaseName(requireNonNull(name, "name must not be null")))
    {
      throw new IllegalArgumentException("post-formatter name '" + name +
          "' must match the kebab-case naming convention");
    }

    this.name = name;
    this.innerMessage = EmptyMessage.INSTANCE;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Post formatter configurations do not support message values, so this method always throws an exception.
   *
   * @throws IllegalArgumentException  always
   */
  @Override
  public @NotNull PostFormatterBuilder configMessage(@NotNull String name, @NotNull Message.WithSpaces message) {
    throw new IllegalArgumentException("post-formatter config cannot be a message value");
  }


  /**
   * Completes this post-formatter part in the enclosing message.
   */
  void flush()
  {
    if (!flushed)
    {
      flushed = true;
      finalized = true;
      builder.activePartFlusher = null;

      builder.parts.add(new PostFormatterPart(name, innerMessage, spaceBefore, spaceAfter, new MessagePartConfig(config)));
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull PostFormatterBuilder withMessage(@NotNull String message) {
    return withMessage(messageFactory.parseMessage(requireNonNull(message, "message must not be null")));
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull PostFormatterBuilder withMessage(@NotNull Message.WithSpaces message)
  {
    innerMessage = requireNonNull(message, "message must not be null");

    return this;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull PostFormatterBuilder withMessage(@NotNull Consumer<MessageBuilder> messageConfigurer)
  {
    requireNonNull(messageConfigurer, "messageConfigurer must not be null");

    final var nestedBuilder = new FluentMessageBuilder(messageFactory);

    messageConfigurer.accept(nestedBuilder);

    innerMessage = nestedBuilder.build();

    return this;
  }


  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException  if the enclosing builder has already been built
   */
  @Override
  public @NotNull TextBuilder text(@NotNull String text)
  {
    flush();

    return builder.text(text);
  }


  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException  if {@code name} does not use a supported naming convention
   * @throws IllegalStateException     if the enclosing builder has already been built
   */
  @Override
  public @NotNull ParameterBuilder parameter(@NotNull String name)
  {
    flush();

    return builder.parameter(name);
  }


  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException  if {@code name} does not use kebab-case
   * @throws IllegalStateException     if the enclosing builder has already been built
   */
  @Override
  public @NotNull PostFormatterBuilder postFormatter(@NotNull String name)
  {
    flush();

    return builder.postFormatter(name);
  }


  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException  if {@code name} does not use kebab-case
   * @throws IllegalStateException     if the enclosing builder has already been built
   */
  @Override
  public @NotNull TemplateBuilder template(@NotNull String name)
  {
    flush();

    return builder.template(name);
  }


  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException  if the enclosing builder has already been built
   */
  @Override
  public @NotNull Message.WithSpaces build()
  {
    flush();

    return builder.build();
  }


  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException  if the enclosing builder has already been built
   */
  @Override
  public @NotNull Message.WithCode buildWithCode(@NotNull String code)
  {
    flush();

    return builder.buildWithCode(code);
  }


  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException  if the enclosing builder has already been built
   */
  @Override
  public @NotNull Template buildAsTemplate()
  {
    flush();

    return builder.buildAsTemplate();
  }
}
