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
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.SingletonParameters;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.util.SupplierDelegate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.forEmptyKey;


/**
 * <p>
 * <table border="1">
 *   <tr><th>&nbsp;array&nbsp;</th><th>&nbsp;sep-last&nbsp;</th><th>&nbsp;max-size&nbsp;</th><th>&nbsp;value-more&nbsp;</th><th>&nbsp;result&nbsp;</th></tr>
 *   <tr><td>[]</td><td>n/a</td><td>0</td><td>n/a</td><td>''</td></tr>
 *   <tr><td>[A,B,C]</td><td>'&nbsp;and'</td><td>(undefined)</td><td>n/a</td><td>'A, B and C'</td></tr>
 *   <tr><td>[A,B,C]</td><td>n/a</td><td>2</td><td>'...'</td><td>'A, B, ...'</td></tr>
 *   <tr><td>[A,B,C]</td><td>'&nbsp;and'</td><td>2</td><td>(undefined)</td><td>'A and B'</td></tr>
 *   <tr><td>[A,B,C]</td><td>'&nbsp;and'</td><td>1</td><td>(undefined)</td><td>'A'</td></tr>
 *   <tr><td>[A,B,C]</td><td>'&nbsp;and'</td><td>0</td><td>(undefined)</td><td>''</td></tr>
 *   <tr><td>[A,B,C]</td><td>n/a</td><td>0</td><td>'...'</td><td>'...'</td></tr>
 *   <tr><td>[A,B,C]</td><td>(undefined)</td><td>(undefined)</td><td>n/a</td><td>'A, B, C'</td></tr>
 *   <tr><td>[A,B,C]</td><td>(undefined)</td><td>2</td><td>(undefined)</td><td>'A, B'</td></tr>
 *   <tr><td>[A,B,C]</td><td>(undefined)</td><td>1</td><td>(undefined)</td><td>'A'</td></tr>
 *   <tr><td>[A,B,C]</td><td>(undefined)</td><td>0</td><td>(undefined)</td><td>''</td></tr>
 * </table>
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ArrayFormatter extends AbstractListFormatter<Object>
{
  @Override
  protected @NotNull Iterator<Text> createIterator(@NotNull FormatterContext context, @NotNull Object value) {
    return new TextIterator(context, value);
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object array) {
    return OptionalLong.of(getLength(array));
  }


  @Override
  public @NotNull MatchResult compareToEmptyKey(Object array, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), array == null || getLength(array) == 0);
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


  @Contract(pure = true)
  private static int getLength(@NotNull Object array)
  {
    if (array instanceof AtomicIntegerArray)
      return ((AtomicIntegerArray)array).length();

    if (array instanceof AtomicLongArray)
      return ((AtomicLongArray)array).length();

    if (array instanceof AtomicReferenceArray)
      return ((AtomicReferenceArray<?>)array).length();

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




  private static final class TextIterator implements Iterator<Text>
  {
    private final MessageAccessor messageAccessor;
    private final IntFunction<Object> getter;
    private final Message.WithSpaces valueMessage;
    private final SingletonParameters parameters;
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
          .getConfigValueMessage(CONFIG_VALUE)
          .orElse(DEFAULT_VALUE_MESSAGE);

      parameters = new SingletonParameters(context.getLocale(), "value");
      thisText = SupplierDelegate.of(() -> noSpaceText(
          context.getConfigValueString(CONFIG_THIS).orElse("(this array)")));
      length = getLength(array);

      prepareNextText();
    }


    private void prepareNextText()
    {
      for(nextText = null; nextText == null && idx < length;)
      {
        var value = getter.apply(idx++);
        var text = value == array
            ? thisText.get()
            : noSpaceText(valueMessage.format(messageAccessor, parameters.setValue(value)));

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
      var text = nextText;

      prepareNextText();

      return text;
    }
  }
}
