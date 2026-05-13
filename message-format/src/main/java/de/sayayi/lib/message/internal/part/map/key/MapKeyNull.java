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
 * Internal implementation of {@link MapKey} representing a null map key. This enum provides two singleton constants
 * for the two supported compare types: {@link #EQ} (matches {@code null} values) and {@link #NE} (matches
 * non-{@code null} values).
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public enum MapKeyNull implements MapKey
{
  /** Null map key with compare type {@link MapKey.CompareType#EQ EQ}. */
  EQ,

  /** Null map key with compare type {@link MapKey.CompareType#NE NE}. */
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
   * @return  always {@link Type#NULL Type#NULL}
   */
  @Override
  public @NotNull Type getType() {
    return Type.NULL;
  }


  /**
   * Serializes this null map key into its format string representation (e.g. {@code "null"} or {@code "<>null"}).
   *
   * @param context  the serialization context, not {@code null}
   */
  @Override
  public void serialize(@NotNull Context context) {
    context.textJoiner().addNoSpace(getCompareType().asPrefix() + "null");
  }


  /**
   * Returns the string representation of this null map key.
   *
   * @return  string representation, never {@code null}
   */
  @Override
  public String toString() {
    return getCompareType().asPrefix() + "null";
  }


  /**
   * Writes this null map key to the given pack output stream for binary serialization.
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
   * Reads a {@code MapKeyNull} from the given pack input stream.
   *
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked null map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull MapKeyNull unpack(@NotNull PackInputStream packStream) throws IOException {
    return packStream.readBoolean() ? EQ : NE;
  }
}
