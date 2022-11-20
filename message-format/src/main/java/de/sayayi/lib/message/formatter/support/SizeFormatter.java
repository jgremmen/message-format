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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.Type.NUMBER;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.messageToText;
import static java.util.Collections.emptySet;


/**
 * @author Jeroen Gremmen
 */
public final class SizeFormatter extends AbstractParameterFormatter implements NamedParameterFormatter
{
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "size";
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text format(@NotNull MessageContext messageContext, Object value, String format,
                              @NotNull Parameters parameters, DataMap map)
  {
    long size = 0;

    if (value != null)
    {
      ParameterFormatter formatter = messageContext.getFormatter(value.getClass());
      if (formatter instanceof SizeQueryable)
        size = ((SizeQueryable)formatter).size(value);
    }

    if (map == null || map.isEmpty())
      return messageContext.getFormatter(long.class).format(messageContext, size, null, parameters, null);

    return messageToText(messageContext,
        map.getMessage(messageContext, size, parameters, EnumSet.of(NUMBER), true), parameters);
  }


  @Override
  protected @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                      @NotNull Parameters parameters, DataMap map) {
    throw new IllegalStateException();
  }


  @Override
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return emptySet();
  }
}