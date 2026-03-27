/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.adopter;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;


/**
 * Abstract base class for message adopters. A message adopter reads message information from a
 * particular source (e.g. {@link java.util.ResourceBundle ResourceBundle},
 * {@link java.util.Properties Properties}) and publishes the parsed messages and templates to a
 * {@link MessagePublisher}.
 * <p>
 * This class provides the minimum requirements shared by all message adopters: a
 * {@link MessageFactory} for parsing message format strings and a {@link MessagePublisher} for
 * publishing the resulting messages and templates.
 * <p>
 * Concrete implementations (e.g. {@link ResourceBundleAdopter}, {@link PropertiesAdopter}) define
 * one or more {@code adopt} methods that read from their specific source.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public abstract class AbstractMessageAdopter
{
  /** Message factory instance, not {@code null}. */
  protected final @NotNull MessageFactory messageFactory;

  /** Message publisher instance, not {@code null}. */
  protected final @NotNull MessagePublisher messagePublisher;


  /**
   * Create a message adopter for the given {@code configurableMessageSupport}. The message factory
   * and message publisher are both obtained from the configurable message support instance: the
   * factory via
   * {@link ConfigurableMessageSupport#getMessageAccessor() getMessageAccessor()}.{@link
   * de.sayayi.lib.message.MessageSupport.MessageAccessor#getMessageFactory() getMessageFactory()}
   * and the publisher is the configurable message support itself.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  protected AbstractMessageAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    this(configurableMessageSupport.getMessageAccessor().getMessageFactory(), configurableMessageSupport);
  }


  /**
   * Create a message adopter for the given {@code messageFactory} and {@code publisher}. This
   * constructor allows the message factory and message publisher to be provided independently,
   * which is useful when the publisher is not a {@link ConfigurableMessageSupport} instance or
   * when a custom message factory is required.
   *
   * @param messageFactory  message factory used for parsing message format strings,
   *                        not {@code null}
   * @param publisher       message publisher used for publishing parsed messages and templates,
   *                        not {@code null}
   */
  protected AbstractMessageAdopter(@NotNull MessageFactory messageFactory, @NotNull MessagePublisher publisher)
  {
    this.messageFactory = requireNonNull(messageFactory, "messageFactory must not be null");
    this.messagePublisher = requireNonNull(publisher, "publisher must not be null");
  }
}
