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

import java.io.DataOutput;
import java.io.IOException;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class Pack
{
  public static void pack(@NotNull Message message, @NotNull DataOutput dataOutput) throws IOException
  {
    if (message instanceof EmptyMessage)
      ((EmptyMessage)message).pack(dataOutput);
    else if (message instanceof EmptyMessageWithCode)
      ((EmptyMessageWithCode)message).pack(dataOutput);
    else if (message instanceof LocalizedMessageBundleWithCode)
      ((LocalizedMessageBundleWithCode)message).pack(dataOutput);
    else if (message instanceof MessageDelegateWithCode)
      ((MessageDelegateWithCode)message).pack(dataOutput);
    else if (message instanceof ParameterizedMessage)
      ((ParameterizedMessage)message).pack(dataOutput);
    else if (message instanceof TextMessage)
      ((TextMessage)message).pack(dataOutput);
    else
      throw new IllegalArgumentException();
  }


  public static void pack(@NotNull MessagePart messagePart, @NotNull DataOutput dataOutput) throws IOException
  {
    if (messagePart instanceof ParameterPart)
      ((ParameterPart)messagePart).pack(dataOutput);
    else if (messagePart instanceof NoSpaceTextPart)
      ((NoSpaceTextPart)messagePart).pack(dataOutput);
    else if (messagePart instanceof TextPart)
      ((TextPart)messagePart).pack(dataOutput);
    else
      throw new IllegalArgumentException();
  }


  public static void pack(@NotNull MapKey mapKey, @NotNull DataOutput dataOutput) throws IOException
  {
    if (mapKey instanceof MapKeyBool)
      ((MapKeyBool)mapKey).pack(dataOutput);
    if (mapKey instanceof MapKeyEmpty)
      ((MapKeyEmpty)mapKey).pack(dataOutput);
    else if (mapKey instanceof MapKeyName)
      ((MapKeyName)mapKey).pack(dataOutput);
    else if (mapKey instanceof MapKeyNull)
      ((MapKeyNull)mapKey).pack(dataOutput);
    else if (mapKey instanceof MapKeyNumber)
      ((MapKeyNumber)mapKey).pack(dataOutput);
    else if (mapKey instanceof MapKeyString)
      ((MapKeyString)mapKey).pack(dataOutput);
    else
      throw new IllegalArgumentException();
  }


  public static void pack(@NotNull MapValue mapValue, @NotNull DataOutput dataOutput) throws IOException
  {
    if (mapValue instanceof MapValueBool)
      ((MapValueBool)mapValue).pack(dataOutput);
    if (mapValue instanceof MapValueMessage)
      ((MapValueMessage)mapValue).pack(dataOutput);
    else if (mapValue instanceof MapValueNumber)
      ((MapValueNumber)mapValue).pack(dataOutput);
    else if (mapValue instanceof MapValueString)
      ((MapValueString)mapValue).pack(dataOutput);
    else
      throw new IllegalArgumentException();
  }
}
