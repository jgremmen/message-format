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
  /**
   * Configuration key for the message format used to render each map entry.
   *
   * @see #DEFAULT_KEY_VALUE_MESSAGE
   */
  private static final String CONFIG_MAP_KEY_VALUE = "map-kv";

  /** Configuration key for the text used when the map references itself as a key or value. */
  private static final String CONFIG_MAP_THIS = "map-this";

  /**
   * Default message used for formatting each map entry: {@code %{key,null:'(null)'}=%{value,null:'(null)'}}.
   *
   * @see #CONFIG_MAP_KEY_VALUE
   */
  private static final Message.WithSpaces DEFAULT_KEY_VALUE_MESSAGE = MessageBuilder
      .create()
      .parameter("key").mapNull().message("(null)")
      .text("=")
      .parameter("value").mapNull().message("(null)")
      .build();


  /**
   * Adds the {@code map} classifier.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier("map");

    return true;
  }


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
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames()
  {
    return Set.of(
        CONFIG_MAX_SIZE, CONFIG_SEPARATOR, CONFIG_SEPARATOR_LAST, CONFIG_THIS, CONFIG_UNIQUE,
        CONFIG_VALUE, CONFIG_VALUE_MORE, CONFIG_MAP_KEY_VALUE, CONFIG_MAP_THIS);
  }




  /**
   * Iterator that formats each map entry using the configured key-value message and yields the resulting
   * non-empty text elements.
   */
  private static final class TextIterator extends AbstractTextIterator
  {
    private final MessageAccessor messageAccessor;
    private final KeyValueParameters parameters;
    private final Supplier<Text> thisText;
    private final Map<?,?> map;
    private final Iterator<Entry<?,?>> iterator;
    private final Message.WithSpaces keyValueMessage;


    /**
     * Creates a new text iterator for the entries of the given map.
     *
     * @param context  formatter context, not {@code null}
     * @param map      the map whose entries to iterate over, not {@code null}
     */
    @SuppressWarnings("unchecked")
    private TextIterator(@NotNull ParameterFormatterContext context, @NotNull Map<?,?> map)
    {
      this.map = map;

      //noinspection rawtypes
      iterator = (Iterator)map.entrySet().iterator();
      messageAccessor = context.getMessageAccessor();

      keyValueMessage = context
          .getConfigValueMessage(CONFIG_MAP_KEY_VALUE)
          .orElse(DEFAULT_KEY_VALUE_MESSAGE);
      parameters = new KeyValueParameters(context.getLocale());

      thisText = SupplierDelegate.of(() ->
          noSpaceText(context.getConfigValueString(CONFIG_MAP_THIS).orElse("(this map)")));

      initIterator();
    }


    /** {@inheritDoc} */
    @Override
    protected Text prepareNextText()
    {
      while(iterator.hasNext())
      {
        final var entry = iterator.next();

        parameters.key = fixValue(entry.getKey());
        parameters.value = fixValue(entry.getValue());

        final var formattedEntry = keyValueMessage.format(messageAccessor, parameters);
        if (!formattedEntry.isEmpty())
          return noSpaceText(formattedEntry);
      }

      return null;
    }


    /**
     * Returns the given value, or the {@code thisText} placeholder if the value is a self-reference to the map.
     *
     * @param value  the value to check
     *
     * @return  the original value or a placeholder text for self-references
     */
    private Object fixValue(Object value) {
      return value == map ? thisText.get() : value;
    }
  }




  /**
   * A {@link Parameters} implementation that provides {@code key} and {@code value} parameters for formatting
   * individual map entries.
   */
  private static final class KeyValueParameters implements Parameters
  {
    private final Locale locale;
    private Object key;
    private Object value;


    /**
     * Creates a new parameters instance with the given locale.
     *
     * @param locale  the locale to use for formatting, not {@code null}
     */
    private KeyValueParameters(@NotNull Locale locale) {
      this.locale = locale;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Locale getLocale() {
      return locale;
    }


    /** {@inheritDoc} */
    @Override
    public Object getParameterValue(@NotNull String parameter) {
      return "key".equals(parameter) ? key : "value".equals(parameter) ? value : null;
    }


    /** {@inheritDoc} */
    @Override
    public @NotNull Set<String> getParameterNames() {
      return Set.of("key", "value");
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "Parameters(locale='" + locale + "',{key=" + key + ",value=" + value + "})";
    }
  }
}
