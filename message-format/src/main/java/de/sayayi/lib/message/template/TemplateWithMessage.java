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
package de.sayayi.lib.message.template;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.MessageTemplate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A template backed by a {@link Message}, which provides the template content.
 * <p>
 * Templates of this kind are created when templates are defined using message format strings or imported from pack
 * files. Formatting such a template formats its message.
 *
 * @since 0.25.1
 */
public sealed interface TemplateWithMessage extends Template permits MessageTemplate
{
  /**
   * Returns the message providing the content of this template.
   *
   * @return  template message, never {@code null}
   */
  @Contract(pure = true)
  @NotNull Message getMessage();


  /**
   * {@inheritDoc}
   * <p>
   * This implementation considers {@code template} the same if it is a message template whose message is the same as
   * the message of this template, as determined by {@link MessageFactory#isSame(Message, Message)}.
   */
  @Contract(pure = true)
  default boolean isSame(@NotNull Template template)
  {
    return
        template instanceof TemplateWithMessage templateWithMessage &&
            MessageFactory.isSame(getMessage(), templateWithMessage.getMessage());
  }
}
