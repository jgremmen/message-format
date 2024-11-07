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
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.NoSpaceTextPart;
import de.sayayi.lib.message.part.parameter.ParameterConfig;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyNull;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import de.sayayi.lib.message.util.SupplierDelegate;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;

import static de.sayayi.lib.message.formatter.FormattableType.DEFAULT_ORDER;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.EQ;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.forEmptyKey;
import static java.util.Collections.emptyIterator;


/**
 * @author Jeroen Gremmen
 */
public final class MapFormatter extends AbstractListFormatter<Map<?,?>>
{
  private static final Message.WithSpaces DEFAULT_KEY_VALUE_MESSAGE;


  static
  {
    final ParameterConfig nullConfig = new ParameterConfig(
        Map.of(new ConfigKeyNull(EQ), new ConfigValueString("(null)")));

    // default map-kv: %{key,null:'(null)'}=%{value,null:'(null)'}
    DEFAULT_KEY_VALUE_MESSAGE = new CompoundMessage(List.of(
        new ParameterPart("key", nullConfig),
        new NoSpaceTextPart("="),
        new ParameterPart("value", nullConfig)
    ));
  }


  @Override
  protected @NotNull Iterator<Text> createIterator(@NotNull FormatterContext context, @NotNull Map<?,?> map) {
    return map.isEmpty() ? emptyIterator() : new TextIterator(context, map);
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value) {
    return OptionalLong.of(((Map<?,?>)value).size());
  }


  @Override
  public @NotNull MatchResult compareToEmptyKey(Map<?,?> value, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), value == null || value.isEmpty());
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    // map implements iterable, so make sure it has a higher precedence than IterableFormatter
    return Set.of(new FormattableType(Map.class, DEFAULT_ORDER - 5));
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
    private TextIterator(@NotNull FormatterContext context, @NotNull Map<?,?> map)
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
          noSpaceText(context.getConfigValueString("map-this")
              .orElse("(this map)")));

      prepareNextText();
    }


    private void prepareNextText()
    {
      Text text;

      for(nextText = null; nextText == null && iterator.hasNext();)
      {
        final Entry<?,?> entry = iterator.next();

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
      final Text text = nextText;

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
