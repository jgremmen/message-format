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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.*;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;


/**
 * @author Jeroen Gremmen
 */
public final class MapFormatter extends AbstractParameterFormatter implements EmptyMatcher, SizeQueryable
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object value)
  {
    final Map<?,?> map = (Map<?,?>)value;
    if (map == null)
      return nullText();
    if (map.isEmpty())
      return emptyText();

    final String separator =
        spacedText(context.getConfigValueString("map-kv-sep").orElse("=")).getTextWithSpaces();
    final String nullKey =
        context.getConfigValueString("map-null-key").orElse("(null)").trim();
    final String nullValue =
        context.getConfigValueString("map-null-value").orElse("(null)").trim();
    final String thisObject = context.getConfigValueString("list-this").orElse("(this map)").trim();

    final List<String> list = map
        .entrySet()
        .stream()
        .map(entry -> {
          final Object key = entry.getKey();
          final String keyString = key == value
              ? thisObject : key != null ? trimNotNull(context.format(key)) : nullKey;

          final Object val = entry.getValue();
          final String valueString = val == value
              ? thisObject : val != null ? trimNotNull(context.format(val)) : nullValue;

          return (keyString + separator + valueString).trim();
        })
        .collect(toList());

    return context.format(list, List.class);
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
}
