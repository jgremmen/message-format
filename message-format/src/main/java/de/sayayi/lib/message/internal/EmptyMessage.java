/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Set;


/**
 * Message implementation, representing an empty text without leading/trailing spaces.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public final class EmptyMessage implements Message.WithSpaces
{
  /**
   * Empty message instance.
   */
  public static final Message.WithSpaces INSTANCE = new EmptyMessage();


  private EmptyMessage() {}


  @Override
  public @NotNull String format(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException {
    return "";
  }


  @Override
  public @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException {
    return Text.EMPTY;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@code false}
   */
  @Override
  public boolean isSpaceBefore() {
    return false;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@code false}
   */
  @Override
  public boolean isSpaceAfter() {
    return false;
  }


  @Override
  public @NotNull MessagePart[] getMessageParts() {
    return new MessagePart[] { Text.EMPTY };
  }


  @Override
  public @NotNull Set<String> getTemplateNames() {
    return Set.of();
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof EmptyMessage;
  }


  @Override
  public int hashCode() {
    return EmptyMessage.class.hashCode();
  }


  @Override
  public String toString() {
    return "EmptyMessage";
  }
}
