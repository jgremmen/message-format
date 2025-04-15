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

import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


/**
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public enum ConfigValueBool implements ConfigValue
{
  /** Boolean config value representing {@code false}. */
  FALSE,

  /** Boolean config value representing {@code true}. */
  TRUE;


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
   * Return the number as boolean.
   *
   * @return  number as boolean
   *
   * @since 0.8.0
   */
  @Contract(pure = true)
  public boolean booleanValue() {
    return this == TRUE;
  }


  /**
   * Returns the boolean value.
   *
   * @return  boolean, never {@code null}
   */
  @Override
  public @NotNull Boolean asObject() {
    return booleanValue();
  }


  @Override
  public String toString() {
    return Boolean.toString(booleanValue());
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
    packStream.writeBoolean(booleanValue());
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked boolean map value, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  public static @NotNull ConfigValueBool unpack(@NotNull PackInputStream packStream) throws IOException {
    return packStream.readBoolean() ? TRUE : FALSE;
  }
}
