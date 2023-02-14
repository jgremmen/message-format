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
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.function.Function.identity;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@NoArgsConstructor
public final class Unpack
{
  private final Map<MapKey,MapKey> mapKeys = new HashMap<>();
  private final Map<MapValue,MapValue> mapValues = new HashMap<>();
  private final Map<MessagePart,MessagePart> messageParts = new HashMap<>();
  private final Map<Message.WithSpaces,Message.WithSpaces> messagesWithSpaces = new HashMap<>();


  public @NotNull MapKey loadMapKey(@NotNull DataInput dataInput) throws IOException
  {
    final MapKey mapKey;

    switch(dataInput.readUnsignedByte())
    {
      case MapKeyBool.PACK_ID:
        mapKey = MapKeyBool.unpack(dataInput);
        break;

      case MapKeyEmpty.PACK_ID:
        mapKey = MapKeyEmpty.unpack(dataInput);
        break;

      case MapKeyName.PACK_ID:
        mapKey = MapKeyName.unpack(dataInput);
        break;

      case MapKeyNull.PACK_ID:
        mapKey = MapKeyNull.unpack(dataInput);
        break;

      case MapKeyNumber.PACK_ID:
        mapKey = MapKeyNumber.unpack(dataInput);
        break;

      case MapKeyString.PACK_ID:
        mapKey = MapKeyString.unpack(dataInput);
        break;

      default:
        throw new IllegalStateException();
    }

    return mapKeys.computeIfAbsent(mapKey, identity());
  }


  public @NotNull MapValue loadMapValue(@NotNull DataInput dataInput) throws IOException
  {
    final MapValue mapValue;

    switch(dataInput.readUnsignedByte())
    {
      case MapValueBool.PACK_ID:
        mapValue = MapValueBool.unpack(dataInput);
        break;

      case MapValueMessage.PACK_ID:
        mapValue = MapValueMessage.unpack(this, dataInput);
        break;

      case MapValueNumber.PACK_ID:
        mapValue = MapValueNumber.unpack(dataInput);
        break;

      case MapValueString.PACK_ID:
        mapValue = MapValueString.unpack(dataInput);
        break;

      default:
        throw new IllegalStateException();
    }

    return mapValues.computeIfAbsent(mapValue, identity());
  }


  public @NotNull MessagePart loadMessagePart(@NotNull DataInput dataInput) throws IOException
  {
    final MessagePart messagePart;

    switch(dataInput.readUnsignedByte())
    {
      case NoSpaceTextPart.PACK_ID:
        messagePart = NoSpaceTextPart.unpack(dataInput);
        break;

      case ParameterPart.PACK_ID:
        messagePart = ParameterPart.unpack(this, dataInput);
        break;

      case TextPart.PACK_ID:
        messagePart = TextPart.unpack(dataInput);
        break;

      default:
        throw new IllegalStateException();
    }

    return messageParts.computeIfAbsent(messagePart, identity());
  }


  public @NotNull Message.WithSpaces loadMessageWithSpaces(@NotNull DataInput dataInput) throws IOException
  {
    final Message.WithSpaces message;

    switch(dataInput.readUnsignedByte())
    {
      case EmptyMessage.PACK_ID:
        message = EmptyMessage.unpack();
        break;

      case ParameterizedMessage.PACK_ID:
        message = ParameterizedMessage.unpack(this, dataInput);
        break;

      case TextMessage.PACK_ID:
        message = TextMessage.unpack(dataInput);
        break;

      default:
        throw new IllegalStateException();
    }

    return messagesWithSpaces.computeIfAbsent(message, identity());
  }


  public @NotNull Message.WithCode loadMessageWithCode(@NotNull DataInput dataInput) throws IOException
  {
    switch(dataInput.readUnsignedByte())
    {
      case EmptyMessageWithCode.PACK_ID:
        return EmptyMessageWithCode.unpack(dataInput);

      case LocalizedMessageBundleWithCode.PACK_ID:
        return LocalizedMessageBundleWithCode.unpack(this, dataInput);

      case MessageDelegateWithCode.PACK_ID:
        return MessageDelegateWithCode.unpack(this, dataInput);
    }

    throw new IllegalStateException();
  }


  public @NotNull Message loadMessage(@NotNull DataInput dataInput) throws IOException
  {
    final Message.WithSpaces message;

    switch(dataInput.readUnsignedByte())
    {
      case EmptyMessage.PACK_ID:
        message = EmptyMessage.unpack();
        break;

      case EmptyMessageWithCode.PACK_ID:
        return EmptyMessageWithCode.unpack(dataInput);

      case LocalizedMessageBundleWithCode.PACK_ID:
        return LocalizedMessageBundleWithCode.unpack(this, dataInput);

      case MessageDelegateWithCode.PACK_ID:
        return MessageDelegateWithCode.unpack(this, dataInput);

      case ParameterizedMessage.PACK_ID:
        message = ParameterizedMessage.unpack(this, dataInput);
        break;

      case TextMessage.PACK_ID:
        message = TextMessage.unpack(dataInput);
        break;

      default:
        throw new IllegalStateException();
    }

    return messagesWithSpaces.computeIfAbsent(message, identity());
  }
}
