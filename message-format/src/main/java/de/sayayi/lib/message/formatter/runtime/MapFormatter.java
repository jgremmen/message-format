/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.NoSpaceTextPart;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.util.SupplierDelegate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;


/**
 * @author Jeroen Gremmen
 */
public final class MapFormatter extends AbstractParameterFormatter
    implements EmptyMatcher, SizeQueryable
{
  private static final Message.WithSpaces DEFAULT_KEY_VALUE_MESSAGE;
  private static final Set<String> KEY_VALUE_PARAMETER_NAMES =
      unmodifiableSet(new TreeSet<>(asList("key", "value")));


  static
  {
    // default map-kv: %{key}=%{value}
    DEFAULT_KEY_VALUE_MESSAGE = new CompoundMessage(asList(
        new ParameterPart("key", false, false),
        new NoSpaceTextPart("="),
        new ParameterPart("value", false, false)
    ));
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Object value)
  {
    final Map<?,?> map = (Map<?,?>)value;
    if (map.isEmpty())
      return emptyText();

    final Message.WithSpaces kvMessage = context
        .getConfigValueMessage("map-kv").orElse(DEFAULT_KEY_VALUE_MESSAGE);
    final Supplier<String> keyNull = SupplierDelegate.of(() -> keyNull(context));
    final Supplier<String> valueNull = SupplierDelegate.of(() -> valueNull(context));
    final Supplier<String> thisString = SupplierDelegate.of(() -> mapThis(context));

    final MessageAccessor messageAccessor = context.getMessageSupport();
    final KeyValueParameters parameters = new KeyValueParameters(messageAccessor.getLocale());

    return context.format(map
        .entrySet()
        .stream()
        .map(entry -> {
          parameters.key = fixValue(map, entry.getKey(), keyNull, thisString);
          parameters.value = fixValue(map, entry.getValue(), valueNull, thisString);

          return kvMessage.format(messageAccessor, parameters);
        })
        .toArray(String[]::new), String[].class);
  }


  private @NotNull Object fixValue(@NotNull Map<?,?> map, Object value,
                                   @NotNull Supplier<String> nullString,
                                   @NotNull Supplier<String> thisString) {
    return value == null ? nullString.get() : value == map ? thisString.get() : value;
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value) {
    return compareType.match(((Map<?,?>)value).size()) ? TYPELESS_EXACT : null;
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value) {
    return OptionalLong.of(((Map<?,?>)value).size());
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(Map.class));
  }


  @Contract(pure = true)
  private String keyNull(@NotNull FormatterContext context)
  {
    return context
        .getConfigValueString("map-k-null")
        .map(String::trim)
        .orElse("(null)");
  }


  @Contract(pure = true)
  private String valueNull(@NotNull FormatterContext context)
  {
    return context
        .getConfigValueString("map-v-null")
        .map(String::trim)
        .orElse("(null)");
  }


  @Contract(pure = true)
  private String mapThis(@NotNull FormatterContext context)
  {
    return context
        .getConfigValueString("map-this")
        .map(String::trim)
        .orElse("(this map)");
  }



  private static final class KeyValueParameters implements Parameters
  {
    private final Locale locale;
    private Object key;
    private Object value;


    private KeyValueParameters(@NotNull Locale locale) {
      this.locale = locale;
    }


    @Override
    public @NotNull Locale getLocale() {
      return locale;
    }


    @Override
    public Object getParameterValue(@NotNull String parameter) {
      return "key".equals(parameter) ? key : "value".equals(parameter) ? value : null;
    }


    @Override
    public @NotNull Set<String> getParameterNames() {
      return KEY_VALUE_PARAMETER_NAMES;
    }


    @Override
    public String toString() {
      return "Parameters(locale='" + locale + "',{key=" + key + ",value=" + value + "})";
    }
  }
}
