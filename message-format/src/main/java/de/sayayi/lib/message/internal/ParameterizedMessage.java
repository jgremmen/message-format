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
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.ParameterPart;
import de.sayayi.lib.message.internal.part.TemplatePart;
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.*;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;


/**
 * @author Jeroen Gremmen
 */
public class ParameterizedMessage implements Message.WithSpaces
{
  private static final long serialVersionUID = 800L;

  private final MessagePart[] parts;


  public ParameterizedMessage(@NotNull List<MessagePart> parts)
  {
    findParameter: {
      for(final MessagePart part: requireNonNull(parts, "parts must not be null"))
        if (part instanceof ParameterPart)
          break findParameter;

      throw new IllegalArgumentException("parts must contain at least 1 parameter part");
    }

    this.parts = parts.toArray(new MessagePart[0]);
  }


  @Override
  @Contract(pure = true)
  public @NotNull String format(@NotNull MessageSupportAccessor messageSupport,
                                @NotNull Parameters parameters)
  {
    final StringBuilder message = new StringBuilder();
    boolean spaceBefore = false;

    for(final MessagePart part: parts)
    {
      final Text textPart = part instanceof ParameterPart
          ? ((ParameterPart)part).getText(messageSupport, parameters)
          : part instanceof TemplatePart
          ? ((TemplatePart)part).getText(messageSupport, parameters)
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
  public boolean isSpaceBefore() {
    return parts[0].isSpaceBefore();
  }


  @Override
  public boolean isSpaceAfter() {
    return parts[parts.length - 1].isSpaceAfter();
  }


  @Override
  public @NotNull Set<String> getTemplateNames()
  {
    return unmodifiableSet(Arrays
        .stream(parts)
        .filter(p -> p instanceof TemplatePart)
        .map(p -> ((TemplatePart)p).getName())
        .collect(toSet()));
  }


  @Override
  public boolean equals(Object o)
  {
    return this == o ||
        o instanceof ParameterizedMessage && deepEquals(parts, ((ParameterizedMessage)o).parts);
  }


  @Override
  public int hashCode() {
    return 59 + deepHashCode(parts);
  }


  @Override
  public String toString() {
    return "ParameterizedMessage(parts=" + deepToString(parts) + ')';
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
    packStream.writeSmallVar(parts.length);

    for(final MessagePart part: parts)
      PackHelper.pack(part, packStream);
  }


  /**
   * @param unpack     unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked parameterized message, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull Message.WithSpaces unpack(@NotNull PackHelper unpack,
                                                   @NotNull PackInputStream packStream)
      throws IOException
  {
    final List<MessagePart> parts = new ArrayList<>();

    for(int n = 0, l = packStream.readSmallVar(); n < l; n++)
      parts.add(unpack.unpackMessagePart(packStream));

    return new ParameterizedMessage(parts);
  }
}
