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
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Locale;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
public final class MapKeyName implements MapKey
{
  @Getter private final String name;


  @NotNull
  @Override
  public Type getType() {
    return Type.NAME;
  }


  @NotNull
  @Override
  public MatchResult match(@NotNull Locale locale, Serializable value)
  {
    return (value instanceof CharSequence || value instanceof Character) && value.toString().equals(name)
        ? MatchResult.EXACT : MatchResult.MISMATCH;
  }
}
