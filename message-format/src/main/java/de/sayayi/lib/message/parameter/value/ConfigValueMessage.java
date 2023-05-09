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
package de.sayayi.lib.message.parameter.value;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
public final class ConfigValueMessage implements ConfigValue
{
  private static final long serialVersionUID = 800L;

  /** Configuration value message. */
  private final @NotNull Message.WithSpaces message;


  public ConfigValueMessage(@NotNull Message.WithSpaces message) {
    this.message = requireNonNull(message, "message must not be null");
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@link Type#MESSAGE Type#MESSAGE}
   */
  @Override
  public @NotNull Type getType() {
    return Type.MESSAGE;
  }


  /**
   * Returns the message with spaces.
   *
   * @return  message with spaces, never {@code null}
   */
  @Override
  public @NotNull Message.WithSpaces asObject() {
    return message;
  }


  @Override
  public boolean equals(Object o)
  {
    return this == o ||
        o instanceof ConfigValueMessage && message.equals(((ConfigValueMessage)o).message);
  }


  @Override
  public int hashCode() {
    return 59 + message.hashCode();
  }


  @Override
  public String toString() {
    return message.toString();
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    PackHelper.pack(message, packStream);
  }


  /**
   * @param unpack      unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked message map value, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull ConfigValueMessage unpack(@NotNull PackHelper unpack,
                                                   @NotNull PackInputStream packStream)
      throws IOException {
    return new ConfigValueMessage(unpack.unpackMessageWithSpaces(packStream));
  }
}
