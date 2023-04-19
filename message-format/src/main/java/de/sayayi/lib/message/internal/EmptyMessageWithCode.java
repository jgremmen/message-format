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
import de.sayayi.lib.message.MessageSupport.MessageSupportAccessor;
import de.sayayi.lib.message.internal.part.MessagePart;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
public final class EmptyMessageWithCode extends AbstractMessageWithCode
{
  private static final long serialVersionUID = 800L;


  /**
   * Constructs an empty message with {@code code}.
   *
   * @param code  message code, not {@code null}
   *
   * @throws IllegalArgumentException  if message code is empty
   */
  public EmptyMessageWithCode(@NotNull String code) {
    super(code);
  }


  @Override
  @Contract(pure = true)
  public @NotNull String format(@NotNull MessageSupportAccessor messageSupport,
                                @NotNull Parameters parameters) {
    return "";
  }


  @Override
  public @NotNull MessagePart[] getMessageParts() {
    return new MessagePart[] { TextPart.EMPTY };
  }


  @Override
  public @NotNull Set<String> getTemplateNames() {
    return emptySet();
  }


  @Override
  public boolean isSame(@NotNull Message message)
  {
    return !(message instanceof LocaleAware) &&
        Arrays.equals(getMessageParts(), message.getMessageParts());
  }


  @Override
  public boolean equals(Object o)
  {
    return this == o ||
        o instanceof EmptyMessageWithCode && code.equals(((EmptyMessageWithCode)o).code);
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
   */
  public static @NotNull Message.WithCode unpack(@NotNull PackInputStream packStream)
      throws IOException {
    return new EmptyMessageWithCode(requireNonNull(packStream.readString()));
  }
}
