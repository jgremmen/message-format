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

import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


/**
 * The empty configuration key represents values which are considered empty by their associated
 * parameter formatter.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public enum ConfigKeyEmpty implements ConfigKey
{
  /** Empty config key with compare type {@link de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType#EQ EQ}. */
  EQ,

  /** Empty config key with compare type {@link de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType#NE NE}. */
  NE;


  @Override
  public @NotNull CompareType getCompareType() {
    return this == EQ ? CompareType.EQ : CompareType.NE;
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
  public String toString() {
    return getCompareType().asPrefix() + "empty";
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
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    packStream.writeBoolean(this == EQ);
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
  public static @NotNull ConfigKeyEmpty unpack(@NotNull PackInputStream packStream) throws IOException {
    return packStream.readBoolean() ? EQ : NE;
  }
}
