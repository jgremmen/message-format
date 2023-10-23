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
import de.sayayi.lib.message.pack.PackHelper;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TemplatePart;
import de.sayayi.lib.message.part.TextPart;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.*;

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
  private static final long serialVersionUID = 800L;

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

    this.messageParts = messageParts.toArray(new MessagePart[0]);
  }


  @Override
  @Contract(pure = true)
  public @NotNull String format(@NotNull MessageAccessor messageAccessor,
                                @NotNull Parameters parameters)
  {
    final TextJoiner message = new TextJoiner();

    for(MessagePart messagePart: messageParts)
      message.add(format(messageAccessor, parameters, messagePart));

    return message.asNoSpaceText().getText();
  }


  @Contract(pure = true)
  private @NotNull Text format(@NotNull MessageAccessor messageAccessor,
                               @NotNull Parameters parameters,
                               @NotNull MessagePart messagePart)
  {
    if (messagePart instanceof ParameterPart)
    {
      final ParameterPart parameterPart = (ParameterPart)messagePart;

      try {
        return parameterPart.getText(messageAccessor, parameters);
      } catch(MessageFormatException ex) {
        throw ex.withParameter(parameterPart.getName());
      } catch(Exception ex) {
        throw new MessageFormatException(ex).withParameter(parameterPart.getName());
      }
    }

    if (messagePart instanceof TemplatePart)
    {
      final TemplatePart templatePart = (TemplatePart)messagePart;

      try {
        return templatePart.getText(messageAccessor, parameters);
      } catch(MessageFormatException ex) {
        throw ex.withTemplate(templatePart.getName());
      } catch(Exception ex) {
        throw new MessageFormatException(ex).withTemplate(templatePart.getName());
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
  public @NotNull Set<String> getTemplateNames()
  {
    final Set<String> templateNames = new TreeSet<>();

    for(final MessagePart messagePart: messageParts)
      if (messagePart instanceof TemplatePart)
        templateNames.add(((TemplatePart)messagePart).getName());
      else if (messagePart instanceof ParameterPart)
        templateNames.addAll(((ParameterPart)messagePart).getParamConfig().getTemplateNames());

    return unmodifiableSet(templateNames);
  }


  @Override
  public boolean isSame(@NotNull Message message)
  {
    if (message instanceof MessageDelegateWithCode)
      message = ((MessageDelegateWithCode)message).getMessage();

    return !(message instanceof LocaleAware) &&
        Arrays.equals(getMessageParts(), message.getMessageParts());
  }


  @Override
  public boolean equals(Object o)
  {
    return this == o ||
        o instanceof CompoundMessage && deepEquals(messageParts, ((CompoundMessage)o).messageParts);
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
  @SuppressWarnings("JavadocDeclaration")
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeSmallVar(messageParts.length);

    for(final MessagePart part: messageParts)
      PackHelper.pack(part, packStream);
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
  @SuppressWarnings("JavadocDeclaration")
  public static @NotNull Message.WithSpaces unpack(@NotNull PackHelper unpack,
                                                   @NotNull PackInputStream packStream)
      throws IOException
  {
    final List<MessagePart> parts = new ArrayList<>();

    for(int n = 0, l = packStream.readSmallVar(); n < l; n++)
      parts.add(unpack.unpackMessagePart(packStream));

    return new CompoundMessage(parts);
  }
}
