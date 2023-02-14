/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
@EqualsAndHashCode(doNotUseGetters = true)
public class DataNumber implements Data
{
  private static final long serialVersionUID = 800L;

  private final long number;


  @Override
  @Contract(pure = true)
  public String toString() {
    return Long.toString(number);
  }


  @Override
  @Contract(pure = true)
  public @NotNull Long asObject() {
    return number;
  }
}
