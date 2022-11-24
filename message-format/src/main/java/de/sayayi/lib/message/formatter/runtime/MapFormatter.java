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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
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
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, DataMap data)
  {
    final Map<?,?> map = (Map<?,?>)value;
    if (map == null)
      return nullText();
    if (map.isEmpty())
      return emptyText();

    final ResourceBundle bundle = getBundle(FORMATTER_BUNDLE_NAME, parameters.getLocale());
    final String separator = getSeparator(messageContext, parameters, data);
    final String nullKey =
        getConfigValueString(messageContext, "map-null-key", parameters, data, "(null)").trim();
    final String nullValue =
        getConfigValueString(messageContext, "map-null-value", parameters, data, "(null)").trim();

    final List<String> list = map
        .entrySet()
        .stream()
        .map(entry -> {
          Object key = entry.getKey();
          String keyString;

          if (key == value)
            keyString = bundle.getString("thisMap");
          else if (key != null)
          {
            keyString = trimNotNull(messageContext.getFormatter(key.getClass())
                .format(messageContext, key, null, parameters, data));
          }
          else
            keyString = nullKey;

          Object val = entry.getValue();
          String valueString;

          if (val == value)
            valueString = bundle.getString("thisMap");
          else if (val != null)
          {
            valueString = trimNotNull(messageContext.getFormatter(val.getClass())
                .format(messageContext, val, null, parameters, data));
          }
          else
            valueString = nullValue;

          return (keyString + separator + valueString).trim();
        })
        .collect(toList());

    return messageContext.getFormatter(List.class)
        .format(messageContext, list, null, parameters, data);
  }


  private String getSeparator(@NotNull MessageContext messageContext, Parameters parameters, DataMap data)
  {
    String sep = getConfigValueString(messageContext, "map-kv-sep", parameters, data, "=");
    if (sep.isEmpty())
      return sep;

    StringBuilder separator = new StringBuilder(sep.trim());

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
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return singleton(Map.class);
  }
}