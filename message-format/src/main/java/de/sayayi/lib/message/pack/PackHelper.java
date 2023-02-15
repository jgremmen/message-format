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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.function.Function.identity;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@NoArgsConstructor
public final class PackHelper
{
  private static final int MAP_KEY_BOOL_ID = 0;
  private static final int MAP_KEY_EMPTY_ID = 1;
  private static final int MAP_KEY_NAME_ID = 2;
  private static final int MAP_KEY_NULL_ID = 3;
  private static final int MAP_KEY_NUMBER_ID = 4;
  private static final int MAP_KEY_STRING_ID = 5;

  private static final int MAP_VALUE_BOOL_ID = 0;
  private static final int MAP_VALUE_MESSAGE_ID = 1;
  private static final int MAP_VALUE_NUMBER_ID = 2;
  private static final int MAP_VALUE_STRING_ID = 3;

  private static final int PART_NO_SPACE_TEXT_ID = 0;
  private static final int PART_PARAMETER_ID = 1;
  private static final int PART_TEXT_ID = 2;

  private static final int MESSAGE_EMPTY = 0;
  private static final int MESSAGE_EMPTY_WITH_CODE = 1;
  private static final int MESSAGE_LOCALIZED_BUNDLE_WITH_CODE = 2;
  private static final int MESSAGE_DELEGATE_WITH_CODE = 3;
  private static final int MESSAGE_PARAMETERIZED = 4;
  private static final int MESSAGE_TEXT = 5;


  private final Map<MapKey,MapKey> mapKeys = new HashMap<>();
  private final Map<MapValue,MapValue> mapValues = new HashMap<>();
  private final Map<MessagePart,MessagePart> messageParts = new HashMap<>();
  private final Map<Message.WithSpaces,Message.WithSpaces> messagesWithSpaces = new HashMap<>();


  public static void pack(@NotNull Message message, @NotNull PackOutputStream packStream) throws IOException
  {
    if (message instanceof EmptyMessage)
      packStream.writeSmall(MESSAGE_EMPTY, 3);
    else if (message instanceof EmptyMessageWithCode)
    {
      packStream.writeSmall(MESSAGE_EMPTY_WITH_CODE, 3);
      ((EmptyMessageWithCode)message).pack(packStream);
    }
    else if (message instanceof LocalizedMessageBundleWithCode)
    {
      packStream.writeSmall(MESSAGE_LOCALIZED_BUNDLE_WITH_CODE, 3);
      ((LocalizedMessageBundleWithCode)message).pack(packStream);
    }
    else if (message instanceof MessageDelegateWithCode)
    {
      packStream.writeSmall(MESSAGE_DELEGATE_WITH_CODE, 3);
      ((MessageDelegateWithCode)message).pack(packStream);
    }
    else if (message instanceof ParameterizedMessage)
    {
      packStream.writeSmall(MESSAGE_PARAMETERIZED, 3);
      ((ParameterizedMessage)message).pack(packStream);
    }
    else if (message instanceof TextMessage)
    {
      packStream.writeSmall(MESSAGE_TEXT, 3);
      ((TextMessage)message).pack(packStream);
    }
    else
      throw new IllegalArgumentException();
  }


  public @NotNull Message.WithSpaces unpackMessageWithSpaces(@NotNull PackInputStream packStream)
      throws IOException
  {
    final Message.WithSpaces message;

    switch(packStream.readSmall(3))
    {
      case MESSAGE_EMPTY:
        message = EmptyMessage.unpack();
        break;

      case MESSAGE_PARAMETERIZED:
        message = ParameterizedMessage.unpack(this, packStream);
        break;

      case MESSAGE_TEXT:
        message = TextMessage.unpack(packStream);
        break;

      default:
        throw new IllegalStateException();
    }

    return messagesWithSpaces.computeIfAbsent(message, identity());
  }


  public @NotNull Message.WithCode unpackMessageWithCode(@NotNull PackInputStream packStream)
      throws IOException
  {
    switch(packStream.readSmall(3))
    {
      case MESSAGE_EMPTY_WITH_CODE:
        return EmptyMessageWithCode.unpack(packStream);

      case MESSAGE_LOCALIZED_BUNDLE_WITH_CODE:
        return LocalizedMessageBundleWithCode.unpack(this, packStream);

      case MESSAGE_DELEGATE_WITH_CODE:
        return MessageDelegateWithCode.unpack(this, packStream);
    }

    throw new IllegalStateException();
  }


  public @NotNull Message unpackMessage(@NotNull PackInputStream packStream) throws IOException
  {
    final Message.WithSpaces message;

    switch(packStream.readSmall(3))
    {
      case MESSAGE_EMPTY:
        message = EmptyMessage.unpack();
        break;

      case MESSAGE_EMPTY_WITH_CODE:
        return EmptyMessageWithCode.unpack(packStream);

      case MESSAGE_LOCALIZED_BUNDLE_WITH_CODE:
        return LocalizedMessageBundleWithCode.unpack(this, packStream);

      case MESSAGE_DELEGATE_WITH_CODE:
        return MessageDelegateWithCode.unpack(this, packStream);

      case MESSAGE_PARAMETERIZED:
        message = ParameterizedMessage.unpack(this, packStream);
        break;

      case MESSAGE_TEXT:
        message = TextMessage.unpack(packStream);
        break;

      default:
        throw new IllegalStateException();
    }

    return messagesWithSpaces.computeIfAbsent(message, identity());
  }


  public static void pack(@NotNull MessagePart messagePart, @NotNull PackOutputStream packStream)
      throws IOException
  {
    if (messagePart instanceof ParameterPart)
    {
      packStream.writeSmall(PART_PARAMETER_ID, 2);
      ((ParameterPart)messagePart).pack(packStream);
    }
    else if (messagePart instanceof NoSpaceTextPart)
    {
      packStream.writeSmall(PART_NO_SPACE_TEXT_ID, 2);
      ((NoSpaceTextPart)messagePart).pack(packStream);
    }
    else if (messagePart instanceof TextPart)
    {
      packStream.writeSmall(PART_TEXT_ID, 2);
      ((TextPart)messagePart).pack(packStream);
    }
    else
      throw new IllegalArgumentException();
  }


  public @NotNull MessagePart unpackMessagePart(@NotNull PackInputStream packStream) throws IOException
  {
    final MessagePart messagePart;

    switch(packStream.readSmall(2))
    {
      case PART_NO_SPACE_TEXT_ID:
        messagePart = NoSpaceTextPart.unpack(packStream);
        break;

      case PART_PARAMETER_ID:
        messagePart = ParameterPart.unpack(this, packStream);
        break;

      case PART_TEXT_ID:
        messagePart = TextPart.unpack(packStream);
        break;

      default:
        throw new IllegalStateException();
    }

    return messageParts.computeIfAbsent(messagePart, identity());
  }


  public static void pack(@NotNull MapKey mapKey, @NotNull PackOutputStream packStream) throws IOException
  {
    if (mapKey instanceof MapKeyBool)
    {
      packStream.writeSmall(MAP_KEY_BOOL_ID, 3);
      ((MapKeyBool)mapKey).pack(packStream);
    }
    if (mapKey instanceof MapKeyEmpty)
    {
      packStream.writeSmall(MAP_KEY_EMPTY_ID, 3);
      ((MapKeyEmpty)mapKey).pack(packStream);
    }
    else if (mapKey instanceof MapKeyName)
    {
      packStream.writeSmall(MAP_KEY_NAME_ID, 3);
      ((MapKeyName)mapKey).pack(packStream);
    }
    else if (mapKey instanceof MapKeyNull)
    {
      packStream.writeSmall(MAP_KEY_NULL_ID, 3);
      ((MapKeyNull)mapKey).pack(packStream);
    }
    else if (mapKey instanceof MapKeyNumber)
    {
      packStream.writeSmall(MAP_KEY_NUMBER_ID, 3);
      ((MapKeyNumber)mapKey).pack(packStream);
    }
    else if (mapKey instanceof MapKeyString)
    {
      packStream.writeSmall(MAP_KEY_STRING_ID, 3);
      ((MapKeyString)mapKey).pack(packStream);
    }
    else
      throw new IllegalArgumentException();
  }


  public @NotNull MapKey unpackMapKey(@NotNull PackInputStream packStream) throws IOException
  {
    final MapKey mapKey;

    switch(packStream.readSmall(3))
    {
      case MAP_KEY_BOOL_ID:
        mapKey = MapKeyBool.unpack(packStream);
        break;

      case MAP_KEY_EMPTY_ID:
        mapKey = MapKeyEmpty.unpack(packStream);
        break;

      case MAP_KEY_NAME_ID:
        mapKey = MapKeyName.unpack(packStream);
        break;

      case MAP_KEY_NULL_ID:
        mapKey = MapKeyNull.unpack(packStream);
        break;

      case MAP_KEY_NUMBER_ID:
        mapKey = MapKeyNumber.unpack(packStream);
        break;

      case MAP_KEY_STRING_ID:
        mapKey = MapKeyString.unpack(packStream);
        break;

      default:
        throw new IllegalStateException();
    }

    return mapKeys.computeIfAbsent(mapKey, identity());
  }


  public static void pack(@NotNull MapValue mapValue, @NotNull PackOutputStream packStream) throws IOException
  {
    if (mapValue instanceof MapValueBool)
    {
      packStream.writeSmall(MAP_VALUE_BOOL_ID, 2);
      ((MapValueBool)mapValue).pack(packStream);
    }
    if (mapValue instanceof MapValueMessage)
    {
      packStream.writeSmall(MAP_VALUE_MESSAGE_ID, 2);
      ((MapValueMessage)mapValue).pack(packStream);
    }
    else if (mapValue instanceof MapValueNumber)
    {
      packStream.writeSmall(MAP_VALUE_NUMBER_ID, 2);
      ((MapValueNumber)mapValue).pack(packStream);
    }
    else if (mapValue instanceof MapValueString)
    {
      packStream.writeSmall(MAP_VALUE_STRING_ID, 2);
      ((MapValueString)mapValue).pack(packStream);
    }
    else
      throw new IllegalArgumentException();
  }


  public @NotNull MapValue unpackMapValue(@NotNull PackInputStream packStream) throws IOException
  {
    final MapValue mapValue;

    switch(packStream.readSmall(2))
    {
      case MAP_VALUE_BOOL_ID:
        mapValue = MapValueBool.unpack(packStream);
        break;

      case MAP_VALUE_MESSAGE_ID:
        mapValue = MapValueMessage.unpack(this, packStream);
        break;

      case MAP_VALUE_NUMBER_ID:
        mapValue = MapValueNumber.unpack(packStream);
        break;

      case MAP_VALUE_STRING_ID:
        mapValue = MapValueString.unpack(packStream);
        break;

      default:
        throw new IllegalStateException();
    }

    return mapValues.computeIfAbsent(mapValue, identity());
  }
}
