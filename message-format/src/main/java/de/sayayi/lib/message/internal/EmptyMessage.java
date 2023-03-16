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
import de.sayayi.lib.message.MessageSupport.MessageSupportAccessor;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static java.util.Collections.emptySet;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class EmptyMessage implements Message.WithSpaces
{
  private static final long serialVersionUID = 800L;

  public static final Message.WithSpaces INSTANCE = new EmptyMessage();


  private EmptyMessage() {}


  @Override
  @Contract(pure = true)
  public @NotNull String format(@NotNull MessageSupportAccessor messageSupport,
                                @NotNull Parameters parameters) {
    return "";
  }


  @Override
  public boolean isSpaceBefore() {
    return false;
  }


  @Override
  public boolean isSpaceAfter() {
    return false;
  }


  @Override
  public @NotNull Set<String> getTemplateNames() {
    return emptySet();
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof EmptyMessage;
  }


  @Override
  public int hashCode() {
    return EmptyMessage.class.hashCode();
  }
}
