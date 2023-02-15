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
package de.sayayi.lib.message.data.map;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


/**
 * @author Jeroen Gremmen
 */
public final class MapValueString extends DataString implements MapValue
{
  public static final byte PACK_ID = 3;

  private static final long serialVersionUID = 800L;

  private Message.WithSpaces message;


  public MapValueString(@NotNull String string) {
    super(string);
  }


  @Override
  public @NotNull Type getType() {
    return Type.STRING;
  }


  @NotNull
  public synchronized Message.WithSpaces asMessage(@NotNull MessageFactory messageFactory)
  {
    if (message == null)
      message = messageFactory.parse(asObject());

    return message;
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
    packStream.writeSmall(PACK_ID, 2);
    packStream.writeString(asObject());
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked string map value, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull MapValueString unpack(@NotNull PackInputStream packStream) throws IOException {
    return new MapValueString(packStream.readString());
  }
}
