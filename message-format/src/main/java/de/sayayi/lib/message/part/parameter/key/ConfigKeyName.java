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

import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public final class ConfigKeyName implements ConfigKey
{
  private static final long serialVersionUID = 800L;

  /** Configuration key name. */
  private final @NotNull String name;


  /**
   * Constructs a configuration key with {@code name}.
   *
   * @param name  configuration key name, not {@code null} or empty
   */
  public ConfigKeyName(@NotNull String name)
  {
    if ((this.name = requireNonNull(name, "name must not be null")).isEmpty())
      throw new IllegalArgumentException("name must not be empty");
  }


  /**
   * Returns the config key name.
   *
   * @return  config key name, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull String getName() {
    return name;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@link Type#NAME Type#NAME}
   */
  @Override
  public @NotNull Type getType() {
    return Type.NAME;
  }


  @Override
  public boolean equals(Object o) {
    return o == this || o instanceof ConfigKeyName && name.equals(((ConfigKeyName)o).name);
  }


  @Override
  public int hashCode() {
    return 59 + name.hashCode();
  }


  @Override
  public String toString() {
    return name;
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
  @SuppressWarnings("JavadocDeclaration")
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    packStream.writeString(name);
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked name map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  @SuppressWarnings("JavadocDeclaration")
  public static @NotNull ConfigKeyName unpack(@NotNull PackInputStream packStream)
      throws IOException {
    return new ConfigKeyName(requireNonNull(packStream.readString()));
  }
}
