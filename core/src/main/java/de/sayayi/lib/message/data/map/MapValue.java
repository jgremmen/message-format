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

import de.sayayi.lib.message.data.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.EnumSet;


/**
 * @author Jeroen Gremmen
 */
public interface MapValue extends Data
{
  EnumSet<Type> STRING_MESSAGE_TYPE = EnumSet.of(Type.STRING, Type.MESSAGE);
  EnumSet<Type> STRING_TYPE = EnumSet.of(Type.STRING);
  EnumSet<Type> BOOL_TYPE = EnumSet.of(Type.BOOL);


  @Contract(pure = true)
  @NotNull Type getType();


  @Contract(pure = true)
  @NotNull Serializable asObject();


  enum Type {
    STRING, NUMBER, BOOL, MESSAGE
  }
}
