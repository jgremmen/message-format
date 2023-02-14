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

import de.sayayi.lib.message.data.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;


/**
 * @author Jeroen Gremmen
 */
public interface MapValue extends Data
{
  Set<Type> STRING_MESSAGE_TYPE = unmodifiableSet(EnumSet.of(Type.STRING, Type.MESSAGE));
  Set<Type> STRING_TYPE = unmodifiableSet(EnumSet.of(Type.STRING));
  Set<Type> BOOL_TYPE = unmodifiableSet(EnumSet.of(Type.BOOL));
  Set<Type> NUMBER_TYPE = unmodifiableSet(EnumSet.of(Type.NUMBER));


  @Contract(pure = true)
  @NotNull Type getType();


  @Contract(pure = true)
  @NotNull Serializable asObject();




  enum Type {
    STRING, NUMBER, BOOL, MESSAGE
  }
}
