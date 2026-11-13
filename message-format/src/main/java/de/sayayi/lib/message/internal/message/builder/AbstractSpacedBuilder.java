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
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;


/**
 * Base class for message part builders that support spacing before and after their part.
 *
 * @param <S>  the self type of the sub-builder
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@Internal
public abstract non-sealed class AbstractSpacedBuilder<S extends SpacedBuilder<S>> implements SpacedBuilder<S>
{
  /** Whether a space should be inserted before the part produced by this builder. */
  protected boolean spaceBefore;

  /** Whether a space should be inserted after the part produced by this builder. */
  protected boolean spaceAfter;

  /** Indicates whether the part has already been completed and written to the enclosing builder. */
  protected boolean flushed;


  /**
   * Ensures that the part has not been completed yet. A part is completed when the next part is started or the message
   * is built; changes made to its builder afterwards would be lost.
   *
   * @throws IllegalStateException  if the part has already been completed
   *
   * @since 0.25.0
   */
  protected void checkNotFlushed()
  {
    if (flushed)
      throw new IllegalStateException("part has already been completed and cannot be changed");
  }


  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NotNull S spaceBefore()
  {
    checkNotFlushed();
    spaceBefore = true;

    return (S)this;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public @NotNull S spaceAfter()
  {
    checkNotFlushed();
    spaceAfter = true;

    return (S)this;
  }
}
