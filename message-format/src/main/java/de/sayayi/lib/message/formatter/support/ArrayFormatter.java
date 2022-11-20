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
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.*;
import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.util.Collections.unmodifiableSet;
import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public final class ArrayFormatter extends AbstractParameterFormatter implements EmptyMatcher, SizeQueryable
{
  private static final Set<Class<?>> FORMATTABLE_TYPES;


  static
  {
    final Set<Class<?>> formattableTypes = new HashSet<>();

    formattableTypes.add(Object[].class);
    formattableTypes.add(short[].class);
    formattableTypes.add(int[].class);
    formattableTypes.add(long[].class);
    formattableTypes.add(float[].class);
    formattableTypes.add(double[].class);
    formattableTypes.add(boolean[].class);

    FORMATTABLE_TYPES = unmodifiableSet(formattableTypes);
  }


  @Override
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object array, String format,
                                   @NotNull Parameters parameters, DataMap map)
  {
    if (array == null)
      return nullText();

    final int length = getLength(array);
    if (length == 0)
      return emptyText();

    final StringBuilder s = new StringBuilder();
    final Class<?> arrayType = array.getClass();
    final ParameterFormatter formatter =
        arrayType.isPrimitive() ? messageContext.getFormatter(format, arrayType.getComponentType()) : null;
    final ResourceBundle bundle = getBundle(FORMATTER_BUNDLE_NAME, parameters.getLocale());
    final String sep = getConfigValueString(messageContext, "list-sep", parameters, map,", ");
    final String sepLast = getConfigValueString(messageContext, "list-sep-last", parameters, map, sep);

    for(int i = 0; i < length; i++)
    {
      final Object value = get(array, i);
      Text text = null;

      if (value == array)
        text = new TextPart(bundle.getString("thisArray"));
      else if (formatter != null)
        text = formatter.format(messageContext, value, format, parameters, map);
      else if (value != null)
        text = messageContext.getFormatter(format, value.getClass()).format(messageContext, value, format, parameters, map);

      if (text != null && !text.isEmpty())
      {
        if (s.length() > 0)
          s.append((i + 1) < length ? sep : sepLast);

        s.append(text.getText());
      }
    }

    return noSpaceText(s.toString());
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value) {
    return compareType.match(getLength(value)) ? TYPELESS_EXACT : null;
  }


  @Override
  public long size(@NotNull Object value) {
    return getLength(value);
  }


  @Override
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return FORMATTABLE_TYPES;
  }
}