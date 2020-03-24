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
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public class ParameterFactory implements Parameters
{
  public static final ParameterFactory DEFAULT = createFor((Locale)null, null);

  @Getter private final Locale locale;
  private final FormatterService formatterService;


  public static ParameterFactory createFor(Locale locale, FormatterService formatterService)
  {
    return new ParameterFactory((locale == null) ? Locale.getDefault() : locale,
        (formatterService == null) ? DefaultFormatterService.getSharedInstance() : formatterService);
  }


  public static ParameterFactory createFor(String locale, FormatterService formatterService)
  {
    return new ParameterFactory((locale == null) ? Locale.getDefault() : MessageFactory.forLanguageTag(locale),
        (formatterService == null) ? DefaultFormatterService.getSharedInstance() : formatterService);
  }


  public static ParameterFactory createFor(Locale locale)
  {
    if (locale == null)
      throw new NullPointerException("locale must not be null");

    return createFor(locale, null);
  }


  public static ParameterFactory createFor(String locale)
  {
    if (locale == null)
      throw new NullPointerException("locale must not be null");

    return createFor(locale, null);
  }


  public static ParameterFactory createFor(FormatterService formatterService)
  {
    if (formatterService == null)
      throw new NullPointerException("formatterService must not be null");

    return createFor((Locale)null, formatterService);
  }


  private ParameterFactory(Locale locale, FormatterService formatterService)
  {
    this.locale = locale;
    this.formatterService = formatterService;
  }


  @NotNull
  @Override
  public ParameterFormatter getFormatter(String format, Class<?> type) {
    return formatterService.getFormatter(format, type);
  }


  @Override
  public Object getParameterValue(@NotNull String parameter) {
    return null;
  }


  @NotNull
  @Override
  public Set<String> getParameterNames() {
    return Collections.emptySet();
  }


  public ParameterBuilder parameters() {
    return new ParameterBuilderImpl();
  }


  @SuppressWarnings("unused")
  public ParameterBuilder parameters(Map<String,Object> parameterValues) {
    return new ParameterBuilderImpl().with(parameterValues);
  }


  final class ParameterBuilderImpl implements ParameterBuilder
  {
    private final Map<String,Object> parameterValues;

    @Getter private Locale locale;


    private ParameterBuilderImpl()
    {
      parameterValues = new HashMap<String,Object>();
      locale = ParameterFactory.this.locale;
    }


    @Override
    public ParameterBuilder clear()
    {
      parameterValues.clear();
      return this;
    }


    @NotNull
    @Override
    public ParameterFormatter getFormatter(String format, Class<?> type) {
      return ParameterFactory.this.getFormatter(format, type);
    }


    @Override
    public Object getParameterValue(@NotNull String parameter) {
      return parameterValues.get(parameter);
    }


    @NotNull
    @Override
    public Set<String> getParameterNames() {
      return Collections.unmodifiableSet(parameterValues.keySet());
    }


    @Override
    public ParameterBuilder with(@NotNull String parameter, boolean value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(@NotNull String parameter, int value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(@NotNull String parameter, long value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(@NotNull String parameter, float value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(@NotNull String parameter, double value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(@NotNull String parameter, Object value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(@NotNull Map<String,Object> parameterValues)
    {
      this.parameterValues.putAll(parameterValues);
      return this;
    }


    @Override
    public ParameterBuilder withNotNull(@NotNull String parameter, Object value, @NotNull Object notNullValue)
    {
      parameterValues.put(parameter, value == null ? notNullValue : value);
      return this;
    }


    @Override
    public ParameterBuilder withNotEmpty(@NotNull String parameter, Object value, @NotNull Object notEmptyValue)
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
    public ParameterBuilder withLocale(Locale locale)
    {
      this.locale = (locale == null) ? ParameterFactory.this.getLocale() : locale;
      return this;
    }


    @Override
    public ParameterBuilder withLocale(String locale)
    {
      this.locale = (locale == null) ? ParameterFactory.this.getLocale() : MessageFactory.forLanguageTag(locale);
      return this;
    }
  }
}
