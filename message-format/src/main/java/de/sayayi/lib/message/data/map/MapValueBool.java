/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.data.map;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import static lombok.AccessLevel.PRIVATE;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor(access = PRIVATE)
public enum MapValueBool implements MapValue
{
  FALSE(false),
  TRUE(true);

  private static final long serialVersionUID = 800L;

  private final boolean bool;


  @Override
  public @NotNull Type getType() {
    return Type.BOOL;
  }


  @Override
  public @NotNull Boolean asObject() {
    return bool;
  }


  @Override
  public String toString() {
    return Boolean.toString(bool);
  }
}