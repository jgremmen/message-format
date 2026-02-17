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
package de.sayayi.lib.message.internal.part.parameter;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.NoParameters;
import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.internal.part.config.MessagePartConfig;
import de.sayayi.lib.message.internal.part.map.MessagePartMap;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.TypedValue;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Jeroen Gremmen
 */
public abstract class AbstractFormatterTest
{
  protected void assertFormatterForType(ParameterFormatter formatter, Class<?> type)
  {
    for(val formattableType: formatter.getFormattableTypes())
      if (formattableType.getType().isAssignableFrom(type))
        return;

    fail();
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext, Object value) {
    return format(messageContext, new NoParameters(messageContext.getLocale()), value, Map.of(), Map.of(), null);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext, Object value,
                                        @NotNull String format) {
    return format(messageContext, new NoParameters(messageContext.getLocale()), value, Map.of(), Map.of(), format);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext,
                                        @NotNull Parameters parameters, Object value) {
    return format(messageContext, parameters, value, Map.of(), Map.of(), null);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext, Object value,
                                        @NotNull Map<String,TypedValue<?>> config,
                                        @NotNull Map<MapKey,TypedValue<?>> map)
  {
    return format(messageContext, new NoParameters(messageContext.getLocale()), value, config, map, null);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext, Object value,
                                        @NotNull Map<String,TypedValue<?>> config,
                                        @NotNull Map<MapKey,TypedValue<?>> map,
                                        @NotNull String format) {
    return format(messageContext, new NoParameters(messageContext.getLocale()), value, config, map, format);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext,
                                        @NotNull Parameters parameters, Object value,
                                        @NotNull Map<String,TypedValue<?>> config,
                                        @NotNull Map<MapKey,TypedValue<?>> map,
                                        String format)
  {
    return new ParameterFormatterContextImpl(messageContext, parameters, value, null, format,
        new MessagePartConfig(config), new MessagePartMap(map)).delegateToNextFormatter();
  }


  @Contract(pure = true)
  protected @NotNull FormatterService.WithRegistry createFormatterService(@NotNull ParameterFormatter ... formatters)
  {
    val formatterService = new GenericFormatterService();

    for(val formatter: formatters)
      formatterService.addFormatter(formatter);

    return formatterService;
  }
}
