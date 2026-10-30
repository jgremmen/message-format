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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.formatter.parameter.SingletonParameters;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
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

import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for array values, including primitive arrays, object arrays and atomic arrays
 * ({@link AtomicIntegerArray}, {@link AtomicLongArray}, {@link AtomicReferenceArray}).
 * <p>
 * Each array element is formatted individually and the results are joined into a single text string. Separator,
 * truncation and overflow behavior are controlled by the list configuration keys inherited from
 * {@link AbstractListFormatter}.
 * <p>
 * As a {@link SizeQueryable} formatter, it reports the length of the array.
 * <p>
 * The table below illustrates how the configuration keys interact:
 * <table border="1">
 *   <caption>Interaction of list configuration keys</caption>
 *   <tr>
 *     <th>&nbsp;array&nbsp;</th><th>&nbsp;sep-last&nbsp;</th><th>&nbsp;max-size&nbsp;</th>
 *     <th>&nbsp;value-more&nbsp;</th><th>&nbsp;result&nbsp;</th>
 *   </tr>
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
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the array formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code list-sep}</td>
 *     <td>string</td>
 *     <td>{@code ", "}</td>
 *     <td>Separator inserted between two elements.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code list-sep-last}</td>
 *     <td>string</td>
 *     <td>value of {@code list-sep}</td>
 *     <td>
 *       Separator inserted before the last element, e.g. {@code " and "}. It is not used if {@code
 *       list-value-more} is set.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@code list-max-size}</td>
 *     <td>number</td>
 *     <td>unlimited</td>
 *     <td>Maximum number of elements to include. With {@code 0} no element is included.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code list-value-more}</td>
 *     <td>string</td>
 *     <td>(none)</td>
 *     <td>Text added as a final element if the list is truncated by {@code list-max-size}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code list-unique}</td>
 *     <td>boolean</td>
 *     <td>{@code false}</td>
 *     <td>If {@code true}, only the first occurrence of each formatted element text is included.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code list-value}</td>
 *     <td>message</td>
 *     <td>{@code %{value}}</td>
 *     <td>
 *       Message used to format each element, which is available as parameter {@code value}. Elements formatted as
 *       empty text are omitted.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@code list-this}</td>
 *     <td>string</td>
 *     <td>{@code (this array)}</td>
 *     <td>Text used for an element that refers to the formatted value itself.</td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * Join the elements using a natural language separator:
 * <pre>{@code
 * messageSupport
 *     .message("%{items,list-sep-last:' and '}")
 *     .with("items", new String[] { "apples", "bananas", "cherries" })
 *     .format();
 * // "apples, bananas and cherries"
 * }</pre>
 * <p>
 * Show only the first elements and indicate that there are more:
 * <pre>{@code
 * messageSupport
 *     .message("Failed ids: %{ids,list-max-size:3,list-value-more:'...'}")
 *     .with("ids", new long[] { 101, 102, 103, 104, 105 })
 *     .format();
 * // "Failed ids: 101, 102, 103, ..."
 * }</pre>
 * <p>
 * Format each element individually. Elements formatted as empty text are omitted, which allows for filtering:
 * <pre>{@code
 * messageSupport
 *     .message("%{amounts,list-value:'%{value,number:\"#,##0.00\"}',list-sep:' | '}")
 *     .with("amounts", new double[] { 1234.5, 0.25 })
 *     .locale(Locale.US)
 *     .format();
 * // "1,234.50 | 0.25"
 *
 * messageSupport
 *     .message("%{values,list-value:\"%{value,<0:''}\"}")
 *     .with("values", new int[] { 3, -1, 7 })
 *     .format();
 * // "3, 7"
 * }</pre>
 * <p>
 * Suppress duplicates and use the size of the array for selecting the wording:
 * <pre>{@code
 * messageSupport
 *     .message("%{tags,format:size} tags: %{tags,list-unique:true}")
 *     .with("tags", new String[] { "java", "kotlin", "java" })
 *     .format();
 * // "3 tags: java, kotlin"
 *
 * messageSupport
 *     .message("%{tags,empty:'no tags',!empty:'tags: %{tags}'}")
 *     .with("tags", new String[0])
 *     .format();
 * // "no tags"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ArrayFormatter extends AbstractListFormatter<Object> implements SizeQueryable
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code array} classifier in addition to the {@link ClassifierContext#CLASSIFIER_LIST list} classifier.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier("array");
    super.updateClassifiers(context, value);

    return true;
  }


  /** {@inheritDoc} */
  @Override
  protected @NotNull Iterator<Text> createIterator(@NotNull ParameterFormatterContext context, @NotNull Object value) {
    return new TextIterator(context, value);
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns the length of the array.
   */
  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object array) {
    return OptionalLong.of(getLength(array));
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToEmptyKey(Object array, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), array == null || getLength(array) == 0);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing formattable types for primitive arrays, object arrays and atomic arrays,
   *          never {@code null}
   */
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


  /**
   * Returns the length of the given array or atomic array.
   *
   * @param array  array instance, not {@code null}
   *
   * @return  the array length
   */
  @Contract(pure = true)
  private static int getLength(@NotNull Object array)
  {
    return switch(array) {
      case AtomicIntegerArray atomicIntegerArray -> atomicIntegerArray.length();
      case AtomicLongArray atomicLongArray -> atomicLongArray.length();
      case AtomicReferenceArray<?> atomicReferenceArray -> atomicReferenceArray.length();

      default -> Array.getLength(array);
    };
  }


  /**
   * Creates an element accessor function for the given array or atomic array type.
   *
   * @param array  array instance, not {@code null}
   *
   * @return  an {@link IntFunction} that retrieves the element at a given index, never {@code null}
   */
  @Contract(pure = true)
  private static @NotNull IntFunction<Object> createGetter(@NotNull Object array)
  {
    return switch(array) {
      case AtomicIntegerArray atomicIntegerArray -> atomicIntegerArray::get;
      case AtomicLongArray atomicLongArray -> atomicLongArray::get;
      case AtomicReferenceArray<?> atomicReferenceArray -> atomicReferenceArray::get;

      default -> index -> Array.get(array, index);
    };
  }




  /**
   * Iterator that formats each array element using the configured list value message and yields the resulting non-empty
   * text elements.
   *
   * @since 0.8.0
   */
  private static final class TextIterator extends AbstractTextIterator
  {
    /** Message accessor used to format the elements. */
    private final MessageAccessor messageAccessor;

    /** Function returning the array element at a given index. */
    private final IntFunction<Object> getter;

    /** Message used to format each element. */
    private final Message.WithSpaces valueMessage;

    /** Reusable parameters holding the element currently being formatted. */
    private final SingletonParameters parameters;

    /** Lazily created text for elements referring to the formatted value itself. */
    private final Supplier<Text> thisText;

    /** Array being iterated. */
    private final Object array;

    /** Length of the array being iterated. */
    private final int length;

    /** Index of the next array element to format. */
    private int idx = 0;


    /**
     * Creates a new text iterator for the given array.
     *
     * @param context  formatter context, not {@code null}
     * @param array    the array to iterate over, not {@code null}
     */
    private TextIterator(@NotNull ParameterFormatterContext context, @NotNull Object array)
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

      initIterator();
    }


    /** {@inheritDoc} */
    @Override
    protected Text prepareNextText()
    {
      while(idx < length)
      {
        var value = getter.apply(idx++);
        var text = value == array
            ? thisText.get()
            : valueMessage.formatAsText(messageAccessor, parameters.setValue(value));

        if (!text.isEmpty())
          return text;
      }

      return null;
    }
  }
}
