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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


/**
 * Internal implementation of {@link MapKey} representing a boolean map key. This enum provides two singleton constants,
 * {@link #TRUE} and {@link #FALSE}, corresponding to the two boolean key values.
 * <p>
 * Boolean map keys always use the {@link CompareType#EQ EQ} compare type.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public enum MapKeyBool implements MapKey
{
  /** Boolean map key representing {@code false}. */
  FALSE,

  /** Boolean map key representing {@code true}. */
  TRUE;


  /**
   * Returns the boolean value of this map key.
   *
   * @return  {@code true} for {@link #TRUE}, {@code false} for {@link #FALSE}
   */
  @Contract(pure = true)
  public boolean isBool() {
    return this == TRUE;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@link Type#BOOL Type#BOOL}
   */
  @Override
  public @NotNull Type getType() {
    return Type.BOOL;
  }


  /**
   * Serializes this boolean map key into its format string representation ({@code "true"} or {@code "false"}).
   *
   * @param context  the serialization context, not {@code null}
   */
  @Override
  public void serialize(@NotNull Context context) {
    context.textJoiner().addNoSpace(Boolean.toString(isBool()));
  }


  /**
   * Returns the string representation of this boolean map key ({@code "true"} or {@code "false"}).
   *
   * @return  string representation, never {@code null}
   */
  @Override
  public String toString() {
    return Boolean.toString(isBool());
  }


  /**
   * Writes this boolean map key to the given pack output stream for binary serialization.
   *
   * @param packStream  data output pack target, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    packStream.writeBoolean(isBool());
  }


  /**
   * Reads a {@code MapKeyBool} from the given pack input stream.
   *
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked boolean map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull MapKeyBool unpack(@NotNull PackInputStream packStream) throws IOException {
    return packStream.readBoolean() ? TRUE : FALSE;
  }
}
