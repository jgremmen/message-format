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
package de.sayayi.lib.message.part;

import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static de.sayayi.lib.message.util.SpacesUtil.trimSpaces;
import static java.util.Objects.requireNonNull;


/**
 * Text message part without leading and trailing spaces.
 *
 * @author Jeroen Gremmen
 */
public final class NoSpaceTextPart implements Text
{
  private static final long serialVersionUID = 800L;

  private final @NotNull String text;


  /**
   * Constructs a text part without leading/trailing spaces. If {@code text} contains leading and/or
   * trailing spaces, they will be removed.
   *
   * @param text  text, not {@code null}
   */
  public NoSpaceTextPart(@NotNull String text) {
    this.text = trimSpaces(requireNonNull(text, "text must not be null"));
  }


  @Override
  public @NotNull String getText() {
    return text;
  }


  @Override
  public boolean isEmpty() {
    return text.isEmpty();
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@code false}
   */
  @Override
  public boolean isSpaceBefore() {
    return false;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@code false}
   */
  @Override
  public boolean isSpaceAfter() {
    return false;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@code false}
   */
  @Override
  public boolean isSpaceAround() {
    return false;
  }


  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    if (!(o instanceof Text))
      return false;

    final Text that = (Text)o;

    return !that.isSpaceBefore() && !that.isSpaceAfter() && text.equals(that.getText());
  }


  @Override
  public int hashCode() {
    return isEmpty() ? 0 : text.hashCode();
  }


  @Override
  @Contract(pure = true)
  public String toString() {
    return "Text('" + text.replace("'", "\\'") + "')";
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    packStream.writeString(text);
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked no-space text part, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull Text unpack(@NotNull PackInputStream packStream) throws IOException
  {
    final String text = requireNonNull(packStream.readString());
    return text.isEmpty() ? Text.EMPTY : new NoSpaceTextPart(text);
  }
}
