/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.part;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.part.MessagePart.Template;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static de.sayayi.lib.message.part.TextPartFactory.*;
import static java.util.Objects.requireNonNull;


/**
 * Text message part with optional leading and/or trailing spaces.
 *
 * @author Jeroen Gremmen
 *
 * @since 0.8.0
 */
public final class TemplatePart implements Template
{
  private static final long serialVersionUID = 800L;

  /** template name. */
  private final @NotNull String name;

  /** tells whether the template has a leading space. */
  private final boolean spaceBefore;

  /** tells whether the template has a trailing space. */
  private final boolean spaceAfter;


  /**
   * Constructs a template part.
   *
   * @param name         template name, not empty or {@code null}
   * @param spaceBefore  {@code true} if the part has a leading space,
   *                     {@code false} if the part has no leading space
   * @param spaceAfter   {@code true} if the part has a trailing space,
   *                     {@code false} if the part has no trailing space
   */
  public TemplatePart(@NotNull String name, boolean spaceBefore, boolean spaceAfter)
  {
    if ((this.name = requireNonNull(name, "name must not be null")).isEmpty())
      throw new IllegalArgumentException("name must not be empty");

    this.spaceBefore = spaceBefore;
    this.spaceAfter = spaceAfter;
  }


  @Override
  public @NotNull String getName() {
    return name;
  }


  @Override
  public boolean isSpaceBefore() {
    return spaceBefore;
  }


  @Override
  public boolean isSpaceAfter() {
    return spaceAfter;
  }


  @Override
  public boolean isSpaceAround() {
    return spaceBefore && spaceAfter;
  }


  @Override
  public @NotNull Text getText(@NotNull MessageAccessor messageAccessor,
                               @NotNull Parameters parameters)
  {
    final Message message = messageAccessor.getTemplateByName(name);

    return addSpaces(message != null
        ? noSpaceText(message.format(messageAccessor, parameters))
        : emptyText(), spaceBefore, spaceAfter);
  }


  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    if (!(o instanceof Template))
      return false;

    final Template that = (Template)o;

    return spaceBefore == that.isSpaceBefore() &&
           spaceAfter == that.isSpaceAfter() &&
           name.equals(that.getName());
  }


  @Override
  public int hashCode() {
    return name.hashCode() * 11 + (spaceBefore ? 8 : 0) + (spaceAfter ? 2 : 0);
  }


  @Override
  @Contract(pure = true)
  public String toString()
  {
    final StringBuilder s = new StringBuilder("Template(name=").append(name);

    if (spaceBefore && spaceAfter)
      s.append(",space-around");
    else if (spaceBefore)
      s.append(",space-before");
    else if (spaceAfter)
      s.append(",space-after");

    return s.append(')').toString();
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeBoolean(spaceBefore);
    packStream.writeBoolean(spaceAfter);
    packStream.writeString(name);
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked template part, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   */
  public static @NotNull Template unpack(@NotNull PackInputStream packStream) throws IOException
  {
    final boolean spaceBefore = packStream.readBoolean();
    final boolean spaceAfter = packStream.readBoolean();

    return new TemplatePart(requireNonNull(packStream.readString()), spaceBefore, spaceAfter);
  }
}