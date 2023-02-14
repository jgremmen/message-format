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

import static lombok.AccessLevel.PRIVATE;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@NoArgsConstructor(access = PRIVATE)
public final class Pack
{
  public static void pack(@NotNull Message message, @NotNull PackOutputStream packStream) throws IOException
  {
    if (message instanceof EmptyMessage)
      ((EmptyMessage)message).pack(packStream);
    else if (message instanceof EmptyMessageWithCode)
      ((EmptyMessageWithCode)message).pack(packStream);
    else if (message instanceof LocalizedMessageBundleWithCode)
      ((LocalizedMessageBundleWithCode)message).pack(packStream);
    else if (message instanceof MessageDelegateWithCode)
      ((MessageDelegateWithCode)message).pack(packStream);
    else if (message instanceof ParameterizedMessage)
      ((ParameterizedMessage)message).pack(packStream);
    else if (message instanceof TextMessage)
      ((TextMessage)message).pack(packStream);
    else
      throw new IllegalArgumentException();
  }


  public static void pack(@NotNull MessagePart messagePart, @NotNull PackOutputStream packStream) throws IOException
  {
    if (messagePart instanceof ParameterPart)
      ((ParameterPart)messagePart).pack(packStream);
    else if (messagePart instanceof NoSpaceTextPart)
      ((NoSpaceTextPart)messagePart).pack(packStream);
    else if (messagePart instanceof TextPart)
      ((TextPart)messagePart).pack(packStream);
    else
      throw new IllegalArgumentException();
  }


  public static void pack(@NotNull MapKey mapKey, @NotNull PackOutputStream packStream) throws IOException
  {
    if (mapKey instanceof MapKeyBool)
      ((MapKeyBool)mapKey).pack(packStream);
    if (mapKey instanceof MapKeyEmpty)
      ((MapKeyEmpty)mapKey).pack(packStream);
    else if (mapKey instanceof MapKeyName)
      ((MapKeyName)mapKey).pack(packStream);
    else if (mapKey instanceof MapKeyNull)
      ((MapKeyNull)mapKey).pack(packStream);
    else if (mapKey instanceof MapKeyNumber)
      ((MapKeyNumber)mapKey).pack(packStream);
    else if (mapKey instanceof MapKeyString)
      ((MapKeyString)mapKey).pack(packStream);
    else
      throw new IllegalArgumentException();
  }


  public static void pack(@NotNull MapValue mapValue, @NotNull PackOutputStream packStream) throws IOException
  {
    if (mapValue instanceof MapValueBool)
      ((MapValueBool)mapValue).pack(packStream);
    if (mapValue instanceof MapValueMessage)
      ((MapValueMessage)mapValue).pack(packStream);
    else if (mapValue instanceof MapValueNumber)
      ((MapValueNumber)mapValue).pack(packStream);
    else if (mapValue instanceof MapValueString)
      ((MapValueString)mapValue).pack(packStream);
    else
      throw new IllegalArgumentException();
  }
}
