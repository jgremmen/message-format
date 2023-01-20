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
import java.util.ResourceBundle;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.emptyText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singleton;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;


/**
 * @author Jeroen Gremmen
 */
public final class MapFormatter extends AbstractParameterFormatter implements EmptyMatcher, SizeQueryable
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value)
  {
    final Map<?,?> map = (Map<?,?>)value;
    if (map == null)
      return nullText();
    if (map.isEmpty())
      return emptyText();

    final ResourceBundle bundle = getBundle(FORMATTER_BUNDLE_NAME, formatterContext.getLocale());
    final String separator = getSeparator(formatterContext);
    final String nullKey =
        formatterContext.getConfigValueString("map-null-key").orElse("(null)").trim();
    final String nullValue =
        formatterContext.getConfigValueString("map-null-value").orElse("(null)").trim();

    final List<String> list = map
        .entrySet()
        .stream()
        .map(entry -> {
          final Object key = entry.getKey();
          final String keyString = key == value
              ? bundle.getString("thisMap")
              : key != null ? trimNotNull(formatterContext.format(key)) : nullKey;

          final Object val = entry.getValue();
          final String valueString = val == value
              ? bundle.getString("thisMap")
              : val != null ? trimNotNull(formatterContext.format(val)) : nullValue;

          return (keyString + separator + valueString).trim();
        })
        .collect(toList());

    return formatterContext.format(list, List.class);
  }


  private @NotNull String getSeparator(@NotNull FormatterContext formatterContext)
  {
    final String sep = formatterContext.getConfigValueString("map-kv-sep").orElse("=");
    if (sep.isEmpty())
      return sep;

    final StringBuilder separator = new StringBuilder(sep.trim());

    if (Character.isSpaceChar(sep.charAt(0)))
      separator.insert(0, ' ');
    if (Character.isSpaceChar(sep.charAt(sep.length() - 1)))
      separator.append(' ');

    return separator.toString();
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