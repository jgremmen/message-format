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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static java.util.Collections.emptySet;


/**
 * @author Jeroen Gremmen
 */
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EmptyMessage implements Message.WithSpaces
{
  private static final long serialVersionUID = 500L;

  public static final Message.WithSpaces INSTANCE = new EmptyMessage();


  @Override
  @Contract(value = "_ -> null", pure = true)
  public String format(@NotNull Parameters parameters) {
    return null;
  }


  @Override
  @Contract(value = "-> false", pure = true)
  public boolean hasParameters() {
    return false;
  }


  @Override
  public @NotNull Set<String> getParameterNames() {
    return emptySet();
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
