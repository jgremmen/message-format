/*
 * Copyright 2024 Jeroen Gremmen
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
import de.sayayi.lib.message.MessageBuilder;
import de.sayayi.lib.message.formatter.parameter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextJoiner;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_LIST;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.spacedText;
import static de.sayayi.lib.message.util.MessageUtil.isTrimmedEmpty;
import static java.lang.Integer.MAX_VALUE;


/**
 * Base class for formatters that render a collection of elements as a joined text string.
 * <p>
 * Subclasses (such as array, iterable and map formatters) provide an iterator over the elements; this class takes
 * care of joining them with separators and applying optional truncation.
 * <p>
 * The following configuration keys control the output. Keys marked with (*) are handled by this base class; the
 * remaining keys must be handled appropriately by the implementing subclass.
 * <ul>
 *   <li>{@code list-sep} (*) &ndash; separator between elements (default: {@code ", "})</li>
 *   <li>
 *     {@code list-sep-last} (*) &ndash; separator before the last element (e.g. {@code " and "}); if not set, the
 *     regular separator is used
 *   </li>
 *   <li>{@code list-max-size} (*) &ndash; maximum number of elements to include</li>
 *   <li>{@code list-value-more} (*) &ndash; text to append when the list is truncated (e.g. {@code "..."})</li>
 *   <li>{@code list-unique} (*) &ndash; if {@code true}, duplicate element texts are suppressed</li>
 *   <li>
 *     {@code list-value} &ndash; message format used to format each individual element (default: {@code %{value}})
 *   </li>
 *   <li>{@code list-this} &ndash; message for a reference to the list value itself</li>
 * </ul>
 * <p>
 * The table below illustrates how the configuration keys interact:
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
 * @param <T>  the collection/container type handled by this formatter
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public abstract class AbstractListFormatter<T> extends AbstractParameterFormatter<T> implements MapKeyComparator<T>
{
  /** Default message used for formatting each list element: {@code %{value}}. */
  // default list-value: %{value}
  protected static final Message.WithSpaces DEFAULT_VALUE_MESSAGE =
      MessageBuilder.create().parameter("value").build();

  /** Default separator inserted between list elements ({@code ", "}). */
  protected static final String DEFAULT_SEPARATOR = ", ";

  /** Configuration key for the maximum number of elements to include in the output. */
  protected static final String CONFIG_MAX_SIZE = "list-max-size";

  /** Configuration key for the separator between elements. */
  protected static final String CONFIG_SEPARATOR = "list-sep";

  /** Configuration key for the separator before the last element. */
  protected static final String CONFIG_SEPARATOR_LAST = "list-sep-last";

  /** Configuration key for a message that represents a self-reference to the list value itself. */
  protected static final String CONFIG_THIS = "list-this";

  /** Configuration key that controls whether duplicate element texts are suppressed. */
  protected static final String CONFIG_UNIQUE = "list-unique";

  /** Configuration key for the message format used to format each individual element. */
  protected static final String CONFIG_VALUE = "list-value";

  /** Configuration key for the text appended when the list is truncated due to {@code list-max-size}. */
  protected static final String CONFIG_VALUE_MORE = "list-value-more";


  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@link ClassifierContext#CLASSIFIER_LIST LIST} classifier.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier(CLASSIFIER_LIST);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Iterates over the elements, formats each one and joins them with separators. Truncation and overflow text are
   * applied based on the list configuration keys.
   */
  @Override
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull T list)
  {
    final var moreValue = context.getConfigValueString(CONFIG_VALUE_MORE).orElse(null);
    final var hasMoreValue = moreValue != null && !isTrimmedEmpty(moreValue);
    final var joiner = new TextJoiner();
    final var iterator = context.getConfigValueBool(CONFIG_UNIQUE).orElse(false)
        ? new UniqueTextIterator(createIterator(context, list))
        : createIterator(context, list);

    var n = (int)context.getConfigValueNumber(CONFIG_MAX_SIZE).orElse(MAX_VALUE);

    if (n == 0 && iterator.hasNext() && hasMoreValue)
      joiner.add(noSpaceText(moreValue));
    else
    {
      final var separator = spacedText(context.getConfigValueString(CONFIG_SEPARATOR).orElse(DEFAULT_SEPARATOR));

      for(var first = true; iterator.hasNext() && !(n == 0 && !hasMoreValue);)
      {
        final var text = iterator.next();

        if (first)
          first = false;
        else if (!hasMoreValue && (!iterator.hasNext() || n == 1))
        {
          joiner.add(spacedText(context
              .getConfigValueString(CONFIG_SEPARATOR_LAST)
              .orElseGet(separator::getTextWithSpaces)));
        }
        else
          joiner.add(separator);

        joiner.add(n-- == 0 ? spacedText(moreValue) : text);
      }
    }

    return joiner.asNoSpaceText();
  }


  /**
   * Creates an iterator over the formatted text representations of the elements in {@code value}.
   *
   * @param context  formatter context, not {@code null}
   * @param value    the collection/container to iterate over, not {@code null}
   *
   * @return  an iterator over the formatted element texts, never {@code null}
   */
  @Contract(pure = true)
  protected abstract @NotNull Iterator<Text> createIterator(@NotNull ParameterFormatterContext context, @NotNull T value);


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the list configuration key names, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of(CONFIG_MAX_SIZE, CONFIG_SEPARATOR, CONFIG_SEPARATOR_LAST, CONFIG_VALUE, CONFIG_VALUE_MORE, CONFIG_THIS);
  }




  /**
   * Abstract base class for iterators that lazily prepare the next {@link Text} element.
   * <p>
   * Subclasses implement {@link #prepareNextText()} to supply the next text element. The iterator reports
   * {@code hasNext() == true} as long as {@code prepareNextText()} returns a non-{@code null} value.
   */
  protected static abstract class AbstractTextIterator implements Iterator<Text>
  {
    private Text nextText = null;


    /**
     * Initializes the iterator by preparing the first text element. Must be called by subclass constructors after
     * all fields have been assigned.
     */
    protected void initIterator() {
      nextText = prepareNextText();
    }


    /**
     * Prepares and returns the next text element, or {@code null} if no more elements are available.
     * The returned text must not be {@linkplain Text#isEmpty() empty}.
     *
     * @return  the next non-empty text element, or {@code null} when exhausted
     */
    protected abstract Text prepareNextText();


    /** {@inheritDoc} */
    @Override
    public final boolean hasNext() {
      return nextText != null;
    }


    /** {@inheritDoc} */
    @Override
    public final Text next()
    {
      var text = nextText;
      if (text == null)
        throw new NoSuchElementException("next");

      nextText = prepareNextText();

      return text;
    }
  }




  /**
   * An iterator wrapper that filters out duplicate {@link Text} elements, ensuring each unique text is returned only
   * once.
   */
  private static final class UniqueTextIterator extends AbstractTextIterator
  {
    private final Set<Text> uniqueTexts = new HashSet<>();
    private final Iterator<Text> iterator;


    /**
     * Creates a new unique-filtering iterator wrapping the given delegate.
     *
     * @param iterator  the delegate iterator to filter, not {@code null}
     */
    private UniqueTextIterator(@NotNull Iterator<Text> iterator)
    {
      this.iterator = iterator;

      initIterator();
    }


    /** {@inheritDoc} */
    @Override
    protected Text prepareNextText()
    {
      while(iterator.hasNext())
      {
        final var text = iterator.next();

        if (uniqueTexts.add(text))
          return text;
      }

      return null;
    }
  }
}
