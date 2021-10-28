/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.SortedSet;

import static java.util.Collections.emptySortedSet;
import static lombok.AccessLevel.PRIVATE;


/**
 * @author Jeroen Gremmen
 */
@ToString
@NoArgsConstructor(access = PRIVATE)
public final class EmptyMessage implements Message.WithSpaces
{
  private static final long serialVersionUID = 500L;

  public static final Message.WithSpaces INSTANCE = new EmptyMessage();


  @Override
  @Contract(value = "_, _ -> null", pure = true)
  public String format(@NotNull MessageContext messageContext, @NotNull Parameters parameters) {
    return null;
  }


  @Override
  @Contract(value = "-> false", pure = true)
  public boolean hasParameters() {
    return false;
  }


  @Override
  public @NotNull SortedSet<String> getParameterNames() {
    return emptySortedSet();
  }


  @Override
  public boolean isSpaceBefore() {
    return false;
  }


  @Override
  public boolean isSpaceAfter() {
    return false;
  }
}
