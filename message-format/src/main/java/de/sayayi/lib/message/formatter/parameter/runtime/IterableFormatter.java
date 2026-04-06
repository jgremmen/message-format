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
 *
 * @author Jeroen Gremmen
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




  private static final class TextIterator implements Iterator<Text>
  {
    private final MessageAccessor messageAccessor;
    private final Message.WithSpaces valueMessage;
    private final SingletonParameters parameters;
    private final Supplier<Text> thisText;
    private final Iterable<?> iterable;
    private final Iterator<?> iterator;
    private Text nextText;


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

      prepareNextText();
    }


    private void prepareNextText()
    {
      for(nextText = null; nextText == null && iterator.hasNext();)
      {
        var value = iterator.next();
        var text = value == iterable
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
