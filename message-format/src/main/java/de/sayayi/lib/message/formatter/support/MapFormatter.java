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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.*;
import static java.util.Collections.singleton;
import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public final class MapFormatter extends AbstractParameterFormatter implements EmptyMatcher, SizeQueryable
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, Data data)
  {
    final Map<?,?> map = (Map<?,?>)value;
    if (map == null)
      return nullText();
    if (map.isEmpty())
      return emptyText();

    final ResourceBundle bundle = getBundle(FORMATTER_BUNDLE_NAME, parameters.getLocale());
    final String separator = getSeparator(messageContext, parameters, data);
    final StringBuilder s = new StringBuilder();
    final String nullKey = getConfigValueString(messageContext, "null-key", parameters, data,
        false, "(null)").trim();
    final String nullValue = getConfigValueString(messageContext, "null-value", parameters, data,
        false, "(null)").trim();

    for(Entry<?,?> entry: map.entrySet())
    {
      if (s.length() > 0)
        s.append(", ");

      Object key = entry.getKey();
      String keyString;

      if (key == value)
        keyString = bundle.getString("thisMap");
      else if (key != null)
        keyString = trimNotNull(messageContext.getFormatter(key.getClass()).format(messageContext, key, null, parameters, null));
      else
        keyString = nullKey;

      Object val = entry.getValue();
      String valueString;

      if (val == value)
        valueString = bundle.getString("thisMap");
      else if (val != null)
        valueString = trimNotNull(messageContext.getFormatter(val.getClass()).format(messageContext, val, null, parameters, null));
      else
        valueString = nullValue;

      s.append((keyString + separator + valueString).trim());
    }

    return noSpaceText(s.toString());
  }


  private String getSeparator(@NotNull MessageContext messageContext, Parameters parameters, Data data)
  {
    String sep = getConfigValueString(messageContext, "sep", parameters, data, true,"=");
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