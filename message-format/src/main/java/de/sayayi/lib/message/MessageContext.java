/*
 * Copyright 2021 Jeroen Gremmen
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

import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Collections.emptySortedSet;
import static java.util.Collections.unmodifiableSortedSet;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
public class MessageContext
{
  private final FormatterService formatterService;
  @Getter private final MessageFactory messageFactory;
  @Getter private final Locale locale;


  public MessageContext(@NotNull FormatterService formatterService, @NotNull MessageFactory messageFactory,
                        @NotNull String locale) {
    this(formatterService, messageFactory, MessageFactory.forLanguageTag(locale));
  }


  public MessageContext(@NotNull FormatterService formatterService, @NotNull MessageFactory messageFactory) {
    this(formatterService, messageFactory, Locale.getDefault());
  }


  /**
   * Returns the best matching formatter for the given {@code type}.
   *
   * @param type  type, never {@code null}
   *
   * @return  formatter for the given {@code type}, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull ParameterFormatter getFormatter(@NotNull Class<?> type) {
    return formatterService.getFormatter(null, type);
  }


  /**
   * <p>
   *   Returns the best matching formatter for the given {@code format} and {@code type}
   * </p>
   * <p>
   *   If {@code format} matches a named formatter it always takes precedence over {@code type}.
   * </p>
   *
   * @param format  formatter name
   * @param type    type, never {@code null}
   *
   * @return  formatter for the given {@code format} and {@code type}, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull ParameterFormatter getFormatter(String format, @NotNull Class<?> type) {
    return formatterService.getFormatter(format, type);
  }


  @Contract(value = "-> new", pure = true)
  public @NotNull ParameterBuilder parameters() {
    return new ParameterBuilderImpl();
  }


  @Contract(value = "_ -> new", pure = true)
  public @NotNull ParameterBuilder parameters(@NotNull Map<String,Object> parameterValues) {
    return new ParameterBuilderImpl().with(parameterValues);
  }


  @Contract(pure = true)
  public @NotNull Parameters noParameters()
  {
    return new Parameters() {
      @Override
      public @NotNull Locale getLocale() {
        return locale;
      }


      @Override
      public Object getParameterValue(@NotNull String parameter) {
        return null;
      }


      @Override
      public @NotNull SortedSet<String> getParameterNames() {
        return emptySortedSet();
      }
    };
  }




  @SuppressWarnings("java:S1214")
  public interface Parameters
  {
    Parameters EMPTY = new Parameters() {
      @Override
      public @NotNull Locale getLocale() {
        return Locale.ROOT;
      }


      @Override
      public Object getParameterValue(@NotNull String parameter) {
        return null;
      }


      @Override
      public @NotNull SortedSet<String> getParameterNames() {
        return emptySortedSet();
      }
    };


    /**
     * Tells for which locale the message must be formatted. If no locale is provided or if no message
     * is available for the given locale, the formatter will look for a reasonable default message.
     *
     * @return  locale, never {@code null}
     */
    @Contract(pure = true)
    @NotNull Locale getLocale();


    /**
     * Returns the value for the named {@code data}.
     *
     * @param parameter  data name
     *
     * @return  data value or {@code null} if no value is available for the given data name
     */
    @Contract(pure = true)
    Object getParameterValue(@NotNull String parameter);


    /**
     * Returns a set with names for all parameters available in this context.
     *
     * @return  set with all data names
     */
    @SuppressWarnings("unused")
    @Contract(pure = true)
    @NotNull SortedSet<String> getParameterNames();
  }




  public interface ParameterBuilder extends Parameters
  {
    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull ParameterBuilder with(@NotNull String parameter, boolean value) {
      return with(parameter, Boolean.valueOf(value));
    }


    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull ParameterBuilder with(@NotNull String parameter, int value) {
      return with(parameter, Integer.valueOf(value));
    }


    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull ParameterBuilder with(@NotNull String parameter, long value) {
      return with(parameter, Long.valueOf(value));
    }


    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull ParameterBuilder with(@NotNull String parameter, float value) {
      return with(parameter, Float.valueOf(value));
    }


    @Contract(value = "_, _ -> this", mutates = "this")
    default @NotNull ParameterBuilder with(@NotNull String parameter, double value) {
      return with(parameter, Double.valueOf(value));
    }


    @Contract(value = "_, _ -> this", mutates = "this")
    @NotNull ParameterBuilder with(@NotNull String parameter, Object value);


    @Contract(value = "_ -> this", mutates = "this")
    @NotNull ParameterBuilder with(@NotNull Map<String,Object> parameterValues);


    @Contract(value = "_ -> this", mutates = "this")
    default @NotNull ParameterBuilder with(@NotNull Properties properties)
    {
      for(Entry<Object,Object> entry: properties.entrySet())
        with((String)entry.getKey(), entry.getValue());

      return this;
    }


    @Contract(value = "_ -> this", mutates = "this")
    @NotNull ParameterBuilder withLocale(Locale locale);


    @Contract(value = "_ -> this", mutates = "this")
    @NotNull ParameterBuilder withLocale(String locale);
  }




  private final class ParameterBuilderImpl implements ParameterBuilder
  {
    private final Map<String,Object> parameterValues;

    @Getter private Locale locale;


    private ParameterBuilderImpl()
    {
      parameterValues = new HashMap<>();
      locale = MessageContext.this.locale;
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
    public @NotNull ParameterBuilder withLocale(Locale locale)
    {
      this.locale = locale == null ? MessageContext.this.locale : locale;
      return this;
    }


    @Override
    public @NotNull ParameterBuilder withLocale(String locale)
    {
      this.locale = locale == null ? MessageContext.this.locale : MessageFactory.forLanguageTag(locale);
      return this;
    }


    @Override
    public Object getParameterValue(@NotNull String parameter) {
      return parameterValues.get(parameter);
    }


    @Override
    public @NotNull SortedSet<String> getParameterNames() {
      return unmodifiableSortedSet(new TreeSet<>(parameterValues.keySet()));
    }
  }
}
