/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.internal.part;

import de.sayayi.lib.message.internal.part.MessagePart.Text;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static de.sayayi.lib.message.internal.SpacesUtil.trimSpaces;


/**
 * Text message part without leading and trailing spaces.
 *
 * @author Jeroen Gremmen
 */
public final class NoSpaceTextPart implements Text
{
  public static final byte PACK_ID = 1;

  private static final long serialVersionUID = 800L;

  @Getter private final @NotNull String text;


  public NoSpaceTextPart(@NotNull String text) {
    this.text = trimSpaces(text);
  }


  @Override
  public boolean isEmpty() {
    return text.isEmpty();
  }


  @Override
  public boolean isSpaceBefore() {
    return false;
  }


  @Override
  public boolean isSpaceAfter() {
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
    return "Text(text=" + text + ')';
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
    dataOutput.writeByte(PACK_ID);
    dataOutput.writeUTF(text);
  }


  /**
   * @param dataInput  source data input, not {@code null}
   *
   * @return  unpacked no-space text part, never {@code null}
   *
   * @throws IOException  if an I/O error occurs.
   *
   * @since 0.8.0
   */
  public static @NotNull Text unpack(@NotNull DataInput dataInput) throws IOException
  {
    final String text = dataInput.readUTF();
    return text.isEmpty() ? Text.EMPTY : new NoSpaceTextPart(text);
  }
}
