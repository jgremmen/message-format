/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.MapKey.Type;
import de.sayayi.lib.message.exception.MessageException;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.Type.BOOL;
import static de.sayayi.lib.message.data.map.MapKey.Type.EMPTY;
import static de.sayayi.lib.message.data.map.MapKey.Type.NULL;
import static de.sayayi.lib.message.data.map.MapKey.Type.NUMBER;
import static de.sayayi.lib.message.data.map.MapKey.Type.STRING;


/**
 * @author Jeroen Gremmen
 */
public final class ChoiceFormatter extends AbstractParameterFormatter implements NamedParameterFormatter
{
  public static final EnumSet<Type> KEY_TYPES = EnumSet.of(NULL, EMPTY, BOOL, NUMBER, STRING);


  @NotNull
  @Override
  @Contract(pure = true)
  public String getName() {
    return "choice";
  }


  @Override
  @Contract(pure = true)
  public String format(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    if (!(data instanceof DataMap))
      throw new MessageException("data must be a choice map");

    final Message message = ((DataMap)data).getMessage(value, KEY_TYPES, true);
    return message == null ? null : message.format(parameters);
  }



  @NotNull
  @Override
  @Contract(pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.emptySet();
  }
}
