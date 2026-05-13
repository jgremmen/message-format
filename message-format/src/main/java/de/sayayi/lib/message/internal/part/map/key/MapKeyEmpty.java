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
package de.sayayi.lib.message.internal.part.map.key;

import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


/**
 * Internal implementation of {@link MapKey} representing an empty map key. This enum provides
 * two singleton constants for the two supported compare types: {@link #EQ} (matches empty values)
 * and {@link #NE} (matches non-empty values).
 * <p>
 * What constitutes an "empty" value is determined by the associated parameter formatter.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public enum MapKeyEmpty implements MapKey
{
  /** Empty map key with compare type {@link MapKey.CompareType#EQ EQ}. */
  EQ,

  /** Empty map key with compare type {@link MapKey.CompareType#NE NE}. */
  NE;


  /**
   * {@inheritDoc}
   */
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


  /**
   * Serializes this empty map key into its format string representation (e.g. {@code "empty"} or {@code "<>empty"}).
   *
   * @param context  the serialization context, not {@code null}
   */
  @Override
  public void serialize(@NotNull Context context) {
    context.textJoiner().addNoSpace(getCompareType().asPrefix() + "empty");
  }


  /**
   * Returns the string representation of this empty map key.
   *
   * @return  string representation, never {@code null}
   */
  @Override
  public String toString() {
    return getCompareType().asPrefix() + "empty";
  }


  /**
   * Writes this empty map key to the given pack output stream for binary serialization.
   *
   * @param packStream  data output pack target, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    packStream.writeBoolean(this == EQ);
  }


  /**
   * Reads a {@code MapKeyEmpty} from the given pack input stream.
   *
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked empty map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull MapKeyEmpty unpack(@NotNull PackInputStream packStream) throws IOException {
    return packStream.readBoolean() ? EQ : NE;
  }
}
