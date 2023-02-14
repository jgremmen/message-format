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
import de.sayayi.lib.message.internal.part.MessagePart;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.ParameterPart;
import de.sayayi.lib.message.pack.Pack;
import de.sayayi.lib.message.pack.Unpack;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * @author Jeroen Gremmen
 */
@ToString
@EqualsAndHashCode(doNotUseGetters = true)
public class ParameterizedMessage implements Message.WithSpaces
{
  public static final byte PACK_ID = 5;

  private static final long serialVersionUID = 800L;

  private final MessagePart[] parts;


  public ParameterizedMessage(@NotNull List<MessagePart> parts)
  {
    findParameter: {
      for(final MessagePart part: parts)
        if (part instanceof ParameterPart)
          break findParameter;

      throw new IllegalArgumentException("parts must contain at least 1 parameter part");
    }

    this.parts = parts.toArray(new MessagePart[0]);
  }


  @Override
  @Contract(pure = true)
  public @NotNull String format(@NotNull MessageContext messageContext, @NotNull Parameters parameters)
  {
    final StringBuilder message = new StringBuilder();
    boolean spaceBefore = false;

    for(final MessagePart part: parts)
    {
      final Text textPart = part instanceof ParameterPart
          ? ((ParameterPart)part).getText(messageContext, parameters)
          : (Text)part;

      if (!textPart.isEmpty())
      {
        if ((spaceBefore || textPart.isSpaceBefore()) && message.length() > 0)
          message.append(' ');

        message.append(textPart.getText());
        spaceBefore = textPart.isSpaceAfter();
      }
    }

    return message.toString();
  }


  @Override
  @Contract(value = "-> true", pure = true)
  public boolean hasParameters() {
    return true;
  }


  @Override
  public @NotNull SortedSet<String> getParameterNames()
  {
    final SortedSet<String> parameterNames = new TreeSet<>();

    for(final MessagePart part: parts)
      if (part instanceof ParameterPart)
        parameterNames.addAll(((ParameterPart)part).getParameterNames());

    return parameterNames;
  }


  @Override
  public boolean isSpaceBefore() {
    return parts[0].isSpaceBefore();
  }


  @Override
  public boolean isSpaceAfter() {
    return parts[parts.length - 1].isSpaceAfter();
  }


  /**
   * @param dataOutput  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull DataOutput dataOutput) throws IOException
  {
    dataOutput.writeByte(PACK_ID);
    dataOutput.writeByte(parts.length);

    for(final MessagePart part: parts)
      Pack.pack(part, dataOutput);
  }


  /**
   * @param unpack     unpacker instance, not {@code null}
   * @param dataInput  source data input, not {@code null}
   *
   * @return  unpacked parameterized message, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull Message.WithSpaces unpack(@NotNull Unpack unpack, @NotNull DataInput dataInput)
      throws IOException
  {
    final List<MessagePart> parts = new ArrayList<>();

    for(int n = 0, l = dataInput.readUnsignedByte(); n < l; n++)
      parts.add(unpack.loadMessagePart(dataInput));

    return new ParameterizedMessage(parts);
  }
}
