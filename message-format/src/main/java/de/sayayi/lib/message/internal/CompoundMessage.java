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
import de.sayayi.lib.message.SpacesAware;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.message.internal.part.TemplatePart;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextJoiner;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.*;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;


/**
 * A compound message is represented by a concatenation of message parts.
 * <p>
 * During formatting each part is rendered and concatenated. Spaces between rendered parts are
 * inserted based on {@link SpacesAware#isSpaceBefore()} and {@link SpacesAware#isSpaceAfter()}.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class CompoundMessage implements Message.WithSpaces
{
  /** Message parts, not empty */
  private final @NotNull MessagePart[] messageParts;


  /**
   * Construct a compound message based on the given message {@code parts}.
   * <p>
   * At least 1 message part is required. If the sole message part is a
   * {@link TextPart TextPart}, it is better to use
   * {@link EmptyMessage} or {@link TextMessage} in that case.
   *
   * @param messageParts  message parts, not {@code null} and not empty
   */
  public CompoundMessage(@NotNull List<MessagePart> messageParts)
  {
    if (requireNonNull(messageParts, "parts must not be null").isEmpty())
      throw new IllegalArgumentException("parts must not be empty");

    this.messageParts = messageParts.toArray(MessagePart[]::new);
  }


  @Override
  public @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters)
      throws MessageFormatException
  {
    var message = new TextJoiner();

    for(var messagePart: messageParts)
      message.add(format(messageAccessor, parameters, messagePart));

    return message.asSpacedText();
  }


  @Contract(pure = true)
  private @NotNull Text format(@NotNull MessageAccessor messageAccessor,
                               @NotNull Parameters parameters,
                               @NotNull MessagePart messagePart)
  {
    if (messagePart instanceof ParameterPart)
    {
      try {
        return ((ParameterPart)messagePart).getText(messageAccessor, parameters);
      } catch(Exception ex) {
        throw MessageFormatException.of(ex).withParameter(((ParameterPart)messagePart).getName());
      }
    }

    if (messagePart instanceof TemplatePart)
    {
      try {
        return ((TemplatePart)messagePart).getText(messageAccessor, parameters);
      } catch(Exception ex) {
        throw MessageFormatException.of(ex).withTemplate(((TemplatePart)messagePart).getName());
      }
    }

    return (Text)messagePart;
  }


  @Override
  public boolean isSpaceBefore() {
    return messageParts[0].isSpaceBefore();
  }


  @Override
  public boolean isSpaceAfter() {
    return messageParts[messageParts.length - 1].isSpaceAfter();
  }


  @Override
  public @NotNull MessagePart[] getMessageParts() {
    return copyOf(messageParts, messageParts.length);
  }


  @Override
  @Unmodifiable
  public @NotNull Set<String> getTemplateNames()
  {
    var templateNames = new TreeSet<String>();

    for(var messagePart: messageParts)
    {
      if (messagePart instanceof TemplatePart)
        templateNames.add(((TemplatePart)messagePart).getName());
      else if (messagePart instanceof ParameterPart)
        templateNames.addAll(((ParameterPart)messagePart).getParamConfig().getTemplateNames());
    }

    return unmodifiableSet(templateNames);
  }


  @Override
  public boolean equals(Object o) {
    return o instanceof CompoundMessage && deepEquals(messageParts, ((CompoundMessage)o).messageParts);
  }


  @Override
  public int hashCode() {
    return 59 + deepHashCode(messageParts);
  }


  @Override
  public String toString() {
    return "CompoundMessage(parts=" + deepToString(messageParts) + ')';
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
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeSmallVar(messageParts.length);

    for(var part: messageParts)
      PackSupport.pack(part, packStream);
  }


  /**
   * @param unpack      unpacker instance, not {@code null}
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked parameterized message, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   *
   * @hidden
   */
  public static @NotNull Message.WithSpaces unpack(@NotNull PackSupport unpack, @NotNull PackInputStream packStream)
      throws IOException
  {
    var parts = new ArrayList<MessagePart>();

    for(int n = 0, l = packStream.readSmallVar(); n < l; n++)
      parts.add(unpack.unpackMessagePart(packStream));

    return new CompoundMessage(parts);
  }
}
