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
import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
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

import java.util.Collection;
import java.util.Iterator;
import java.util.OptionalLong;
import java.util.function.Supplier;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.spacedText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.forEmptyKey;
import static java.util.Collections.singletonList;


/**
 * @author Jeroen Gremmen
 */
public final class IterableFormatter
    extends AbstractSingleTypeParameterFormatter<Iterable<?>>
    implements SizeQueryable, ConfigKeyComparator<Iterable<?>>
{
  // default list-value: %{value}
  private static final Message.WithSpaces DEFAULT_VALUE_MESSAGE =
      new CompoundMessage(singletonList(new ParameterPart("value")));


  @Override
  @Contract(pure = true)
  @SuppressWarnings("DuplicatedCode")
  public @NotNull Text formatValue(@NotNull FormatterContext context,
                                   @NotNull Iterable<?> iterable)
  {
    final Text separator = spacedText(context
        .getConfigValueString("list-sep")
        .orElse(", "));
    final Text lastSeparator = spacedText(context
        .getConfigValueString("list-sep-last")
        .orElseGet(separator::getTextWithSpaces));

    final TextJoiner joiner = new TextJoiner();
    boolean first = true;

    for(final Iterator<Text> iterator = new TextIterator(context, iterable); iterator.hasNext();)
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
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value)
  {
    if (value instanceof Collection)
      return OptionalLong.of(((Collection<?>)value).size());

    long size = 0;

    for(Object ignored: (Iterable<?>)value)
      size++;

    return OptionalLong.of(size);
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Iterable.class);
  }


  @Override
  public @NotNull MatchResult compareToEmptyKey(Iterable<?> value,
                                                @NotNull ComparatorContext context)
  {
    return forEmptyKey(context.getCompareType(),
        value instanceof Collection
            ? ((Collection<?>)value).isEmpty()
            : value == null || !value.iterator().hasNext());
  }




  private static final class TextIterator implements Iterator<Text>
  {
    private final MessageAccessor messageAccessor;
    private final Message.WithSpaces valueMessage;
    private final ValueParameters parameters;
    private final Supplier<Text> thisText;
    private final Iterable<?> iterable;
    private final Iterator<?> iterator;
    private Text nextText;


    private TextIterator(@NotNull FormatterContext context, @NotNull Iterable<?> iterable)
    {
      this.iterable = iterable;

      iterator = iterable.iterator();
      messageAccessor = context.getMessageAccessor();

      valueMessage = context
          .getConfigValueMessage("list-value")
          .orElse(DEFAULT_VALUE_MESSAGE);

      parameters = new ValueParameters(context.getLocale(), "value");
      thisText = SupplierDelegate.of(() ->
          noSpaceText(context.getConfigValueString("list-this")
              .orElse("(this collection)")));

      prepareNextText();
    }


    private void prepareNextText()
    {
      for(nextText = null; nextText == null && iterator.hasNext();)
      {
        final Text text = (parameters.value = iterator.next()) == iterable
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
