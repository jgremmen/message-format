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
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageBuilder;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.util.SupplierDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;

import static de.sayayi.lib.message.formatter.FormattableType.DEFAULT_ORDER;
import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Collections.emptyIterator;


/**
 * Parameter formatter for {@link Map} values.
 * <p>
 * Each map entry is formatted as a key-value pair and the results are joined into a single text string. Separator,
 * truncation and overflow behavior are controlled by the list configuration keys inherited from
 * {@link AbstractListFormatter}.
 * <p>
 * In addition to the inherited configuration keys, this formatter supports:
 * <ul>
 *   <li>
 *     {@code map-kv} &ndash; message format used to render each entry; the parameters {@code key} and {@code value}
 *     are available in the message (default: {@code %{key,null:'(null)'}=%{value,null:'(null)'}})
 *   </li>
 *   <li>
 *     {@code map-this} &ndash; text to use when the map references itself as a key or value
 *     (default: {@code "(this map)"})
 *   </li>
 * </ul>
 * <p>
 * As a {@link SizeQueryable} formatter, it reports the number of entries in the map.
 *
 * @author Jeroen Gremmen
 */
public final class MapFormatter extends AbstractListFormatter<Map<?,?>> implements SizeQueryable
{
  // default map-kv: %{key,null:'(null)'}=%{value,null:'(null)'}
  private static final Message.WithSpaces DEFAULT_KEY_VALUE_MESSAGE = MessageBuilder
      .create()
      .parameter("key").mapNull().message("(null)")
      .text("=")
      .parameter("value").mapNull().message("(null)")
      .build();


  /** {@inheritDoc} */
  @Override
  protected @NotNull Iterator<Text> createIterator(@NotNull ParameterFormatterContext context, @NotNull Map<?,?> map) {
    return map.isEmpty() ? emptyIterator() : new TextIterator(context, map);
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns the number of entries in the map.
   */
  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object map) {
    return OptionalLong.of(((Map<?,?>)map).size());
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToEmptyKey(Map<?,?> map, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), map == null || map.isEmpty());
  }


  /**
   * {@inheritDoc}
   * <p>
   * The {@link Map} type is registered with a higher priority than the default order. As {@code Map} is an interface,
   * concrete implementations may also match other formattable types (e.g. {@link Iterable}). The higher priority
   * ensures that map values are handled by this formatter first.
   *
   * @return  a set containing the {@link Map} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    // map implements iterable, so make sure it has a higher precedence than IterableFormatter
    return Set.of(new FormattableType(Map.class, DEFAULT_ORDER - 5));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the list configuration key names and map-specific keys ({@code map-kv},
   *          {@code map-this}), never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of(CONFIG_MAX_SIZE, CONFIG_SEPARATOR, CONFIG_SEPARATOR_LAST, CONFIG_VALUE_MORE, "map-kv", "map-this");
  }




  private static final class TextIterator implements Iterator<Text>
  {
    private final MessageAccessor messageAccessor;
    private final KeyValueParameters parameters;
    private final Supplier<Text> thisText;
    private final Map<?,?> map;
    private final Iterator<Entry<?,?>> iterator;
    private final Message.WithSpaces keyValueMessage;
    private Text nextText;


    @SuppressWarnings("unchecked")
    private TextIterator(@NotNull ParameterFormatterContext context, @NotNull Map<?,?> map)
    {
      this.map = map;

      //noinspection rawtypes
      iterator = (Iterator)map.entrySet().iterator();
      messageAccessor = context.getMessageAccessor();

      keyValueMessage = context
          .getConfigValueMessage("map-kv")
          .orElse(DEFAULT_KEY_VALUE_MESSAGE);
      parameters = new KeyValueParameters(context.getLocale());

      thisText = SupplierDelegate.of(() ->
          noSpaceText(context.getConfigValueString("map-this").orElse("(this map)")));

      prepareNextText();
    }


    private void prepareNextText()
    {
      Text text;

      for(nextText = null; nextText == null && iterator.hasNext();)
      {
        final var entry = iterator.next();

        parameters.key = fixValue(entry.getKey());
        parameters.value = fixValue(entry.getValue());

        if (!(text = noSpaceText(keyValueMessage.format(messageAccessor, parameters))).isEmpty())
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
      final var text = nextText;

      prepareNextText();

      return text;
    }


    private Object fixValue(Object value) {
      return value == map ? thisText.get() : value;
    }
  }




  private static final class KeyValueParameters implements Parameters
  {
    private final Locale locale;
    private Object key;
    private Object value;


    private KeyValueParameters(@NotNull Locale locale) {
      this.locale = locale;
    }


    @Override
    public @NotNull Locale getLocale() {
      return locale;
    }


    @Override
    public Object getParameterValue(@NotNull String parameter) {
      return "key".equals(parameter) ? key : "value".equals(parameter) ? value : null;
    }


    @Override
    public @NotNull Set<String> getParameterNames() {
      return Set.of("key", "value");
    }


    @Override
    public String toString() {
      return "Parameters(locale='" + locale + "',{key=" + key + ",value=" + value + "})";
    }
  }
}
