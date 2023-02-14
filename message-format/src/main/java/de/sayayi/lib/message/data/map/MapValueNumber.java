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

import de.sayayi.lib.message.data.DataNumber;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * @author Jeroen Gremmen
 */
@EqualsAndHashCode(doNotUseGetters = true, callSuper = true)
public final class MapValueNumber extends DataNumber implements MapValue
{
  public static final byte PACK_ID = 3;

  private static final long serialVersionUID = 800L;


  public MapValueNumber(long number) {
    super(number);
  }


  @Override
  public @NotNull Type getType() {
    return Type.NUMBER;
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
    dataOutput.writeLong(asObject());
  }


  /**
   * @param dataInput  source data input, not {@code null}
   *
   * @return  unpacked number map value, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull MapValueNumber unpack(@NotNull DataInput dataInput) throws IOException {
    return new MapValueNumber(dataInput.readLong());
  }
}
