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
import de.sayayi.lib.message.MessageBuilder.TemplateBuilder;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.part.template.TemplatePart;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.TypedValue;
import de.sayayi.lib.message.template.Template;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static de.sayayi.lib.message.util.MessageUtil.isKebabCaseName;
import static de.sayayi.lib.message.util.MessageUtil.isKebabOrLowerCamelCaseName;
import static java.util.Objects.requireNonNull;


/**
 * Default implementation of {@link TemplateBuilder}.
 * <p>
 * Configures a template reference, including default parameter values and parameter mappings.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@ApiStatus.Internal
public final class TemplateBuilderImpl extends AbstractSpacedBuilder<TemplateBuilder> implements TemplateBuilder
{
  /** Enclosing message builder that receives the completed template part. */
  private final FluentMessageBuilder builder;

  private final @NotNull MessageFactory messageFactory;

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
   * @param builder  enclosing message builder, not {@code null}
   * @param name     template name (must follow kebab-case convention), not {@code null}
   *
   * @throws IllegalArgumentException if {@code name} does not match the kebab-case naming convention
   */
  TemplateBuilderImpl(@NotNull FluentMessageBuilder builder, @NotNull String name)
  {
    this.builder = builder;
    this.messageFactory = builder.messageFactory;
    if (!isKebabCaseName(requireNonNull(name, "name must not be null")))
      throw new IllegalArgumentException("template name '" + name + "' must match the kebab-case naming convention");

    this.name = name;
    this.defaultParameters = new LinkedHashMap<>();
    this.parameterDelegates = new LinkedHashMap<>();
  }


  /**
   * Completes this template part in the enclosing message.
   */
  void flush()
  {
    if (!flushed)
    {
      flushed = true;
      builder.activePartFlusher = null;

      builder.parts.add(new TemplatePart(name, spaceBefore, spaceAfter, defaultParameters, parameterDelegates));
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

    final var nestedBuilder = new FluentMessageBuilder(messageFactory);

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
}
