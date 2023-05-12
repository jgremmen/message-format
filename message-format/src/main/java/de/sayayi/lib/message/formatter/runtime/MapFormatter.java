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
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.NoSpaceTextPart;
import de.sayayi.lib.message.internal.part.ParameterPart;
import de.sayayi.lib.message.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.emptyText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.parameter.key.ConfigKey.NAME_TYPE;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;


/**
 * @author Jeroen Gremmen
 */
public final class MapFormatter extends AbstractParameterFormatter
    implements EmptyMatcher, SizeQueryable
{
  private static final Message.WithSpaces DEFAULT_KEY_VALUE_MESSAGE;
  private static final SortedSet<String> KEY_VALUE_PARAMETER_NAMES =
      unmodifiableSortedSet(new TreeSet<>(asList("key", "value")));


  static
  {
    DEFAULT_KEY_VALUE_MESSAGE = new CompoundMessage(asList(
        new ParameterPart("key", null, false, false, emptyMap()),
        new NoSpaceTextPart("="),
        new ParameterPart("value", null, false, false, emptyMap())
    ));
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object value)
  {
    final Map<?,?> map = (Map<?,?>)value;
    if (map == null)
      return nullText();
    if (map.isEmpty())
      return emptyText();

    final MessageAccessor messageAccessor = context.getMessageSupport();
    final Message.WithSpaces kvMessage = context
        .getConfigMessage("map-kv", NAME_TYPE).orElse(DEFAULT_KEY_VALUE_MESSAGE);
    final String keyNull = context.getConfigValueString("map-k-null")
        .map(String::trim).orElse("(null)");
    final String valueNull = context.getConfigValueString("map-v-null")
        .map(String::trim).orElse("(null)");
    final String thisString = context.getConfigValueString("map-this")
        .map(String::trim).orElse("(this map)");
    final KeyValueParameters parameters = new KeyValueParameters(messageAccessor.getLocale());

    return context.format(map
        .entrySet()
        .stream()
        .map(entry -> {
          parameters.key = fixValue(map, entry.getKey(), keyNull, thisString);
          parameters.value = fixValue(map, entry.getValue(), valueNull, thisString);

          return kvMessage.format(messageAccessor, parameters);
        })
        .collect(toList()), Iterable.class);
  }


  private @NotNull Object fixValue(@NotNull Map<?,?> map, Object value,
                                   @NotNull String nullString, @NotNull String thisString) {
    return value == null ? nullString : value == map ? thisString : value;
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value) {
    return compareType.match(((Map<?,?>)value).size()) ? TYPELESS_EXACT : null;
  }


  @Override
  public long size(@NotNull Object value) {
    return ((Map<?,?>)value).size();
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(Map.class));
  }




  private static final class KeyValueParameters implements Parameters
  {
    private final Locale locale;
    private Object key = null;
    private Object value = null;


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
    public @NotNull SortedSet<String> getParameterNames() {
      return KEY_VALUE_PARAMETER_NAMES;
    }
  }
}
