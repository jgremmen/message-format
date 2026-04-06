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

import java.util.Iterator;
import java.util.Set;

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
  // default list-value: %{value}
  protected static final Message.WithSpaces DEFAULT_VALUE_MESSAGE =
      MessageBuilder.create().parameter("value").build();

  protected static final String DEFAULT_SEPARATOR = ", ";

  protected static final String CONFIG_MAX_SIZE = "list-max-size";
  protected static final String CONFIG_SEPARATOR = "list-sep";
  protected static final String CONFIG_SEPARATOR_LAST = "list-sep-last";
  protected static final String CONFIG_VALUE = "list-value";
  protected static final String CONFIG_VALUE_MORE = "list-value-more";
  protected static final String CONFIG_THIS = "list-this";


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
    final var iterator = createIterator(context, list);

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
}
