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
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.util.SupplierDelegate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.forEmptyKey;


/**
 * @author Jeroen Gremmen
 */
public final class IterableFormatter extends AbstractListFormatter<Iterable<?>>
{
  @Override
  protected @NotNull Iterator<Text> createIterator(@NotNull FormatterContext context,
                                                   @NotNull Iterable<?> iterable) {
    return new TextIterator(context, iterable);
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object iterable)
  {
    if (iterable instanceof Collection)
      return OptionalLong.of(((Collection<?>)iterable).size());

    long size = 0;

    for(Object ignored: (Iterable<?>)iterable)
      size++;

    return OptionalLong.of(size);
  }


  @Override
  public @NotNull MatchResult compareToEmptyKey(Iterable<?> iterable, @NotNull ComparatorContext context)
  {
    return forEmptyKey(context.getCompareType(),
        iterable instanceof Collection
            ? ((Collection<?>)iterable).isEmpty()
            : iterable == null || !iterable.iterator().hasNext());
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Iterable.class));
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
          .getConfigValueMessage(CONFIG_VALUE)
          .orElse(DEFAULT_VALUE_MESSAGE);

      parameters = new ValueParameters(context.getLocale(), "value");
      thisText = SupplierDelegate.of(() ->
          noSpaceText(context.getConfigValueString(CONFIG_THIS)
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
