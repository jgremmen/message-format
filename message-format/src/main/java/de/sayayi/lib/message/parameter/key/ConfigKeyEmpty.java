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
package de.sayayi.lib.message.parameter.key;

import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

import static de.sayayi.lib.message.parameter.key.ConfigKey.CompareType.EQ;
import static de.sayayi.lib.message.parameter.key.ConfigKey.CompareType.NE;
import static de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult.*;


/**
 * @author Jeroen Gremmen
 */
public final class ConfigKeyEmpty implements ConfigKey
{
  private static final long serialVersionUID = 800L;

  /** Configuration empty key comparison type. */
  private final @NotNull CompareType compareType;


  public ConfigKeyEmpty(@NotNull CompareType compareType)
  {
    if (compareType != EQ && compareType != NE)
      throw new IllegalArgumentException("compareType must be EQ or NE");

    this.compareType = compareType;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@link Type#EMPTY Type#EMPTY}
   */
  @Override
  public @NotNull Type getType() {
    return Type.EMPTY;
  }


  @Override
  public @NotNull MatchResult match(@NotNull MessageAccessor messageAccessor,
                                    @NotNull Locale locale, Object value)
  {
    if (value == null)
      return compareType == EQ ? TYPELESS_LENIENT : MISMATCH;

    for(ParameterFormatter formatter: messageAccessor.getFormatters(value.getClass()))
      if (formatter instanceof EmptyMatcher)
      {
        final MatchResult result = ((EmptyMatcher)formatter).matchEmpty(compareType, value);
        assert result == TYPELESS_LENIENT || result == TYPELESS_EXACT || result == null;

        if (result != null)
          return result;
      }

    return MISMATCH;
  }


  @Override
  public boolean equals(Object o)
  {
    return o == this ||
        o instanceof ConfigKeyEmpty && compareType == ((ConfigKeyEmpty)o).compareType;
  }


  @Override
  public int hashCode() {
    return 59 + compareType.hashCode();
  }


  @Override
  public String toString() {
    return compareType.asPrefix() + "empty";
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
   * @return  unpacked empty map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull ConfigKeyEmpty unpack(@NotNull PackInputStream packStream)
      throws IOException {
    return new ConfigKeyEmpty(packStream.readEnum(CompareType.class));
  }
}
