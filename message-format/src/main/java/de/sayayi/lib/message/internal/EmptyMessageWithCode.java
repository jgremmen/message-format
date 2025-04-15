/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public final class EmptyMessageWithCode extends AbstractMessageWithCode
{
  /**
   * Constructs an empty message with {@code code}.
   *
   * @param code  message code, not {@code null} and not empty
   *
   * @throws IllegalArgumentException  if message code is empty
   */
  public EmptyMessageWithCode(@NotNull String code) {
    super(code);
  }


  @Override
  @Contract(pure = true)
  public @NotNull String format(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters) {
    return "";
  }


  @Override
  public @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException {
    return Text.EMPTY;
  }


  @Override
  public @NotNull MessagePart[] getMessageParts() {
    return new MessagePart[] { Text.EMPTY };
  }


  @Override
  public @NotNull Set<String> getTemplateNames() {
    return Set.of();
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof EmptyMessageWithCode && code.equals(((EmptyMessageWithCode)o).code);
  }


  @Override
  public String toString() {
    return "EmptyMessageWithCode(" + code + ')';
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
    packStream.writeString(getCode());
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked empty message with code, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  public static @NotNull Message.WithCode unpack(@NotNull PackInputStream packStream) throws IOException {
    return new EmptyMessageWithCode(requireNonNull(packStream.readString()));
  }
}
