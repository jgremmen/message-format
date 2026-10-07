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
import de.sayayi.lib.message.MessageBuilder.TextBuilder;
import de.sayayi.lib.message.template.Template;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.TextPartFactory.addSpaces;
import static de.sayayi.lib.message.part.TextPartFactory.spacedText;
import static java.util.Objects.requireNonNull;


/**
 * Default implementation of {@link TextBuilder}.
 * <p>
 * Represents a literal text part while it is being added to a message.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@ApiStatus.Internal
public final class TextBuilderImpl extends AbstractSpacedBuilder<TextBuilder> implements TextBuilder
{
  /** Enclosing message builder that receives the completed text part. */
  private final @NotNull FluentMessageBuilder builder;

  private final @NotNull String text;

  /** Indicates whether this text part has already been written to the enclosing builder. */
  private boolean flushed;


  /**
   * Construct a new text builder for the given literal text.
   *
   * @param builder  enclosing message builder, not {@code null}
   * @param text     literal text, not {@code null}
   */
  TextBuilderImpl(@NotNull FluentMessageBuilder builder, @NotNull String text)
  {
    this.builder = builder;
    this.text = text;
  }


  /**
   * Completes this text part, including its spacing settings, in the enclosing message.
   */
  void flush()
  {
    if (!flushed)
    {
      flushed = true;
      builder.parts.add(addSpaces(spacedText(text), spaceBefore, spaceAfter));
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

    return builder.text(requireNonNull(text, "text must not be null"));
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
}
