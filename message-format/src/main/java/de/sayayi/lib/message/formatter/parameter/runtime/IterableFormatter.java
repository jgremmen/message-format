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
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;

import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Iterable} values, including all {@link Collection} types.
 * <p>
 * Each element is formatted individually and the results are joined into a single text string. Separator, truncation
 * and overflow behavior are controlled by the list configuration keys inherited from {@link AbstractListFormatter}.
 * <p>
 * As a {@link SizeQueryable} formatter, it reports the number of elements in the iterable. For {@link Collection}
 * instances, the {@link Collection#size()} method is used directly; for other iterables, the elements are counted by
 * iteration.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the iterable formatter</caption>
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
 *     <td>{@code (this collection)}</td>
 *     <td>Text used for an element that refers to the formatted value itself.</td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * Format a list as natural language:
 * <pre>{@code
 * messageSupport
 *     .message("Choose %{colors,list-sep-last:' or '}")
 *     .with("colors", List.of("red", "green", "blue"))
 *     .format();
 * // "Choose red, green or blue"
 * }</pre>
 * <p>
 * Show only the first elements of a large collection:
 * <pre>{@code
 * messageSupport
 *     .message("Missing: %{files,list-max-size:2,list-value-more:'...'}")
 *     .with("files", List.of("a.txt", "b.txt", "c.txt", "d.txt"))
 *     .format();
 * // "Missing: a.txt, b.txt, ..."
 * }</pre>
 * <p>
 * Apply a message to each element, including the formatting options of the element type:
 * <pre>{@code
 * messageSupport
 *     .message("%{days,list-value:'%{value,date:\"d MMM\"}',list-sep:' / '}")
 *     .with("days", List.of(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 12, 25)))
 *     .locale(Locale.UK)
 *     .format();
 * // "1 May / 25 Dec"
 * }</pre>
 * <p>
 * Combine it with the {@code empty} key and the size of the collection:
 * <pre>{@code
 * var message = "%{users,empty:'No users',!empty:'%{users,format:size} users: %{users}'}";
 *
 * messageSupport.message(message).with("users", List.of()).format();
 * // "No users"
 *
 * messageSupport.message(message).with("users", List.of("alice", "bob")).format();
 * // "2 users: alice, bob"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.6.0
 */
public final class IterableFormatter extends AbstractListFormatter<Iterable<?>> implements SizeQueryable
{
  /** {@inheritDoc} */
  @Override
  protected @NotNull Iterator<Text> createIterator(@NotNull ParameterFormatterContext context,
                                                   @NotNull Iterable<?> iterable) {
    return new TextIterator(context, iterable);
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns the number of elements in the iterable.
   */
  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object iterable)
  {
    if (iterable instanceof Collection)
      return OptionalLong.of(((Collection<?>)iterable).size());

    var size = 0L;

    for(var ignored: (Iterable<?>)iterable)
      size++;

    return OptionalLong.of(size);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToEmptyKey(Iterable<?> iterable, @NotNull ComparatorContext context)
  {
    return forEmptyKey(context.getCompareType(),
        iterable instanceof Collection
            ? ((Collection<?>)iterable).isEmpty()
            : iterable == null || !iterable.iterator().hasNext());
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link Iterable} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Iterable.class));
  }




  /**
   * Iterator that formats each iterable element using the configured list value message and yields the resulting
   * non-empty text elements.
   *
   * @since 0.8.0
   */
  private static final class TextIterator extends AbstractTextIterator
  {
    /** Message accessor used to format the elements. */
    private final MessageAccessor messageAccessor;

    /** Message used to format each element. */
    private final Message.WithSpaces valueMessage;

    /** Reusable parameters holding the element currently being formatted. */
    private final SingletonParameters parameters;

    /** Lazily created text for elements referring to the formatted value itself. */
    private final Supplier<Text> thisText;

    /** Iterable being iterated. */
    private final Iterable<?> iterable;

    /** Iterator providing the elements to format. */
    private final Iterator<?> iterator;


    /**
     * Creates a new text iterator for the given iterable.
     *
     * @param context   formatter context, not {@code null}
     * @param iterable  the iterable to iterate over, not {@code null}
     */
    private TextIterator(@NotNull ParameterFormatterContext context, @NotNull Iterable<?> iterable)
    {
      this.iterable = iterable;

      iterator = iterable.iterator();
      messageAccessor = context.getMessageAccessor();

      valueMessage = context
          .getConfigValueMessage(CONFIG_VALUE)
          .orElse(DEFAULT_VALUE_MESSAGE);

      parameters = new SingletonParameters(context.getLocale(), "value");
      thisText = SupplierDelegate.of(() ->
          noSpaceText(context.getConfigValueString(CONFIG_THIS)
              .orElse("(this collection)")));

      initIterator();
    }


    /** {@inheritDoc} */
    @Override
    protected Text prepareNextText()
    {
      while(iterator.hasNext())
      {
        final var value = iterator.next();
        final var text = value == iterable
            ? thisText.get()
            : valueMessage.formatAsText(messageAccessor, parameters.setValue(value));

        if (!text.isEmpty())
          return text;
      }

      return null;
    }
  }
}
