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

import de.sayayi.lib.message.MessageBuilder.SpacedBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;


/**
 * Base class for message part builders that support spacing before and after their part.
 *
 * @param <S>  the self type of the sub-builder
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@ApiStatus.Internal
public abstract non-sealed class AbstractSpacedBuilder<S extends SpacedBuilder<S>> implements SpacedBuilder<S>
{
  /** Whether a space should be inserted before the part produced by this builder. */
  protected boolean spaceBefore;

  /** Whether a space should be inserted after the part produced by this builder. */
  protected boolean spaceAfter;


  /**
   * Marks the part produced by this builder to include a leading space when it is flushed.
   *
   * @return  this builder, never {@code null}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NotNull S spaceBefore()
  {
    spaceBefore = true;

    return (S)this;
  }


  /**
   * Marks the part produced by this builder to include a trailing space when it is flushed.
   *
   * @return  this builder, never {@code null}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NotNull S spaceAfter()
  {
    spaceAfter = true;

    return (S)this;
  }
}
