/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.pack.Pack;
import de.sayayi.lib.message.pack.Unpack;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.SortedSet;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class MessageDelegateWithCode extends AbstractMessageWithCode
{
  private static final long serialVersionUID = 800L;

  @Getter private final @NotNull Message message;


  public MessageDelegateWithCode(@NotNull String code, @NotNull Message message)
  {
    super(code);

    this.message = message;
  }


  @Override
  @Contract(pure = true)
  public @NotNull String format(@NotNull MessageContext messageContext, @NotNull Parameters parameters) {
    return message.format(messageContext, parameters);
  }


  @Override
  @Contract(pure = true)
  public boolean hasParameters() {
    return message.hasParameters();
  }


  @Override
  public @NotNull SortedSet<String> getParameterNames() {
    return message.getParameterNames();
  }


  /**
   * @param dataOutput  data output pack target
   *
   * @throws IOException  if an I/O error occurs.
   *
   * @since 0.8.0
   */
  public void pack(@NotNull DataOutput dataOutput) throws IOException
  {
    dataOutput.writeByte(4);
    dataOutput.writeUTF(getCode());
    Pack.pack(message, dataOutput);
  }


  /**
   * @param dataInput  source data input, not {@code null}
   *
   * @return  unpacked message delegate with code, never {@code null}
   *
   * @throws IOException  if an I/O error occurs.
   *
   * @since 0.8.0
   */
  public static @NotNull Message.WithCode unpack(@NotNull DataInput dataInput) throws IOException
  {
    final String code = dataInput.readUTF();
    return new MessageDelegateWithCode(code, Unpack.loadMessage(dataInput));
  }
}
