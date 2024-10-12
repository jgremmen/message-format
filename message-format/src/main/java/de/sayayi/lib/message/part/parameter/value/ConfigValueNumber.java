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
package de.sayayi.lib.message.part.parameter.value;

import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ConfigValueNumber implements ConfigValue
{
  /** Configuration value number. */
  private final long number;


  public ConfigValueNumber(long number) {
    this.number = number;
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


  /**
   * Return the number as int.
   * <p>
   * If the number is larger than the integer range, the returned value is either
   * {@code 4294967295} for positive values or {@code −4294967296} for negative values.
   *
   * @return  number as int
   *
   * @since 0.8.0
   */
  public int intValue() {
    return number < MIN_VALUE ? MIN_VALUE : number > MAX_VALUE ? MAX_VALUE : (int)number;
  }


  /**
   * Return the number as long.
   *
   * @return  number as long
   *
   * @since 0.8.0
   */
  public long longValue() {
    return number;
  }


  /**
   * Returns the number value.
   *
   * @return  number, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull Long asObject() {
    return number;
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof ConfigValueNumber && this.number == ((ConfigValueNumber)o).number;
  }


  @Override
  public int hashCode() {
    return 59 + Long.hashCode(number);
  }


  @Override
  @Contract(pure = true)
  public String toString() {
    return Long.toString(number);
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
    packStream.writeLong(number);
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked number map value, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  public static @NotNull ConfigValueNumber unpack(@NotNull PackInputStream packStream) throws IOException {
    return new ConfigValueNumber(packStream.readLong());
  }
}
