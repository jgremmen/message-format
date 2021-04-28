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
package de.sayayi.lib.message;

import de.sayayi.lib.message.Message.ParameterBuilder;
import de.sayayi.lib.message.Message.ParameterBuilderStart;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static lombok.AccessLevel.PRIVATE;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor(access = PRIVATE)
public final class ParameterFactory implements ParameterBuilderStart
{
  /**
   * Parameter factory for the default system locale. The factory is backed by the shared instance of
   * {@link DefaultFormatterService}.
   *
   * @see Locale#getDefault()
   * @see DefaultFormatterService#getSharedInstance()
   */
  public static final ParameterFactory DEFAULT =
      new ParameterFactory(Locale.getDefault(), DefaultFormatterService.getSharedInstance());


  @Getter private final Locale locale;
  @Getter private final FormatterService formatterService;


  @Contract(value = "!null, null -> new; null, !null -> new; !null, !null -> new", pure = true)
  public static @NotNull ParameterFactory createFor(Locale locale, FormatterService formatterService)
  {
    if (locale == null && formatterService == null)
      return DEFAULT;

    return new ParameterFactory(locale == null ? Locale.getDefault() : locale,
        formatterService == null ? DefaultFormatterService.getSharedInstance() : formatterService);
  }


  @Contract(value = "!null, null -> new; null, !null -> new; !null, !null -> new", pure = true)
  public static @NotNull ParameterFactory createFor(String locale, FormatterService formatterService) {
    return createFor(locale == null ? null : MessageFactory.forLanguageTag(locale), formatterService);
  }


  @Contract(value = "_ -> new", pure = true)
  public static @NotNull ParameterFactory createFor(@NotNull Locale locale)
  {
    //noinspection ConstantConditions
    if (locale == null)
      throw new NullPointerException("locale must not be null");

    return createFor(locale, null);
  }


  @Contract(value = "_ -> new", pure = true)
  public static @NotNull ParameterFactory createFor(@NotNull String locale)
  {
    //noinspection ConstantConditions
    if (locale == null)
      throw new NullPointerException("locale must not be null");

    return createFor(locale, null);
  }


  @Contract(value = "_ -> new", pure = true)
  public static @NotNull ParameterFactory createFor(@NotNull FormatterService formatterService)
  {
    //noinspection ConstantConditions
    if (formatterService == null)
      throw new NullPointerException("formatterService must not be null");

    return createFor((Locale)null, formatterService);
  }


  @Contract(value = "-> new", pure = true)
  public @NotNull Parameters noParameters() {
    return new EmptyParameters();
  }


  @Contract(value = "_, _ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder with(@NotNull String parameter, boolean value) {
    return new ParameterBuilderImpl().with(parameter, value);
  }


  @Contract(value = "_, _ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder with(@NotNull String parameter, int value) {
    return new ParameterBuilderImpl().with(parameter, value);
  }


  @Contract(value = "_, _ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder with(@NotNull String parameter, long value) {
    return new ParameterBuilderImpl().with(parameter, value);
  }


  @Contract(value = "_, _ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder with(@NotNull String parameter, float value) {
    return new ParameterBuilderImpl().with(parameter, value);
  }


  @Contract(value = "_, _ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder with(@NotNull String parameter, double value) {
    return new ParameterBuilderImpl().with(parameter, value);
  }


  @Contract(value = "_, _ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder with(@NotNull String parameter, Object value) {
    return new ParameterBuilderImpl().with(parameter, value);
  }


  @Contract(value = "_ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder with(@NotNull Map<String, Object> parameterValues) {
    return new ParameterBuilderImpl().with(parameterValues);
  }


  @Contract(value = "_, _, _ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder withNotNull(@NotNull String parameter, Object value, @NotNull Object notNullValue) {
    return new ParameterBuilderImpl().withNotNull(parameter, value, notNullValue);
  }


  @Contract(value = "_, _, _ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder withNotEmpty(@NotNull String parameter, Object value,
                                                @NotNull Object notEmptyValue) {
    return new ParameterBuilderImpl().withNotEmpty(parameter, value, notEmptyValue);
  }


  @Contract(value = "_ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder withLocale(Locale locale) {
    return new ParameterBuilderImpl().withLocale(locale);
  }


  @Contract(value = "_ -> new", pure = true)
  @Override
  public @NotNull ParameterBuilder withLocale(String locale) {
    return new ParameterBuilderImpl().withLocale(locale);
  }




  final class ParameterBuilderImpl implements ParameterBuilder
  {
    private final Map<String,Object> parameterValues;

    @Getter private Locale locale;


    private ParameterBuilderImpl()
    {
      parameterValues = new HashMap<>();
      locale = ParameterFactory.this.locale;
    }


    @Override
    public ParameterBuilder clear()
    {
      parameterValues.clear();
      return this;
    }


    @Override
    public @NotNull ParameterFormatter getFormatter(@NotNull Class<?> type) {
      return formatterService.getFormatter(null, type);
    }


    @Override
    public @NotNull ParameterFormatter getFormatter(String format, @NotNull Class<?> type) {
      return formatterService.getFormatter(format, type);
    }


    @Override
    public Object getParameterValue(@NotNull String parameter) {
      return parameterValues.get(parameter);
    }


    @Override
    public @NotNull Set<String> getParameterNames() {
      return unmodifiableSet(parameterValues.keySet());
    }


    @Override
    public @NotNull ParameterBuilder with(@NotNull String parameter, boolean value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public @NotNull ParameterBuilder with(@NotNull String parameter, int value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public @NotNull ParameterBuilder with(@NotNull String parameter, long value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public @NotNull ParameterBuilder with(@NotNull String parameter, float value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public @NotNull ParameterBuilder with(@NotNull String parameter, double value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public @NotNull ParameterBuilder with(@NotNull String parameter, Object value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public @NotNull ParameterBuilder with(@NotNull Map<String,Object> parameterValues)
    {
      this.parameterValues.putAll(parameterValues);
      return this;
    }


    @Override
    public @NotNull ParameterBuilder withNotNull(@NotNull String parameter, Object value, @NotNull Object notNullValue)
    {
      parameterValues.put(parameter, value == null ? notNullValue : value);
      return this;
    }


    @Override
    public @NotNull ParameterBuilder withNotEmpty(@NotNull String parameter, Object value,
                                                  @NotNull Object notEmptyValue)
    {
      final boolean empty =
          value == null ||
          (value instanceof String && ((String)value).trim().isEmpty()) ||
          (value instanceof CharSequence && ((CharSequence)value).length() == 0) ||
          (value instanceof Collection && ((Collection<?>)value).isEmpty()) ||
          (value instanceof Map && ((Map<?,?>)value).isEmpty()) ||
          (value.getClass().isArray() && Array.getLength(value) == 0) ||
          (value instanceof Iterable && !((Iterable<?>)value).iterator().hasNext()) ||
          (value instanceof Iterator && !((Iterator<?>)value).hasNext());

      parameterValues.put(parameter, empty ? notEmptyValue : value);
      return this;
    }


    @Override
    public @NotNull ParameterBuilder withLocale(Locale locale)
    {
      this.locale = (locale == null) ? ParameterFactory.this.getLocale() : locale;
      return this;
    }


    @Override
    public @NotNull ParameterBuilder withLocale(String locale)
    {
      this.locale = locale == null ? ParameterFactory.this.getLocale() : MessageFactory.forLanguageTag(locale);
      return this;
    }
  }




  @NoArgsConstructor(access = PRIVATE)
  private class EmptyParameters implements Parameters
  {
    @Override
    public @NotNull Locale getLocale() {
      return locale;
    }


    @Override
    public @NotNull ParameterFormatter getFormatter(@NotNull Class<?> type) {
      return formatterService.getFormatter(null, type);
    }


    @Override
    public @NotNull ParameterFormatter getFormatter(String format, @NotNull Class<?> type) {
      return formatterService.getFormatter(format, type);
    }


    @Override
    public Object getParameterValue(@NotNull String parameter) {
      return null;
    }


    @Override
    public @NotNull Set<String> getParameterNames() {
      return emptySet();
    }
  }
}
