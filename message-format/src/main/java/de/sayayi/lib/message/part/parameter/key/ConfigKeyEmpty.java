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
package de.sayayi.lib.message.part.parameter.key;

import de.sayayi.lib.message.internal.pack.PackInputStream;
import de.sayayi.lib.message.internal.pack.PackOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.EQ;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.NE;


/**
 * The empty configuration key represents values which are considered empty by their associated
 * parameter formatter.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public final class ConfigKeyEmpty implements ConfigKey
{
  /** Configuration empty key comparison type. */
  private final @NotNull CompareType compareType;


  /**
   * Constructs the empty configuration key with the given {@code compareType}.
   *
   * @param compareType  comparison type for the empty key, only
   *                     {@link de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType#EQ EQ} and
   *                     {@link de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType#NE NE}
   *                     are allowed
   */
  public ConfigKeyEmpty(@NotNull CompareType compareType)
  {
    if (compareType != EQ && compareType != NE)
      throw new IllegalArgumentException("compareType must be EQ or NE");

    this.compareType = compareType;
  }


  @Override
  public @NotNull CompareType getCompareType() {
    return compareType;
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
  public boolean equals(Object o) {
    return o instanceof ConfigKeyEmpty && compareType == ((ConfigKeyEmpty)o).compareType;
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
   *
   * @hidden
   */
  @SuppressWarnings("ClassEscapesDefinedScope")
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
   *
   * @hidden
   */
  @SuppressWarnings("ClassEscapesDefinedScope")
  public static @NotNull ConfigKeyEmpty unpack(@NotNull PackInputStream packStream) throws IOException {
    return new ConfigKeyEmpty(packStream.readEnum(CompareType.class));
  }
}
