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
    final String keyNull = context.getConfigValueString("map-k-null").orElse("(null)").trim();
    final String keyFormat = context.getConfigValueString("map-k-fmt").orElse(null);
    final String valueNull = context.getConfigValueString("map-v-null").orElse("(null)").trim();
    final String valueFormat = context.getConfigValueString("map-v-fmt").orElse(null);
    final String thisString = context.getConfigValueString("map-this").orElse("(this map)").trim();

    return context.format(map
        .entrySet()
        .stream()
        .map(entry -> (
            formatValue(context, entry.getKey(), thisString, map, keyFormat, keyNull) +
            separator +
            formatValue(context, entry.getValue(), thisString, map, valueFormat, valueNull)).trim())
        .collect(toList()), List.class);
  }


  @Contract(pure = true)
  private @NotNull String formatValue(@NotNull FormatterContext context, Object mapValue, @NotNull String thisString,
                                      @NotNull Map<?,?> map, String valueFormat, @NotNull String valueNull)
  {
    return mapValue == map
        ? thisString
        : mapValue != null
            ? trimNotNull(context.format(mapValue, null, valueFormat))
            : valueNull;
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
