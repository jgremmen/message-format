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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.MapKey;
import de.sayayi.lib.message.data.map.MapValue;
import de.sayayi.lib.message.internal.FormatterContextImpl;
import de.sayayi.lib.message.internal.ParameterizedMessage;
import de.sayayi.lib.message.internal.part.MessagePart;
import de.sayayi.lib.message.internal.part.ParameterPart;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Jeroen Gremmen
 */
public abstract class AbstractFormatterTest
{
  protected void assertFormatterForType(ParameterFormatter formatter, Class<?> type)
  {
    for(FormattableType formattableType: formatter.getFormattableTypes())
      if (formattableType.getType().isAssignableFrom(type))
        return;

    fail();
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageContext messageContext, Object value) {
    return format(messageContext, messageContext.noParameters(), value, emptyMap(), null);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageContext messageContext, Object value,
                                        @NotNull String format) {
    return format(messageContext, messageContext.noParameters(), value, emptyMap(), format);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageContext messageContext,
                                        @NotNull Parameters parameters, Object value) {
    return format(messageContext, parameters, value, emptyMap(), null);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageContext messageContext, Object value,
                                        @NotNull Map<MapKey,MapValue> map) {
    return format(messageContext, messageContext.noParameters(), value, map, null);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageContext messageContext, Object value,
                                        @NotNull Map<MapKey,MapValue> map, @NotNull String format) {
    return format(messageContext, messageContext.noParameters(), value, map, format);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageContext messageContext,
                                        @NotNull Parameters parameters, Object value,
                                        @NotNull Map<MapKey,MapValue> map,
                                        String format)
  {
    return new FormatterContextImpl(messageContext, parameters, value, null, format, new DataMap(map))
        .delegateToNextFormatter();
  }


  @Contract(pure = true)
  protected @NotNull FormatterService.WithRegistry createFormatterService(@NotNull ParameterFormatter ... formatters)
  {
    val formatterService = new GenericFormatterService();

    for(val formatter: formatters)
      formatterService.addFormatter(formatter);

    return formatterService;
  }


  @Contract(pure = true)
  protected @NotNull Message buildMessage() {
    return buildMessage(null, emptyMap());
  }


  @Contract(pure = true)
  protected @NotNull Message buildMessage(String format) {
    return buildMessage(format, emptyMap());
  }


  @Contract(pure = true)
  protected @NotNull Message buildMessage(String format, @NotNull Map<MapKey,MapValue> map)
  {
    return new ParameterizedMessage(singletonList(
        new ParameterPart("value", format, false, false, map)));
  }
}