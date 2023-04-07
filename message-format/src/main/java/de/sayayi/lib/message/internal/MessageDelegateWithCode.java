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
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class MessageDelegateWithCode extends AbstractMessageWithCode
{
  private static final long serialVersionUID = 800L;

  private final @NotNull Message message;


  public MessageDelegateWithCode(@NotNull String code, @NotNull Message message)
  {
    super(code);

    this.message = requireNonNull(message, "message must not be null");
  }


  /**
   * Returns the underlying message requests are delegated to.
   *
   * @return  underlying message, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull Message getMessage() {
    return message;
  }


  @Override
  @Contract(pure = true)
  public @NotNull String format(@NotNull MessageSupportAccessor messageSupport,
                                @NotNull Parameters parameters) {
    return message.format(messageSupport, parameters);
  }


  @Override
  public @NotNull Set<String> getTemplateNames() {
    return message.getTemplateNames();
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeString(getCode());
    PackHelper.pack(message, packStream);
  }


  /**
   * @param unpack     unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked message delegate with code, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull Message.WithCode unpack(@NotNull PackHelper unpack,
                                                 @NotNull PackInputStream packStream)
      throws IOException {
    return new MessageDelegateWithCode(requireNonNull(packStream.readString()), unpack.unpackMessage(packStream));
  }
}
