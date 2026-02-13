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
package de.sayayi.lib.message.internal.part.parameter.value;

import de.sayayi.lib.message.part.parameter.ConfigValue.NumberValue;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static de.sayayi.lib.message.internal.pack.PackSupport.packLongVar;
import static de.sayayi.lib.message.internal.pack.PackSupport.unpackLongVar;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;


/**
 * This class represents a numeric configuration value.
 *
 * @param longValue  configuration value number.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public record ConfigValueNumber(long longValue) implements NumberValue
{
  /**
   * Return the number as int.
   * <p>
   * If the number is larger than the integer range, the returned value is either
   * {@code 4294967295} for positive values or {@code −4294967296} for negative values.
   *
   * @return number as int
   * @since 0.8.0
   */
  @Override
  public int intValue() {
    return longValue < MIN_VALUE ? MIN_VALUE : longValue > MAX_VALUE ? MAX_VALUE : (int)longValue;
  }


  /**
   * Returns the number value.
   *
   * @return number, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull Long asObject() {
    return longValue;
  }


  @Override
  @Contract(pure = true)
  public @NotNull String toString() {
    return Long.toString(longValue);
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
    packLongVar(longValue, packStream);
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
  public static @NotNull ConfigValueNumber unpack(@NotNull PackInputStream packStream) throws IOException
  {
    final var number = packStream.getVersion().orElseThrow() == 1
        ? packStream.readLong()
        : unpackLongVar(packStream);

    return new ConfigValueNumber(number);
  }
}
