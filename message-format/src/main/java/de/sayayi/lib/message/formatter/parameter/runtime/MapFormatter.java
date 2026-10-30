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
import static java.util.Collections.unmodifiableMap;


/**
 * Parameter formatter for {@link Map} values.
 * <p>
 * Each map entry is formatted as a key-value pair and the results are joined into a single text string. Separator,
 * truncation and overflow behavior are controlled by the list configuration keys inherited from
 * {@link AbstractListFormatter}.
 * <p>
 * As a {@link SizeQueryable} formatter, it reports the number of entries in the map.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the map formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code map-kv}</td>
 *     <td>message</td>
 *     <td>{@code %{key,null:'(null)'}=%{value,null:'(null)'}}</td>
 *     <td>
 *       Message used to format each entry. The parameters {@code key} and {@code value} are available in the
 *       message.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@code map-this}</td>
 *     <td>string</td>
 *     <td>{@code (this map)}</td>
 *     <td>Text used if the map refers to itself as a key or value.</td>
 *   </tr>
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
 * </table>
 * <p>
 * The inherited keys {@code list-value} and {@code list-this} have no effect; use {@code map-kv} and {@code map-this}
 * instead.
 * <p>
 * <b>Examples:</b>
 * <p>
 * By default, entries are formatted as {@code key=value}:
 * <pre>{@code
 * var scores = new LinkedHashMap<String,Integer>();
 * scores.put("Alice", 95);
 * scores.put("Bob", 87);
 *
 * messageSupport.message("%{scores}").with("scores", scores).format();
 * // "Alice=95, Bob=87"
 * }</pre>
 * <p>
 * Customize the format of the entries and the separators. Keys and values can use their own configuration:
 * <pre>{@code
 * var limits = new LinkedHashMap<String,Double>();
 * limits.put("cpu", 0.5);
 * limits.put("memory", 0.875);
 * limits.put("disk", 1.0);
 *
 * messageSupport
 *     .message("%{limits,map-kv:'%{key}: %{value,number:\"0.0\"}',list-sep:'; ',list-sep-last:' and '}")
 *     .with("limits", limits)
 *     .locale(Locale.US)
 *     .format();
 * // "cpu: 0.5; memory: 0.9 and disk: 1.0"
 * }</pre>
 * <p>
 * Limit the number of entries for large maps:
 * <pre>{@code
 * var env = new LinkedHashMap<String,String>();
 * env.put("HOME", "/home/user");
 * env.put("LANG", "en_US");
 * env.put("PATH", "/usr/bin");
 *
 * messageSupport
 *     .message("%{env,list-max-size:2,list-value-more:'...'}")
 *     .with("env", env)
 *     .format();
 * // "HOME=/home/user, LANG=en_US, ..."
 * }</pre>
 * <p>
 * Handle missing or empty maps and select the wording using the number of entries:
 * <pre>{@code
 * var message = "%{map,null:'no map',empty:'no entries',!empty:'%{map,format:size} entries'}";
 *
 * messageSupport.message(message).with("map", null).format();
 * // "no map"
 *
 * messageSupport.message(message).with("map", Map.of()).format();
 * // "no entries"
 *
 * messageSupport.message(message).with("map", Map.of("a", 1, "b", 2)).format();
 * // "2 entries"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
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
   * {@inheritDoc}
   * <p>
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
   * Iterator that formats each map entry using the configured key-value message and yields the resulting non-empty text
   * elements.
   *
   * @since 0.12.0
   */
  private static final class TextIterator extends AbstractTextIterator
  {
    /** Message accessor used to format the elements. */
    private final MessageAccessor messageAccessor;

    /** Reusable parameters holding the key and value of the entry currently being formatted. */
    private final KeyValueParameters parameters;

    /** Lazily created text for elements referring to the formatted value itself. */
    private final Supplier<Text> thisText;

    /** Map being iterated. */
    private final Map<?,?> map;

    /** Iterator providing the map entries to format. */
    private final Iterator<Entry<?,?>> iterator;

    /** Message used to format each map entry. */
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
   *
   * @since 0.8.0
   */
  private static final class KeyValueParameters implements Parameters
  {
    /** Locale used for formatting. */
    private final Locale locale;

    /** Key of the map entry currently being formatted. */
    private Object key;

    /** Value of the map entry currently being formatted. */
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


    /**
     * {@inheritDoc}
     *
     * @return  an unmodifiable map with keys {@code key} and {@code value}
     */
    @Override
    public @Unmodifiable @NotNull Map<String,Object> asParameterMap()
    {
      final var map = new HashMap<String,Object>(4, 1.0f);

      map.put("key", key);
      map.put("value", value);

      return unmodifiableMap(map);
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "Parameters(locale=" + locale + ",{key=" + key + ",value=" + value + "})";
    }
  }
}
