/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.internal.MessageSupportImpl.Configurer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Arrays.copyOf;
import static java.util.Collections.emptyIterator;
import static java.util.Objects.requireNonNull;
import static java.util.Spliterators.emptySpliterator;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
final class MessageParameters implements Parameters
{
  private final Locale locale;
  private final Object[] parameters;


  MessageParameters(@NotNull Configurer<?> configurer)
  {
    locale = configurer.locale;
    parameters = copyOf(configurer.parameters, configurer.parameterCount * 2);
  }


  @Override
  public @NotNull Locale getLocale() {
    return locale;
  }


  @Override
  public Object getParameterValue(@NotNull String parameter)
  {
    for(int low = 0, high = parameters.length - 2; low <= high;)
    {
      var mid = ((low + high) >>> 1) & 0xfffe;
      var cmp = parameter.compareTo((String)parameters[mid]);

      if (cmp < 0)
        high = mid - 2;
      else if (cmp > 0)
        low = mid + 2;
      else
        return parameters[mid + 1];
    }

    return null;
  }


  @Contract(pure = true)
  public @NotNull Set<String> getParameterNames() {
    return new NameSet();
  }


  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof Parameters))
      return false;

    var that = (MessageParameters)o;

    return locale.equals(that.locale) && Arrays.equals(parameters, that.parameters);
  }


  @Override
  public int hashCode() {
    return Arrays.hashCode(parameters) * 59 + locale.hashCode();
  }


  @Override
  public String toString()
  {
    var s = new StringBuilder("Parameters(locale='").append(locale).append("',{");

    for(int n = 0, l = parameters.length; n < l; n += 2)
    {
      if (n > 0)
        s.append(',');

      s.append(parameters[n]).append("=").append(parameters[n + 1]);
    }

    return s.append("})").toString();
  }




  private final class NameSet extends AbstractSet<String>
  {
    @Override
    public boolean isEmpty() {
      return parameters.length == 0;
    }


    @Override
    public int size() {
      return parameters.length >> 1;
    }


    @Override
    public void clear() {
      throw new UnsupportedOperationException("clear");
    }


    @Override
    public boolean contains(Object o)
    {
      if (o instanceof String)
        for(int low = 0, high = parameters.length - 2; low <= high;)
        {
          var mid = ((low + high) >>> 1) & 0xfffe;
          var cmp = ((String)o).compareTo((String)parameters[mid]);

          if (cmp < 0)
            high = mid - 2;
          else if (cmp > 0)
            low = mid + 2;
          else
            return true;
        }

      return false;
    }


    @Override
    public boolean add(String s) {
      throw new UnsupportedOperationException("add");
    }


    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException("remove");
    }


    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException("removeAll");
    }


    @Override
    public boolean removeIf(@NotNull Predicate<? super String> filter) {
      throw new UnsupportedOperationException("removeIf");
    }


    @Override
    public void forEach(Consumer<? super String> action)
    {
      requireNonNull(action);

      for(int n = 0, l = parameters.length; n < l; n += 2)
        action.accept((String)parameters[n]);
    }


    @Override
    public @NotNull Iterator<String> iterator()
    {
      return parameters.length == 0
          ? emptyIterator()
          : new NameIterator(MessageParameters.this);
    }


    @Override
    public @NotNull Spliterator<String> spliterator()
    {
      return parameters.length == 0
          ? emptySpliterator()
          : new NameSpliterator(MessageParameters.this);
    }


    @Override
    public String toString()
    {
      if (isEmpty())
        return "[]";

      var s = new StringBuilder("[");

      for(int n = 0, l = parameters.length; n < l; n += 2)
      {
        if (n > 0)
          s.append(", ");

        s.append((String)parameters[n]);
      }

      return s.append(']').toString();
    }
  }




  private static final class NameIterator implements Iterator<String>
  {
    private final Object[] parameters;
    private int n = 0;


    private NameIterator(@NotNull MessageParameters messageParameters) {
      parameters = messageParameters.parameters;
    }


    @Override
    public boolean hasNext() {
      return n < parameters.length;
    }


    @Override
    public String next()
    {
      if (!hasNext())
        throw new NoSuchElementException("parameter name iterator out of bounds");

      final String name = (String)parameters[n];
      n += 2;

      return name;
    }
  }




  private static final class NameSpliterator implements Spliterator<String>
  {
    private final Object[] parameters;
    private int n = 0;


    private NameSpliterator(@NotNull MessageParameters messageParameters) {
      parameters = messageParameters.parameters;
    }


    @Override
    public boolean tryAdvance(Consumer<? super String> action)
    {
      if (n < parameters.length)
      {
        action.accept((String)parameters[n]);
        n += 2;

        return true;
      }

      return false;
    }


    @Override
    public Spliterator<String> trySplit() {
      return null;
    }


    @Override
    public long estimateSize() {
      return parameters.length >> 1;
    }


    @Override
    public Comparator<? super String> getComparator() {
      return null;
    }


    @Override
    public int characteristics() {
      return DISTINCT | IMMUTABLE | NONNULL | ORDERED | SORTED | SIZED;
    }
  }
}
