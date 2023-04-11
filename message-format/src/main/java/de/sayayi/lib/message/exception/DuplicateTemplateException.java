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
package de.sayayi.lib.message.exception;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;


/**
 * Duplicate template exception. This exception is thrown by the default template handler if a
 * template with the same name is published twice.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 *
 * @see de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport#setTemplateHandler(Predicate)
 *      ConfigurableMessageSupport#setTemplateHandler(Predicate)
 */
public final class DuplicateTemplateException extends MessageException
{
  /** Duplicate template name. */
  private final @NotNull String name;


  public DuplicateTemplateException(@NotNull String name, String message) {
    this(name, message, null);
  }


  public DuplicateTemplateException(@NotNull String name, String message, Throwable cause)
  {
    super(message, cause);

    this.name = name;
  }


  /**
   * Returns the template name which has been identified as being a duplicate.
   *
   * @return  duplicate template name, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull String getName() {
    return name;
  }
}
