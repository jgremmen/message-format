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
package de.sayayi.lib.message.data.map;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.pack.Pack;
import de.sayayi.lib.message.pack.Unpack;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
public final class MapValueMessage implements MapValue
{
  private static final long serialVersionUID = 800L;

  private final @NotNull Message.WithSpaces message;


  @Override
  public @NotNull Type getType() {
    return Type.MESSAGE;
  }


  @Override
  public @NotNull Message.WithSpaces asObject() {
    return message;
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
    dataOutput.writeByte(2);
    Pack.pack(message, dataOutput);
  }


  /**
   * @param dataInput  source data input, not {@code null}
   *
   * @return  unpacked message map value, never {@code null}
   *
   * @throws IOException  if an I/O error occurs.
   *
   * @since 0.8.0
   */
  public static @NotNull MapValueMessage unpack(@NotNull DataInput dataInput) throws IOException {
    return new MapValueMessage(Unpack.loadMessageWithSpaces(dataInput));
  }
}
