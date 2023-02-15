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
package de.sayayi.lib.message.data.map;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

import static de.sayayi.lib.message.data.map.MapKey.CompareType.EQ;
import static de.sayayi.lib.message.data.map.MapKey.CompareType.NE;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_EXACT;


/**
 * @author Jeroen Gremmen
 */
@ToString(doNotUseGetters = true)
@EqualsAndHashCode(doNotUseGetters = true)
public final class MapKeyNull implements MapKey
{
  private static final long serialVersionUID = 800L;

  private final @NotNull CompareType compareType;


  public MapKeyNull(@NotNull CompareType compareType)
  {
    if (compareType != EQ && compareType != NE)
      throw new IllegalArgumentException("compareType must be EQ or NE");

    this.compareType = compareType;
  }


  @Override
  public @NotNull Type getType() {
    return Type.NULL;
  }


  @Override
  public @NotNull MatchResult match(@NotNull MessageContext messageContext, @NotNull Locale locale, Object value)
  {
    if (value == null && compareType == EQ)
      return TYPELESS_EXACT;
    if (value != null && compareType == NE)
      return TYPELESS_EXACT;

    return MatchResult.MISMATCH;
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    packStream.writeEnum(compareType);
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked null map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull MapKeyNull unpack(@NotNull PackInputStream packStream) throws IOException {
    return new MapKeyNull(packStream.readEnum(CompareType.class));
  }
}
