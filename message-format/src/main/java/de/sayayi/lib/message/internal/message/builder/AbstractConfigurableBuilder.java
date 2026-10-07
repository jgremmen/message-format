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
import de.sayayi.lib.message.MessageBuilder.ConfigurableBuilder;
import de.sayayi.lib.message.MessageBuilder.SpacedBuilder;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.TypedValue;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import static de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool.FALSE;
import static de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool.TRUE;
import static de.sayayi.lib.message.util.MessageUtil.isKebabCaseName;
import static java.util.Objects.requireNonNull;


/**
 * Abstract base class for sub-builders that implement {@link ConfigurableBuilder}, providing common configuration
 * methods.
 *
 * @param <S>  the self type of the sub-builder
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@ApiStatus.Internal
public abstract non-sealed class AbstractConfigurableBuilder<S extends ConfigurableBuilder<S> & SpacedBuilder<S>>
    extends AbstractSpacedBuilder<S>
    implements ConfigurableBuilder<S>
{
  /** Enclosing message builder that receives the completed part. */
  protected final FluentMessageBuilder builder;

  /** Message factory used to convert string values into parsed message values. */
  protected final MessageFactory messageFactory;

  /** Configuration values collected for the part being built. */
  protected final @NotNull Map<String,TypedValue<?>> config;

  /** Indicates whether this part's configuration has been finalized. */
  protected boolean finalized;


  /**
   * Construct a new configurable builder with an empty configuration map.
   *
   * @param builder  enclosing message builder, not {@code null}
   */
  protected AbstractConfigurableBuilder(@NotNull FluentMessageBuilder builder)
  {
    this.builder = requireNonNull(builder, "builder must not be null");
    this.messageFactory = builder.messageFactory;
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
    return withConfig(name, value ? TRUE : FALSE);
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
