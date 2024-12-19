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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.internal.CompoundMessage;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextJoiner;
import de.sayayi.lib.message.part.parameter.ParameterPart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.spacedText;
import static de.sayayi.lib.message.util.SpacesUtil.isTrimmedEmpty;
import static java.lang.Integer.MAX_VALUE;


/**
 * <p>
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
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
public abstract class AbstractListFormatter<T> extends AbstractParameterFormatter<T>
    implements SizeQueryable, ConfigKeyComparator<T>
{
  // default list-value: %{value}
  protected static final Message.WithSpaces DEFAULT_VALUE_MESSAGE =
      new CompoundMessage(List.of(new ParameterPart("value")));

  protected static final String DEFAULT_SEPARATOR = ", ";

  protected static final String CONFIG_MAX_SIZE = "list-max-size";
  protected static final String CONFIG_SEPARATOR = "list-sep";
  protected static final String CONFIG_SEPARATOR_LAST = "list-sep-last";
  protected static final String CONFIG_VALUE = "list-value";
  protected static final String CONFIG_VALUE_MORE = "list-value-more";
  protected static final String CONFIG_THIS = "list-this";


  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull T list)
  {
    var moreValue = context.getConfigValueString(CONFIG_VALUE_MORE).orElse(null);
    var hasMoreValue = moreValue != null && !isTrimmedEmpty(moreValue);
    var joiner = new TextJoiner();
    var iterator = createIterator(context, list);

    int n = (int)context.getConfigValueNumber(CONFIG_MAX_SIZE).orElse(MAX_VALUE);

    if (n == 0 && iterator.hasNext() && hasMoreValue)
      joiner.add(noSpaceText(moreValue));
    else
    {
      var separator = spacedText(context.getConfigValueString(CONFIG_SEPARATOR).orElse(DEFAULT_SEPARATOR));

      for(var first = true; iterator.hasNext() && !(n == 0 && !hasMoreValue);)
      {
        var text = iterator.next();

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


  @Contract(pure = true)
  protected abstract @NotNull Iterator<Text> createIterator(@NotNull FormatterContext context, @NotNull T value);
}
