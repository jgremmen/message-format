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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.ResourceBundle.getBundle;


/**
 * @author Jeroen Gremmen
 */
public final class CollectionFormatter extends AbstractParameterFormatter
{
  @SuppressWarnings("rawtypes")
  @Override
  @Contract(pure = true)
  public String format(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return formatNull(parameters, data);

    final Iterable iterable = (Iterable)value;
    if (!iterable.iterator().hasNext())
      return formatEmpty(parameters, data);

    final ResourceBundle bundle = getBundle(getClass().getPackage().getName() + ".Formatter",
        parameters.getLocale());
    final StringBuilder s = new StringBuilder();

    for(Object element: iterable)
    {
      if (s.length() > 0)
        s.append(", ");

      if (element == iterable)
        s.append(bundle.getString("thisCollection"));
      else if (element != null)
        s.append(parameters.getFormatter(format, element.getClass()).format(element, format, parameters, data));
    }

    return s.toString();
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return new HashSet<Class<?>>(Arrays.asList(Collection.class, Iterable.class));
  }
}