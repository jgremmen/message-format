/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.pack;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.data.map.*;
import de.sayayi.lib.message.internal.*;
import de.sayayi.lib.message.internal.part.MessagePart;
import de.sayayi.lib.message.internal.part.NoSpaceTextPart;
import de.sayayi.lib.message.internal.part.ParameterPart;
import de.sayayi.lib.message.internal.part.TextPart;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.IOException;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class Unpack
{
  public static @NotNull MapKey loadMapKey(@NotNull DataInput dataInput) throws IOException
  {
    switch((int)dataInput.readByte() & 0xff)
    {
      case 1: return MapKeyBool.unpack(dataInput);
      case 2: return MapKeyEmpty.unpack(dataInput);
      case 3: return MapKeyName.unpack(dataInput);
      case 4: return MapKeyNull.unpack(dataInput);
      case 5: return MapKeyNumber.unpack(dataInput);
      case 6: return MapKeyString.unpack(dataInput);
    }

    throw new IllegalStateException();
  }


  public static @NotNull MapValue loadMapValue(@NotNull DataInput dataInput) throws IOException
  {
    switch((int)dataInput.readByte() & 0xff)
    {
      case 1: return MapValueBool.unpack(dataInput);
      case 2: return MapValueMessage.unpack(dataInput);
      case 3: return MapValueNumber.unpack(dataInput);
      case 4: return MapValueString.unpack(dataInput);
    }

    throw new IllegalStateException();
  }


  public static @NotNull MessagePart loadMessagePart(@NotNull DataInput dataInput) throws IOException
  {
    switch((int)dataInput.readByte() & 0xff)
    {
      case 1: return NoSpaceTextPart.unpack(dataInput);
      case 2: return ParameterPart.unpack(dataInput);
      case 3: return TextPart.unpack(dataInput);
    }

    throw new IllegalStateException();
  }


  public static @NotNull Message.WithSpaces loadMessageWithSpaces(@NotNull DataInput dataInput) throws IOException
  {
    switch((int)dataInput.readByte() & 0xff)
    {
      case 1: return EmptyMessage.unpack();
      case 5: return ParameterizedMessage.unpack(dataInput);
      case 6: return TextMessage.unpack(dataInput);
    }

    throw new IllegalStateException();
  }


  public static @NotNull Message.WithCode loadMessageWithCode(@NotNull DataInput dataInput) throws IOException
  {
    switch((int)dataInput.readByte() & 0xff)
    {
      case 2: return EmptyMessageWithCode.unpack(dataInput);
      case 3: return LocalizedMessageBundleWithCode.unpack(dataInput);
      case 4: return MessageDelegateWithCode.unpack(dataInput);
    }

    throw new IllegalStateException();
  }


  public static @NotNull Message loadMessage(@NotNull DataInput dataInput) throws IOException
  {
    switch((int)dataInput.readByte() & 0xff)
    {
      case 1: return EmptyMessage.unpack();
      case 2: return EmptyMessageWithCode.unpack(dataInput);
      case 3: return LocalizedMessageBundleWithCode.unpack(dataInput);
      case 4: return MessageDelegateWithCode.unpack(dataInput);
      case 5: return ParameterizedMessage.unpack(dataInput);
      case 6: return TextMessage.unpack(dataInput);
    }

    throw new IllegalStateException();
  }
}
