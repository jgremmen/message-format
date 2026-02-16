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
package de.sayayi.lib.message.internal.part.typedvalue;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.part.TypedValue.MessageValue;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static java.util.Objects.requireNonNull;


/**
 * This class represents a message configuration value.
 *
 * @param messageValue  configuration value message.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public record TypedValueMessage(@NotNull Message.WithSpaces messageValue) implements MessageValue
{
  public TypedValueMessage(@NotNull Message.WithSpaces messageValue) {
    this.messageValue = requireNonNull(messageValue, "message must not be null");
  }


  /**
   * Returns the message with spaces.
   *
   * @return  message with spaces, never {@code null}
   */
  @Override
  public @NotNull Message.WithSpaces asObject() {
    return messageValue();
  }


  @Override
  public @NotNull String toString() {
    return messageValue.toString();
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    PackSupport.pack(messageValue, packStream);
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
  public static @NotNull TypedValueMessage unpack(@NotNull PackSupport unpack, @NotNull PackInputStream packStream)
      throws IOException {
    return new TypedValueMessage(unpack.unpackMessageWithSpaces(packStream));
  }
}
