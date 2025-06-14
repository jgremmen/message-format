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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static de.sayayi.lib.message.internal.pack.PackSupport.packLongVar;
import static de.sayayi.lib.message.internal.pack.PackSupport.unpackLongVar;
import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public final class ConfigKeyNumber implements ConfigKey
{
  /** Configuration number key comparison type. */
  private final @NotNull CompareType compareType;

  /** Configuration key number. */
  private final long number;


  /**
   * Constructs a configuration key number with a comparison type.
   *
   * @param compareType  configuration key comparison type, not {@code null}
   * @param number       configuration key number
   *
   * @since 0.4.0 (made public in 0.10.0)
   */
  public ConfigKeyNumber(@NotNull CompareType compareType, long number)
  {
    this.compareType = requireNonNull(compareType, "compareType must not be null");
    this.number = number;
  }


  @Override
  public @NotNull CompareType getCompareType() {
    return compareType;
  }


  /**
   * Returns the config key number value.
   *
   * @return  config key number value
   */
  @Contract(pure = true)
  public long getNumber() {
    return number;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@link Type#NUMBER Type#NUMBER}
   */
  @Override
  public @NotNull Type getType() {
    return Type.NUMBER;
  }


  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof ConfigKeyNumber))
      return false;

    final var that = (ConfigKeyNumber)o;

    return number == that.number && compareType == that.compareType;
  }


  @Override
  public int hashCode() {
    return (59 + Long.hashCode(number)) * 59 + compareType.hashCode();
  }


  @Override
  public String toString() {
    return compareType.asPrefix() + number;
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
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeEnum(compareType);
    packLongVar(number, packStream);
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked number map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  public static @NotNull ConfigKeyNumber unpack(@NotNull PackInputStream packStream) throws IOException
  {
    final var compareType = packStream.readEnum(CompareType.class);
    final var number = packStream.getVersion().orElseThrow() == 1
        ? packStream.readLong()
        : unpackLongVar(packStream);

    return new ConfigKeyNumber(compareType, number);
  }
}
