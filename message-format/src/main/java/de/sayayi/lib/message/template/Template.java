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

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.internal.MessageTemplate;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * Represents a reusable template that can be registered by name in a
 * {@link MessageSupport.ConfigurableMessageSupport ConfigurableMessageSupport} and referenced
 * from messages.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 *
 * @see MessageAccessor#getTemplateByName(String)
 */
public sealed interface Template permits MessageTemplate, NamedTemplate
{
  /**
   * Tells whether this template is semantically equivalent to the given {@code template}.
   *
   * @param template  template to compare with, not {@code null}
   *
   * @return  {@code true} if both templates are considered the same, {@code false} otherwise
   */
  @Contract(pure = true)
  boolean isSame(@NotNull Template template);


  /**
   * Formats this template as a text part using the given message accessor and parameters.
   * <p>
   * The returned text may have leading or trailing spaces, but they are not significant. At formatting time, any such
   * spaces are removed and replaced by the spaces surrounding the template reference {@code %[ ... ]} placeholder in
   * the enclosing message.
   *
   * @param messageAccessor  message accessor providing access to messages, templates and formatters, not {@code null}
   * @param parameters       formatting parameters, not {@code null}
   *
   * @return  formatted text, never {@code null}
   *
   * @throws MessageFormatException  if an error occurs during formatting
   */
  @Contract(pure = true)
  @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException;
}
