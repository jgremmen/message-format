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
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.internal.part.config.MessagePartConfig;
import de.sayayi.lib.message.internal.part.map.MessagePartMap;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.part.TypedValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * Base class for parameter formatter tests. It provides helpers for asserting the formatter selected for a type,
 * formatting values into message parts and creating formatter services, message supports and message accessors.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public abstract class AbstractFormatterTest
{
  protected void assertFormatterForType(ParameterFormatter formatter, Class<?> type)
  {
    for(var formattableType: formatter.getFormattableTypes())
      if (formattableType.getType().isAssignableFrom(type))
        return;

    fail();
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext, Object value) {
    return format(messageContext, Parameters.empty(messageContext.getLocale()), value, Map.of(), Map.of(), null);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext, Object value,
                                        @NotNull String format) {
    return format(messageContext, Parameters.empty(messageContext.getLocale()), value, Map.of(), Map.of(), format);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext,
                                        @NotNull Parameters parameters, Object value) {
    return format(messageContext, parameters, value, Map.of(), Map.of(), null);
  }


  /**
   * Formats {@code value} with a single string configuration value and no map.
   *
   * @param messageContext  message accessor, not {@code null}
   * @param value           value to format
   * @param configName      configuration key, not {@code null}
   * @param configValue     string configuration value, not {@code null}
   *
   * @return  formatted message part, never {@code null}
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext, Object value,
                                        @NotNull String configName, @NotNull String configValue) {
    return format(messageContext, value, stringConfig(messageContext, configName, configValue), Map.of());
  }


  /**
   * Formats {@code value} using the named formatter {@code format} with a single string configuration value and no map.
   *
   * @param messageContext  message accessor, not {@code null}
   * @param value           value to format
   * @param configName      configuration key, not {@code null}
   * @param configValue     string configuration value, not {@code null}
   * @param format          name of the formatter, not {@code null}
   *
   * @return  formatted message part, never {@code null}
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext, Object value,
                                        @NotNull String configName, @NotNull String configValue,
                                        @NotNull String format) {
    return format(messageContext, value, stringConfig(messageContext, configName, configValue), Map.of(), format);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext, Object value,
                                        @NotNull Map<String,TypedValue<?>> config,
                                        @NotNull Map<MapKey,TypedValue.MessageValue> map)
  {
    return format(messageContext, Parameters.empty(messageContext.getLocale()), value, config, map, null);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext, Object value,
                                        @NotNull Map<String,TypedValue<?>> config,
                                        @NotNull Map<MapKey,TypedValue.MessageValue> map,
                                        @NotNull String format) {
    return format(messageContext, Parameters.empty(messageContext.getLocale()), value, config, map, format);
  }


  @Contract(pure = true)
  protected @NotNull MessagePart format(@NotNull MessageAccessor messageContext,
                                        @NotNull Parameters parameters, Object value,
                                        @NotNull Map<String,TypedValue<?>> config,
                                        @NotNull Map<MapKey,TypedValue.MessageValue> map,
                                        String format)
  {
    return new ParameterFormatterContextImpl(messageContext, parameters, value, null, format,
        new MessagePartConfig(config), new MessagePartMap(map)).delegateToNextFormatter();
  }


  @Contract(pure = true)
  protected @NotNull FormatterService.WithRegistry createFormatterService(@NotNull ParameterFormatter ... formatters)
  {
    var formatterService = new GenericFormatterService();

    for(var formatter: formatters)
      formatterService.addFormatter(formatter);

    return formatterService;
  }


  /**
   * Creates a message support with a generic formatter service containing the given {@code formatters} only.
   *
   * @param formatters  formatters to register, not {@code null}
   *
   * @return  new message support, never {@code null}
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  protected @NotNull ConfigurableMessageSupport createMessageSupport(@NotNull ParameterFormatter... formatters) {
    return MessageSupportFactory.create(createFormatterService(formatters));
  }


  /**
   * Creates a message support with a generic formatter service containing the given {@code formatters} only, using
   * {@code locale} as the default locale.
   *
   * @param locale      default locale, not {@code null}
   * @param formatters  formatters to register, not {@code null}
   *
   * @return  new message support, never {@code null}
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  protected @NotNull ConfigurableMessageSupport createMessageSupport(@NotNull Locale locale,
                                                                      @NotNull ParameterFormatter... formatters) {
    return createMessageSupport(formatters).setLocale(locale);
  }


  /**
   * Creates the message accessor of a message support with a generic formatter service containing the given
   * {@code formatters} only.
   *
   * @param formatters  formatters to register, not {@code null}
   *
   * @return  new message accessor, never {@code null}
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  protected @NotNull MessageAccessor createMessageAccessor(@NotNull ParameterFormatter... formatters) {
    return createMessageSupport(formatters).getMessageAccessor();
  }


  /**
   * Creates the message accessor of a message support with a generic formatter service containing the given
   * {@code formatters} only, using {@code locale} as the default locale.
   *
   * @param locale      default locale, not {@code null}
   * @param formatters  formatters to register, not {@code null}
   *
   * @return  new message accessor, never {@code null}
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  protected @NotNull MessageAccessor createMessageAccessor(@NotNull Locale locale,
                                                           @NotNull ParameterFormatter... formatters) {
    return createMessageSupport(locale, formatters).getMessageAccessor();
  }


  /**
   * Creates a message support with a default formatter service, which registers all formatters declared as services,
   * and the given {@code formatters} in addition.
   *
   * @param formatters  additional formatters to register, not {@code null}
   *
   * @return  new message support, never {@code null}
   *
   * @since 0.25.0
   */
  @Contract(pure = true)
  protected @NotNull ConfigurableMessageSupport createDefaultMessageSupport(@NotNull ParameterFormatter... formatters)
  {
    var formatterService = new DefaultFormatterService();

    for(var formatter: formatters)
      formatterService.addFormatter(formatter);

    return MessageSupportFactory.create(formatterService);
  }


  @Contract(pure = true)
  private static @NotNull Map<String,TypedValue<?>> stringConfig(@NotNull MessageAccessor messageContext,
                                                                 @NotNull String configName,
                                                                 @NotNull String configValue) {
    return Map.of(configName, new TypedValueString(messageContext.getMessageFactory(), configValue));
  }
}
