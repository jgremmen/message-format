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

import java.util.Collections;
import java.util.HashMap;
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


  @Override
  public ParameterFormatter getFormatter(String format, Class<?> type) {
    return formatterService.getFormatter(format, type);
  }


  @Override
  public Object getParameterValue(String parameter) {
    return null;
  }


  @Override
  public Set<String> getParameterNames() {
    return Collections.emptySet();
  }


  public ParameterBuilder parameters() {
    return new ParameterBuilderImpl();
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


    @Override
    public ParameterFormatter getFormatter(String format, Class<?> type) {
      return ParameterFactory.this.getFormatter(format, type);
    }


    @Override
    public Object getParameterValue(String parameter) {
      return parameterValues.get(parameter);
    }


    @Override
    public Set<String> getParameterNames() {
      return Collections.unmodifiableSet(parameterValues.keySet());
    }


    @Override
    public ParameterBuilder with(String parameter, boolean value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(String parameter, int value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(String parameter, long value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(String parameter, float value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(String parameter, double value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(String parameter, Object value)
    {
      parameterValues.put(parameter, value);
      return this;
    }


    @Override
    public ParameterBuilder with(Map<String, Object> parameterValues)
    {
      this.parameterValues.putAll(parameterValues);
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
