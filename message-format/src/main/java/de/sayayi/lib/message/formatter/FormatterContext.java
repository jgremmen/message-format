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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.map.MapKey;
import de.sayayi.lib.message.data.map.MapValue;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.util.OptionalBoolean;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public interface FormatterContext extends Parameters
{
  @Contract(pure = true)
  @NotNull MessageContext getMessageContext();


  @Contract(pure = true)
  boolean hasMap();


  @Contract(pure = true)
  @NotNull Optional<MapValue> getMapValue(Object key, @NotNull Set<MapKey.Type> keyTypes,
                                          Set<MapValue.Type> valueTypes);


  @Contract(pure = true)
  default @NotNull Optional<Message.WithSpaces> getMapMessage(Object key, @NotNull Set<MapKey.Type> keyTypes) {
    return getMapMessage(key, keyTypes, false);
  }


  @Contract(pure = true)
  @NotNull Optional<Message.WithSpaces> getMapMessage(Object key, @NotNull Set<MapKey.Type> keyTypes,
                                                      boolean includeDefault);


  @Contract(pure = true)
  default @NotNull Message.WithSpaces getMapMessageOrEmpty(Object key, @NotNull Set<MapKey.Type> keyTypes,
                                                           boolean includeDefault) {
    return getMapMessage(key, keyTypes, includeDefault).orElse(EmptyMessage.INSTANCE);
  }


  @Contract(pure = true)
  @NotNull Optional<Data> getConfigValueData(@NotNull String name);


  @Contract(pure = true)
  @NotNull Optional<String> getConfigValueString(@NotNull String name);


  @Contract(pure = true)
  @NotNull OptionalLong getConfigValueNumber(@NotNull String name);


  @Contract(pure = true)
  @NotNull OptionalBoolean getConfigValueBool(@NotNull String name);


  @NotNull Text delegateToNextFormatter();


  @Contract(pure = true)
  default @NotNull Text format(Object value) {
    return format(value, null, false);
  }


  @Contract(pure = true)
  default @NotNull Text format(Object value, boolean propagateFormat) {
    return format(value, null, propagateFormat);
  }


  @Contract(pure = true)
  default @NotNull Text format(Object value, @NotNull Class<?> type) {
    return format(value, type, false);
  }


  @Contract(pure = true)
  @NotNull Text format(Object value, Class<?> type, boolean propagateFormat);


  @Contract(pure = true)
  @NotNull Text format(Message.WithSpaces message);
}