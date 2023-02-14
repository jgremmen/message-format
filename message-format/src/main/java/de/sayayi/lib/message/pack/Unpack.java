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

    switch((int)dataInput.readByte() & 0xff)
    {
      case 1: mapKey = MapKeyBool.unpack(dataInput); break;
      case 2: mapKey = MapKeyEmpty.unpack(dataInput); break;
      case 3: mapKey = MapKeyName.unpack(dataInput); break;
      case 4: mapKey = MapKeyNull.unpack(dataInput); break;
      case 5: mapKey = MapKeyNumber.unpack(dataInput); break;
      case 6: mapKey = MapKeyString.unpack(dataInput); break;

      default:
        throw new IllegalStateException();
    }

    return mapKeys.computeIfAbsent(mapKey, identity());
  }


  public @NotNull MapValue loadMapValue(@NotNull DataInput dataInput) throws IOException
  {
    final MapValue mapValue;

    switch((int)dataInput.readByte() & 0xff)
    {
      case 1: mapValue = MapValueBool.unpack(dataInput); break;
      case 2: mapValue = MapValueMessage.unpack(this, dataInput); break;
      case 3: mapValue = MapValueNumber.unpack(dataInput); break;
      case 4: mapValue = MapValueString.unpack(dataInput); break;

      default:
        throw new IllegalStateException();
    }

    return mapValues.computeIfAbsent(mapValue, identity());
  }


  public @NotNull MessagePart loadMessagePart(@NotNull DataInput dataInput) throws IOException
  {
    final MessagePart messagePart;

    switch((int)dataInput.readByte() & 0xff)
    {
      case 1: messagePart = NoSpaceTextPart.unpack(dataInput); break;
      case 2: messagePart = ParameterPart.unpack(this, dataInput); break;
      case 3: messagePart = TextPart.unpack(dataInput); break;

      default:
        throw new IllegalStateException();
    }

    return messageParts.computeIfAbsent(messagePart, identity());
  }


  public @NotNull Message.WithSpaces loadMessageWithSpaces(@NotNull DataInput dataInput) throws IOException
  {
    final Message.WithSpaces message;

    switch((int)dataInput.readByte() & 0xff)
    {
      case 1: message = EmptyMessage.unpack(); break;
      case 5: message = ParameterizedMessage.unpack(this, dataInput); break;
      case 6: message = TextMessage.unpack(dataInput); break;

      default:
        throw new IllegalStateException();
    }

    return messagesWithSpaces.computeIfAbsent(message, identity());
  }


  public @NotNull Message.WithCode loadMessageWithCode(@NotNull DataInput dataInput) throws IOException
  {
    switch((int)dataInput.readByte() & 0xff)
    {
      case 2: return EmptyMessageWithCode.unpack(dataInput);
      case 3: return LocalizedMessageBundleWithCode.unpack(this, dataInput);
      case 4: return MessageDelegateWithCode.unpack(this, dataInput);
    }

    throw new IllegalStateException();
  }


  public @NotNull Message loadMessage(@NotNull DataInput dataInput) throws IOException
  {
    final Message.WithSpaces message;

    switch((int)dataInput.readByte() & 0xff)
    {
      case 1: message = EmptyMessage.unpack(); break;
      case 2: return EmptyMessageWithCode.unpack(dataInput);
      case 3: return LocalizedMessageBundleWithCode.unpack(this, dataInput);
      case 4: return MessageDelegateWithCode.unpack(this, dataInput);
      case 5: message = ParameterizedMessage.unpack(this, dataInput); break;
      case 6: message = TextMessage.unpack(dataInput); break;

      default:
        throw new IllegalStateException();
    }

    return messagesWithSpaces.computeIfAbsent(message, identity());
  }
}
