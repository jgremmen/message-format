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
package de.sayayi.lib.message.internal.pack;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.internal.*;
import de.sayayi.lib.message.internal.part.NoSpaceTextPart;
import de.sayayi.lib.message.internal.part.TemplatePart;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import de.sayayi.lib.message.part.parameter.key.*;
import de.sayayi.lib.message.part.parameter.value.*;
import de.sayayi.lib.pack.PackConfig;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.function.Function.identity;


/**
 * This class provides methods for packing and unpacking message (and related) objects.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class PackSupport
{
  /** Pack version */
  public static final int VERSION = 2;

  /** Pack mime type */
  public static final String MIME_TYPE = "application/x-message-format-pack";

  public static final PackConfig PACK_CONFIG = new PackConfig
      .Builder()
      .withMagic("%{msg}")
      .withVersionRange(1, 100)
      .withCompressionSupport(true)
      .build();

  private static final int MAP_KEY_BOOL_ID = 0;
  private static final int MAP_KEY_EMPTY_ID = 1;
  private static final int MAP_KEY_NAME_ID = 2;
  private static final int MAP_KEY_NULL_ID = 3;
  private static final int MAP_KEY_NUMBER_ID = 4;
  private static final int MAP_KEY_STRING_ID = 5;
  private static final int MAP_KEY_DEFAULT_ID = 6;

  private static final int MAP_VALUE_BOOL_ID = 0;
  private static final int MAP_VALUE_MESSAGE_ID = 1;
  private static final int MAP_VALUE_NUMBER_ID = 2;
  private static final int MAP_VALUE_STRING_ID = 3;

  private static final int PART_NO_SPACE_TEXT_ID = 0;
  private static final int PART_PARAMETER_ID = 1;
  private static final int PART_TEXT_ID = 2;
  private static final int PART_TEMPLATE_ID = 3;

  private static final int MESSAGE_EMPTY = 0;
  private static final int MESSAGE_EMPTY_WITH_CODE = 1;
  private static final int MESSAGE_LOCALIZED_BUNDLE_WITH_CODE = 2;
  private static final int MESSAGE_DELEGATE_WITH_CODE = 3;
  private static final int MESSAGE_COMPOUND = 4;
  private static final int MESSAGE_TEXT = 5;


  private final Map<ConfigKey,ConfigKey> mapKeys = new HashMap<>();
  private final Map<ConfigValue,ConfigValue> mapValues = new HashMap<>();
  private final Map<MessagePart,MessagePart> messageParts = new HashMap<>();
  private final Map<Message.WithSpaces,Message.WithSpaces> messagesWithSpaces = new HashMap<>();


  @Contract(mutates = "param2,io")
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
    else if (message instanceof CompoundMessage)
    {
      packStream.writeSmall(MESSAGE_COMPOUND, 3);
      ((CompoundMessage)message).pack(packStream);
    }
    else if (message instanceof TextMessage)
    {
      packStream.writeSmall(MESSAGE_TEXT, 3);
      ((TextMessage)message).pack(packStream);
    }
    else
    {
      throw new IllegalArgumentException("unknown message type " +
          message.getClass().getSimpleName());
    }
  }


  @Contract(mutates = "param1,io")
  public @NotNull Message.WithSpaces unpackMessageWithSpaces(@NotNull PackInputStream packStream) throws IOException
  {
    final Message.WithSpaces message;

    switch(packStream.readSmall(3))
    {
      case MESSAGE_EMPTY:
        return EmptyMessage.INSTANCE;

      case MESSAGE_COMPOUND:
        message = CompoundMessage.unpack(this, packStream);
        break;

      case MESSAGE_TEXT:
        message = TextMessage.unpack(packStream);
        break;

      default:
        throw new IllegalStateException("message with spaces expected");
    }

    return messagesWithSpaces.computeIfAbsent(message, identity());
  }


  @Contract(mutates = "param1,io")
  public @NotNull Message.WithCode unpackMessageWithCode(@NotNull PackInputStream packStream) throws IOException
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

    throw new IllegalStateException("message with code expected");
  }


  @Contract(mutates = "param1,io")
  public @NotNull Message unpackMessage(@NotNull PackInputStream packStream) throws IOException
  {
    final Message.WithSpaces message;

    switch(packStream.readSmall(3))
    {
      case MESSAGE_EMPTY:
        return EmptyMessage.INSTANCE;

      case MESSAGE_EMPTY_WITH_CODE:
        return EmptyMessageWithCode.unpack(packStream);

      case MESSAGE_LOCALIZED_BUNDLE_WITH_CODE:
        return LocalizedMessageBundleWithCode.unpack(this, packStream);

      case MESSAGE_DELEGATE_WITH_CODE:
        return MessageDelegateWithCode.unpack(this, packStream);

      case MESSAGE_COMPOUND:
        message = CompoundMessage.unpack(this, packStream);
        break;

      case MESSAGE_TEXT:
        message = TextMessage.unpack(packStream);
        break;

      default:
        throw new IllegalStateException("message expected");
    }

    return messagesWithSpaces.computeIfAbsent(message, identity());
  }


  @Contract(mutates = "param2,io")
  public static void pack(@NotNull MessagePart messagePart, @NotNull PackOutputStream packStream) throws IOException
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
    else if (messagePart instanceof TemplatePart)
    {
      packStream.writeSmall(PART_TEMPLATE_ID, 2);
      ((TemplatePart)messagePart).pack(packStream);
    }
    else
      throw new IllegalArgumentException("unknown message part type " + messagePart.getClass().getSimpleName());
  }


  @Contract(mutates = "param1,io")
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

      case PART_TEMPLATE_ID:
        messagePart = TemplatePart.unpack(this, packStream);
        break;

      default:
        throw new IllegalStateException("message part expected");
    }

    return messageParts.computeIfAbsent(messagePart, identity());
  }


  @Contract(mutates = "param2,io")
  public static void pack(ConfigKey configKey, @NotNull PackOutputStream packStream) throws IOException
  {
    if (configKey == null)
      packStream.writeSmall(MAP_KEY_DEFAULT_ID, 3);
    else if (configKey instanceof ConfigKeyBool)
    {
      packStream.writeSmall(MAP_KEY_BOOL_ID, 3);
      ((ConfigKeyBool)configKey).pack(packStream);
    }
    else if (configKey instanceof ConfigKeyEmpty)
    {
      packStream.writeSmall(MAP_KEY_EMPTY_ID, 3);
      ((ConfigKeyEmpty)configKey).pack(packStream);
    }
    else if (configKey instanceof ConfigKeyName)
    {
      packStream.writeSmall(MAP_KEY_NAME_ID, 3);
      ((ConfigKeyName)configKey).pack(packStream);
    }
    else if (configKey instanceof ConfigKeyNull)
    {
      packStream.writeSmall(MAP_KEY_NULL_ID, 3);
      ((ConfigKeyNull)configKey).pack(packStream);
    }
    else if (configKey instanceof ConfigKeyNumber)
    {
      packStream.writeSmall(MAP_KEY_NUMBER_ID, 3);
      ((ConfigKeyNumber)configKey).pack(packStream);
    }
    else if (configKey instanceof ConfigKeyString)
    {
      packStream.writeSmall(MAP_KEY_STRING_ID, 3);
      ((ConfigKeyString)configKey).pack(packStream);
    }
    else
      throw new IllegalArgumentException("unknown map key type " + configKey.getClass().getSimpleName());
  }


  @Contract(mutates = "param1,io")
  public ConfigKey unpackMapKey(@NotNull PackInputStream packStream) throws IOException
  {
    final ConfigKey configKey;

    switch(packStream.readSmall(3))
    {
      case MAP_KEY_BOOL_ID:
        configKey = ConfigKeyBool.unpack(packStream);
        break;

      case MAP_KEY_EMPTY_ID:
        configKey = ConfigKeyEmpty.unpack(packStream);
        break;

      case MAP_KEY_NAME_ID:
        configKey = ConfigKeyName.unpack(packStream);
        break;

      case MAP_KEY_NULL_ID:
        configKey = ConfigKeyNull.unpack(packStream);
        break;

      case MAP_KEY_NUMBER_ID:
        configKey = ConfigKeyNumber.unpack(packStream);
        break;

      case MAP_KEY_STRING_ID:
        configKey = ConfigKeyString.unpack(packStream);
        break;

      case MAP_KEY_DEFAULT_ID:
        configKey = null;
        break;

      default:
        throw new IllegalStateException("map key expected");
    }

    return mapKeys.computeIfAbsent(configKey, identity());
  }


  @Contract(mutates = "param2,io")
  public static void pack(@NotNull ConfigValue configValue, @NotNull PackOutputStream packStream) throws IOException
  {
    if (configValue instanceof ConfigValueBool)
    {
      packStream.writeSmall(MAP_VALUE_BOOL_ID, 2);
      ((ConfigValueBool)configValue).pack(packStream);
    }
    else if (configValue instanceof ConfigValueMessage)
    {
      packStream.writeSmall(MAP_VALUE_MESSAGE_ID, 2);
      ((ConfigValueMessage)configValue).pack(packStream);
    }
    else if (configValue instanceof ConfigValueNumber)
    {
      packStream.writeSmall(MAP_VALUE_NUMBER_ID, 2);
      ((ConfigValueNumber)configValue).pack(packStream);
    }
    else if (configValue instanceof ConfigValueString)
    {
      packStream.writeSmall(MAP_VALUE_STRING_ID, 2);
      ((ConfigValueString)configValue).pack(packStream);
    }
    else
      throw new IllegalArgumentException("unknown map value type " + configValue.getClass().getSimpleName());
  }


  @Contract(mutates = "param1,io")
  public @NotNull ConfigValue unpackMapValue(@NotNull PackInputStream packStream) throws IOException
  {
    final ConfigValue configValue;

    switch(packStream.readSmall(2))
    {
      case MAP_VALUE_BOOL_ID:
        configValue = ConfigValueBool.unpack(packStream);
        break;

      case MAP_VALUE_MESSAGE_ID:
        configValue = ConfigValueMessage.unpack(this, packStream);
        break;

      case MAP_VALUE_NUMBER_ID:
        configValue = ConfigValueNumber.unpack(packStream);
        break;

      case MAP_VALUE_STRING_ID:
        configValue = ConfigValueString.unpack(packStream);
        break;

      default:
        throw new IllegalStateException("map value expected");
    }

    return mapValues.computeIfAbsent(configValue, identity());
  }


  @Contract(mutates = "param2,io")
  public static void packLongVar(long value, @NotNull PackOutputStream packStream) throws IOException
  {
    /*
      00     -> 0..7  (3 bit)
      01     -> -1..-8  (3 bit)
      100    -> 8..135  (7 bit)
      101    -> -9..-1032  (10 bit)
      110    -> 136..1159  (10 bit)
      1110   -> 1160..132231  (17 bit)
      11110  -> 132232..9223372036854775807  (63 bit)
      111110 -> -1033..-132104  (17 bit)
      111111 -> -132105..-9223372036854775808  (63 bit)
     */

    if (value >= 0)
    {
      if (value < 8)  // 0..7
        packStream.writeSmall((int)value, 5);
      else if (value < 136)  // 8..135
      {
        packStream.writeSmall(0b100, 3);
        packStream.writeSmall((int)value - 8, 7);
      }
      else if (value < 1160)  // 136..1159
      {
        packStream.writeSmall(0b110, 3);
        packStream.writeLarge(value - 136, 10);
      }
      else if (value < 132232)  // 1160..132231
      {
        packStream.writeSmall(0b1110, 4);
        packStream.writeLarge(value - 1160, 17);
      }
      else  // 132232..9223372036854775807
      {
        packStream.writeSmall(0b11110, 5);
        packStream.writeLarge(value - 132232, 63);
      }
    }
    else
    {
      if (value >= -8)  // -8..-1
      {
        packStream.writeSmall(0b01, 2);
        packStream.writeSmall((int)value + 8, 3);
      }
      else if (value >= -1032)  // -1032..-9
      {
        packStream.writeSmall(0b101, 3);
        packStream.writeLarge(value + 1032, 10);
      }
      else if (value >= -132104)  // -132104..-1033
      {
        packStream.writeSmall(0b111110, 6);
        packStream.writeLarge(value + 132104, 17);
      }
      else  // -9223372036854775808..-132105
      {
        packStream.writeSmall(0b111111, 6);
        packStream.writeLarge(-(value + 132105), 63);
      }
    }
  }


  @Contract(mutates = "param1,io")
  public static long unpackLongVar(@NotNull PackInputStream packStream) throws IOException
  {
    final var value = packStream.readSmall(5);

    // 00 -> 0..7  (3 bit)
    if ((value & 0b11000) == 0b00000)
      return value;

    // 01 -> -1..-8  (3 bit)
    if ((value & 0b11000) == 0b01000)
      return (value & 0b00111) - 8;

    // 100 -> 8..135  (7 bit)
    if ((value & 0b11100) == 0b10000)
      return ((value & 0b00011) << 5) + packStream.readSmall(5) + 8;

    // 101 -> -9..-1032  (10 bit)
    if ((value & 0b11100) == 0b10100)
      return ((value & 0b00011) << 8) + packStream.readSmall(8) - 1032;

    // 110 -> 136..1159  (10 bit)
    if ((value & 0b11100) == 0b11000)
      return ((value & 0b00011) << 8) + packStream.readSmall(8) + 136;

    // 1110 -> 1160..132231  (17 bit)
    if ((value & 0b11110) == 0b11100)
      return ((value & 0b00001) << 16) + packStream.readLarge(16) + 1160;

    // 11110 -> 132232..9223372036854775807  (63 bit)
    if (value == 0b11110)
      return packStream.readLarge(63) + 132232L;

    // 111110 -> -1033..-132104  (17 bit)
    if (!packStream.readBoolean())
      return packStream.readLarge(17) - 132104L;

    // 111111 -> -132105..-9223372036854775808  (63 bit)
    return -packStream.readLarge(63) - 132105L;
  }
}
