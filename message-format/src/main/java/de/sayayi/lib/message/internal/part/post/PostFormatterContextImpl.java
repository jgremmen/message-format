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
package de.sayayi.lib.message.internal.part.post;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.post.PostFormatterContext;
import de.sayayi.lib.message.internal.part.config.BaseConfigAccessor;
import de.sayayi.lib.message.part.MessagePart.Config;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;


/**
 * Default implementation of {@link PostFormatterContext} used during post format message part evaluation.
 * <p>
 * This context provides access to the post format configuration keys and the formatting locale. Configuration
 * values of type string, number and boolean are resolved through the base class. Message-typed configuration
 * values are not supported; calling {@link #getConfigValueMessage(String)} will throw an
 * {@link UnsupportedOperationException}.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
final class PostFormatterContextImpl extends BaseConfigAccessor implements PostFormatterContext
{
  /**
   * Creates a new post formatter context.
   *
   * @param messageAccessor  message accessor providing locale and default configuration, not {@code null}
   * @param config           post format configuration from the message part, not {@code null}
   */
  PostFormatterContextImpl(@NotNull MessageAccessor messageAccessor, @NotNull Config config) {
    super(messageAccessor, config);
  }


  /**
   * {@inheritDoc}
   *
   * @return  the locale from the message accessor, never {@code null}
   */
  @Override
  public @NotNull Locale getLocale() {
    return messageAccessor.getLocale();
  }


  /**
   * Always throws {@link UnsupportedOperationException}. Message-typed configuration values are not supported
   * in the post formatter context.
   *
   * @param name  configuration key, not {@code null}
   *
   * @return  never returns normally
   *
   * @throws UnsupportedOperationException  always
   */
  @Override
  public @NotNull Optional<Message.WithSpaces> getConfigValueMessage(@NotNull String name) {
    throw new UnsupportedOperationException("getConfigValueMessage");
  }
}
