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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.internal.TextJoiner;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.util.SupplierDelegate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.spacedText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.forEmptyKey;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ArrayFormatter extends AbstractParameterFormatter<Object>
    implements SizeQueryable, ConfigKeyComparator<Object>
{
  // default list-value: %{value}
  private static final Message.WithSpaces DEFAULT_VALUE_MESSAGE =
      new CompoundMessage(List.of(new ParameterPart("value")));


  @Override
  @SuppressWarnings("DuplicatedCode")
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Object array)
  {
    final Text separator = spacedText(context
        .getConfigValueString("list-sep").orElse(", "));
    final Text lastSeparator = spacedText(context
        .getConfigValueString("list-sep-last")
        .orElseGet(separator::getTextWithSpaces));

    final TextJoiner joiner = new TextJoiner();
    boolean first = true;

    for(final Iterator<Text> iterator = new TextIterator(context, array); iterator.hasNext();)
    {
      final Text text = iterator.next();

      if (first)
        first = false;
      else
        joiner.add(iterator.hasNext() ? separator : lastSeparator);

      joiner.add(text);
    }

    return joiner.asNoSpaceText();
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object array) {
    return OptionalLong.of(getLength(array));
  }


  @Contract(pure = true)
  private int getLength(@NotNull Object array)
  {
    if (array instanceof AtomicIntegerArray)
      return ((AtomicIntegerArray)array).length();
    else if (array instanceof AtomicLongArray)
      return ((AtomicLongArray)array).length();
    else if (array instanceof AtomicReferenceArray)
      return ((AtomicReferenceArray<?>)array).length();
    else
      return Array.getLength(array);
  }


  @Contract(pure = true)
  private static @NotNull IntFunction<Object> createGetter(@NotNull Object array)
  {
    if (array instanceof AtomicIntegerArray)
      return ((AtomicIntegerArray)array)::get;

    if (array instanceof AtomicLongArray)
      return ((AtomicLongArray)array)::get;

    if (array instanceof AtomicReferenceArray)
      return ((AtomicReferenceArray<?>)array)::get;

    return index -> Array.get(array, index);
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return Set.of(
        new FormattableType(boolean[].class),
        new FormattableType(byte[].class),
        new FormattableType(short[].class),
        new FormattableType(int[].class),
        new FormattableType(long[].class),
        new FormattableType(float[].class),
        new FormattableType(double[].class),
        new FormattableType(Object[].class),
        new FormattableType(AtomicIntegerArray.class),
        new FormattableType(AtomicLongArray.class),
        new FormattableType(AtomicReferenceArray.class));
  }


  @Override
  public @NotNull MatchResult compareToEmptyKey(Object value, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), value == null || getLength(value) == 0);
  }




  private static final class TextIterator implements Iterator<Text>
  {
    private final MessageAccessor messageAccessor;
    private final IntFunction<Object> getter;
    private final Message.WithSpaces valueMessage;
    private final ValueParameters parameters;
    private final Supplier<Text> thisText;
    private final Object array;
    private final int length;
    private Text nextText = null;
    private int idx = 0;


    private TextIterator(@NotNull FormatterContext context, @NotNull Object array)
    {
      this.array = array;

      messageAccessor = context.getMessageAccessor();
      getter = createGetter(array);

      valueMessage = context
          .getConfigValueMessage("list-value")
          .orElse(DEFAULT_VALUE_MESSAGE);

      parameters = new ValueParameters(context.getLocale(), "value");
      thisText = SupplierDelegate.of(() -> noSpaceText(
          context.getConfigValueString("list-this").orElse("(this array)")));
      length = Array.getLength(array);

      prepareNextText();
    }


    private void prepareNextText()
    {
      for(nextText = null; nextText == null && idx < length;)
      {
        final Text text = (parameters.value = getter.apply(idx++)) == array
            ? thisText.get()
            : noSpaceText(valueMessage.format(messageAccessor, parameters));

        if (!text.isEmpty())
          nextText = text;
      }
    }


    @Override
    public boolean hasNext() {
      return nextText != null;
    }


    @Override
    public Text next()
    {
      final Text text = nextText;

      prepareNextText();

      return text;
    }
  }
}
