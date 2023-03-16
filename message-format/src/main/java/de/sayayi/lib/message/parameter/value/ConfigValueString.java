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
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
@EqualsAndHashCode(doNotUseGetters = true)
public final class ConfigValueString implements ConfigValue
{
  private static final long serialVersionUID = 800L;

  private final String string;
  private Message.WithSpaces message;


  public ConfigValueString(@NotNull String string) {
    this.string = string;
  }


  @Override
  public @NotNull Type getType() {
    return Type.STRING;
  }


  /**
   * Return the string value.
   *
   * @return  string, never {@code null}
   *
   * @since 0.8.0
   */
  public @NotNull String stringValue() {
    return string;
  }


  /**
   * Returns the string value.
   *
   * @return  string, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull String asObject() {
    return string;
  }


  @NotNull
  public synchronized Message.WithSpaces asMessage(@NotNull MessageFactory messageFactory)
  {
    if (message == null)
      message = messageFactory.parseMessage(string);

    return message;
  }


  @Override
  @Contract(pure = true)
  public @NotNull String toString() {
    return string;
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    packStream.writeString(asObject());
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked string map value, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull ConfigValueString unpack(@NotNull PackInputStream packStream) throws IOException {
    return new ConfigValueString(requireNonNull(packStream.readString()));
  }
}
