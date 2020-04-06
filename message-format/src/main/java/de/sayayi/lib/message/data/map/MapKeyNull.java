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

import de.sayayi.lib.message.Message.Parameters;
import org.jetbrains.annotations.NotNull;


/**
 * @author Jeroen Gremmen
 */
public final class MapKeyNull implements MapKey
{
  private final CompareType compareType;


  public MapKeyNull(@NotNull CompareType compareType)
  {
    if (compareType != CompareType.EQ && compareType != CompareType.NE)
      throw new IllegalArgumentException("compareType must be EQ or NE");

    this.compareType = compareType;
  }


  @NotNull
  @Override
  public Type getType() {
    return Type.NULL;
  }


  @NotNull
  @Override
  public MatchResult match(@NotNull Parameters parameters, Object value)
  {
    if (value == null && compareType == CompareType.EQ)
      return MatchResult.TYPELESS_EXACT;
    if (value != null && compareType == CompareType.NE)
      return MatchResult.TYPELESS_EXACT;

    return MatchResult.MISMATCH;
  }
}
