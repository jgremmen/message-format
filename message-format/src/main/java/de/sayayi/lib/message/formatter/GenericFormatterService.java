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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.support.StringFormatter;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.synchronizedMap;


/**
 * @author Jeroen Gremmen
 */
public class GenericFormatterService implements FormatterService.WithRegistry
{
  private static final Comparator<Class<?>> CLASS_SORTER =
      (o1, o2) -> o1 == o2 ? 0 : o1.getName().compareTo(o2.getName());

  private static final Map<Class<?>,Class<?>> WRAPPER_CLASS_MAP = new HashMap<>();

  private final Map<String,NamedParameterFormatter> namedFormatters = new ConcurrentHashMap<>();
  private final Map<Class<?>,ParameterFormatter> typeFormatters = new ConcurrentHashMap<>();
  private final Map<Class<?>,ParameterFormatter> cachedFormatters =
      synchronizedMap(new FixedSizeCacheMap<>(CLASS_SORTER, 128));


  static
  {
    WRAPPER_CLASS_MAP.put(char.class, Character.class);
    WRAPPER_CLASS_MAP.put(byte.class, Byte.class);
    WRAPPER_CLASS_MAP.put(double.class, Double.class);
    WRAPPER_CLASS_MAP.put(float.class, Float.class);
    WRAPPER_CLASS_MAP.put(int.class, Integer.class);
    WRAPPER_CLASS_MAP.put(long.class, Long.class);
    WRAPPER_CLASS_MAP.put(short.class, Short.class);
  }


  public GenericFormatterService()
  {
    final StringFormatter stringFormatter = new StringFormatter();

    addFormatter(stringFormatter);
    addFormatterForType(Object.class, stringFormatter);
  }


  @Override
  public void addFormatterForType(@NotNull Class<?> type, @NotNull ParameterFormatter formatter)
  {
    final ParameterFormatter currentFormatter = typeFormatters.get(type);

    if (currentFormatter == null || currentFormatter.getPriority() < formatter.getPriority())
    {
      typeFormatters.put(type, formatter);
      cachedFormatters.clear();
    }
  }


  @Override
  public void addFormatter(@NotNull ParameterFormatter formatter)
  {
    if (formatter instanceof NamedParameterFormatter)
    {
      final String format = ((NamedParameterFormatter)formatter).getName();
      if (format.isEmpty())
        throw new IllegalArgumentException("formatter name must not be empty");

      ParameterFormatter currentFormatter = namedFormatters.get(format);
      if (currentFormatter == null || currentFormatter.getPriority() < formatter.getPriority())
        namedFormatters.put(format, (NamedParameterFormatter)formatter);
    }

    for(final Class<?> type: formatter.getFormattableTypes())
      addFormatterForType(type, formatter);
  }


  @Override
  public @NotNull ParameterFormatter getFormatter(String format, @NotNull Class<?> type)
  {
    ParameterFormatter formatter = format == null ? null : namedFormatters.get(format);

    if (formatter == null && (formatter = cachedFormatters.get(type)) == null)
      cachedFormatters.put(type, formatter = resolveFormatterForType(type));

    return formatter;
  }


  @SuppressWarnings("java:S1119")
  private @NotNull ParameterFormatter resolveFormatterForType(@NotNull Class<?> type)
  {
    ParameterFormatter formatter = null;

    if (type.isPrimitive() && (formatter = typeFormatters.get(type)) == null)
      type = WRAPPER_CLASS_MAP.get(type);

    resolve:{
      while(formatter == null && type != null)
        if ((formatter = typeFormatters.get(type)) == null)
        {
          for(final Class<?> interfaceType: type.getInterfaces())
            if ((formatter = typeFormatters.get(interfaceType)) != null)
              break resolve;

          type = type.getSuperclass();
        }
    }

    return formatter == null ? typeFormatters.get(Object.class) : formatter;
  }
}
