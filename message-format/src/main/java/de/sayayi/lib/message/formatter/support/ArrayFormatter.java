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

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.formatter.ParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.internal.MessagePart.Text;
import de.sayayi.lib.message.internal.TextPart;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public final class ArrayFormatter extends AbstractParameterFormatter implements EmptyMatcher
{
  @NotNull
  @Override
  public Text formatValue(Object array, String format, @NotNull Parameters parameters, Data data)
  {
    if (array == null)
      return Text.NULL;

    final int length = getLength(array);
    if (length == 0)
      return Text.EMPTY;

    final StringBuilder s = new StringBuilder();
    final Class<?> arrayType = array.getClass();
    final ParameterFormatter formatter =
        arrayType.isPrimitive() ? parameters.getFormatter(format, arrayType.getComponentType()) : null;
    final ResourceBundle bundle = getBundle(FORMATTER_BUNDLE_NAME, parameters.getLocale());

    for(int i = 0; i < length; i++)
    {
      final Object value = get(array, i);
      Text text = null;

      if (value == array)
        text = new TextPart(bundle.getString("thisArray"));
      else if (formatter != null)
        text = formatter.format(value, format, parameters, data);
      else if (value != null)
        text = parameters.getFormatter(format, value.getClass()).format(value, format, parameters, data);

      if (text != null && !text.isEmpty())
      {
        if (s.length() > 0)
          s.append(", ");

        s.append(text.getText());
      }
    }

    return new TextPart(s.toString());
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value) {
    return compareType.match(getLength(value)) ? MatchResult.TYPELESS_EXACT : null;
  }


  @NotNull
  @Override
  public Set<Class<?>> getFormattableTypes()
  {
    return new HashSet<Class<?>>(Arrays.<Class<?>>asList(
        Object[].class,
        short[].class,
        int[].class,
        long[].class,
        float[].class,
        double[].class,
        boolean[].class));
  }
}
