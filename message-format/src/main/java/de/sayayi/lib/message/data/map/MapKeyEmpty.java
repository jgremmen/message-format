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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Locale;

import static de.sayayi.lib.message.data.map.MapKey.CompareType.EQ;
import static de.sayayi.lib.message.data.map.MapKey.CompareType.NE;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.*;


/**
 * @author Jeroen Gremmen
 */
@ToString(doNotUseGetters = true)
@EqualsAndHashCode(doNotUseGetters = true)
public final class MapKeyEmpty implements MapKey
{
  private static final long serialVersionUID = 800L;

  private final @NotNull CompareType compareType;


  public MapKeyEmpty(@NotNull CompareType compareType)
  {
    if (compareType != EQ && compareType != NE)
      throw new IllegalArgumentException("compareType must be EQ or NE");

    this.compareType = compareType;
  }


  @Override
  public @NotNull Type getType() {
    return Type.EMPTY;
  }


  @Override
  public @NotNull MatchResult match(@NotNull MessageContext messageContext, @NotNull Locale locale, Object value)
  {
    if (value == null)
      return compareType == EQ ? TYPELESS_LENIENT : MISMATCH;

    for(ParameterFormatter formatter: messageContext.getFormatters(value.getClass()))
      if (formatter instanceof EmptyMatcher)
      {
        final MatchResult result = ((EmptyMatcher)formatter).matchEmpty(compareType, value);
        assert result == TYPELESS_LENIENT || result == TYPELESS_EXACT || result == null;

        if (result != null)
          return result;
      }

    return MISMATCH;
  }


  /**
   * @param dataOutput  data output pack target
   *
   * @throws IOException  if an I/O error occurs.
   *
   * @since 0.8.0
   */
  public void pack(@NotNull DataOutput dataOutput) throws IOException
  {
    dataOutput.writeByte(2);
    dataOutput.writeByte(compareType.ordinal());
  }


  /**
   * @param dataInput  source data input, not {@code null}
   *
   * @return  unpacked empty map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs.
   *
   * @since 0.8.0
   */
  public static @NotNull MapKeyEmpty unpack(@NotNull DataInput dataInput) throws IOException {
    return new MapKeyEmpty(CompareType.values()[dataInput.readByte() & 0xf]);
  }
}
