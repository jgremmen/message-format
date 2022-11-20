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
import de.sayayi.lib.message.internal.part.TextPart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.*;
import static java.util.Collections.singleton;
import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public final class IterableFormatter extends AbstractParameterFormatter implements EmptyMatcher, SizeQueryable
{
  @SuppressWarnings("rawtypes")
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, DataMap map)
  {
    if (value == null)
      return nullText();

    final Iterator iterator = ((Iterable)value).iterator();
    if (!iterator.hasNext())
      return emptyText();

    final ResourceBundle bundle = getBundle(FORMATTER_BUNDLE_NAME, parameters.getLocale());
    final StringBuilder s = new StringBuilder();
    final String sep = getConfigValueString(messageContext, "list-sep", parameters, map, ", ");
    final String sepLast = getConfigValueString(messageContext, "list-sep-last", parameters, map, sep);

    while(iterator.hasNext())
    {
      final Object element = iterator.next();
      Text text = null;

      if (element == value)
        text = new TextPart(bundle.getString("thisCollection"));
      else if (element != null)
      {
        text = messageContext.getFormatter(format, element.getClass())
            .format(messageContext, element, format, parameters, map);
      }

      if (text != null && !text.isEmpty())
      {
        if (s.length() > 0)
          s.append(iterator.hasNext() ? sep : sepLast);

        s.append(text.getText());
      }
    }

    return noSpaceText(s.toString());
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value)
  {
    final int cmp = value instanceof Collection
        ? ((Collection<?>)value).size()
        : (((Iterable<?>)value).iterator().hasNext() ? 1 : 0);

    return compareType.match(cmp) ? TYPELESS_EXACT : null;
  }


  @Override
  public long size(@NotNull Object value)
  {
    if (value instanceof Collection)
      return ((Collection<?>)value).size();

    long size = 0;

    for(Object ignored: (Iterable<?>)value)
      size++;

    return size;
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return singleton(Iterable.class);
  }
}