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
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.MessageTemplate;
import de.sayayi.lib.message.internal.message.CompoundMessage;
import de.sayayi.lib.message.internal.message.EmptyMessage;
import de.sayayi.lib.message.internal.message.TextMessage;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextJoiner;
import de.sayayi.lib.message.template.Template;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;


/**
 * Internal {@link MessageBuilder} implementation for assembling messages from text, parameter, post-formatter and
 * template parts.
 * <p>
 * The builder creates part-specific builders and produces a completed {@link Message} or {@link Template}.
 * <p>
 * This class is <strong>not thread-safe</strong>. A builder instance must only be used from a single thread and must
 * not be reused after calling {@link #build()}, {@link #buildWithCode(String)} or {@link #buildAsTemplate()}.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 *
 * @see MessageBuilder
 * @see MessageFactory#messageBuilder()
 */
@ApiStatus.Internal
public final class FluentMessageBuilder implements MessageBuilder
{
  /** Message factory used to parse nested messages and create coded messages. */
  final @NotNull MessageFactory messageFactory;

  /** Collected parts of the message currently being assembled. */
  final @NotNull List<MessagePart> parts;

  /** Flush callback for the currently active part builder, if any. */
  Runnable activePartFlusher;

  /** Indicates whether this builder has already produced its final message. */
  private boolean built;


  /**
   * Construct a new builder using the given {@code messageFactory}.
   *
   * @param messageFactory  message factory, not {@code null}
   */
  public FluentMessageBuilder(@NotNull MessageFactory messageFactory)
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

    final var builder = new TextBuilderImpl(this, requireNonNull(text, "text must not be null"));

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

    final var builder = new ParameterBuilderImpl(this, name);

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

    final var builder = new PostFormatterBuilderImpl(this, name);

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

    final var builder = new TemplateBuilderImpl(this, name);

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
   * @throws IllegalStateException  if this builder has already been built or the message contains template references
   */
  @Override
  public @NotNull Template buildAsTemplate()
  {
    final var message = build();

    if (!message.getTemplateNames().isEmpty())
      throw new IllegalStateException("template must not contain template references");

    return new MessageTemplate(message);
  }


  /**
   * Completes the active part, if one exists.
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
   * Ensures this builder has not already produced a message or template.
   *
   * @throws IllegalStateException if this builder has already produced a message or template
   */
  private void checkNotBuilt()
  {
    if (built)
      throw new IllegalStateException("builder must not be reused after calling build() or buildWithCode()");
  }
}
