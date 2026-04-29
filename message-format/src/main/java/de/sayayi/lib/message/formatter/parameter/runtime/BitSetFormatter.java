/*
 * Copyright 2026 Jeroen Gremmen
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
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.formatter.parameter.SingletonParameters;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static de.sayayi.lib.message.part.MapKey.NUMBER_TYPE;
import static de.sayayi.lib.message.part.MessagePart.Text;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyIterator;


/**
 * Parameter formatter for {@link BitSet} values.
 * <p>
 * Each set bit is mapped to a message via a number-keyed map in the parameter configuration. The resulting messages
 * are formatted individually and joined into a single text string. Separator, truncation and overflow behavior are
 * controlled by the list configuration keys inherited from {@link AbstractListFormatter}.
 * <p>
 * The bit ordering can be controlled with the {@code bitset} configuration key:
 * <ul>
 *   <li>{@code lsb1st} (default) &ndash; least significant bit first</li>
 *   <li>{@code msb1st} &ndash; most significant bit first</li>
 * </ul>
 * <p>
 * Only bits that have a matching number-keyed map entry are included in the output; unmatched bits are silently
 * skipped.
 *
 * @author Jeroen Gremmen
 * @since 0.21.1
 */
public final class BitSetFormatter extends AbstractListFormatter<BitSet>
{
  /** {@inheritDoc} */
  @Override
  protected @NotNull Iterator<Text> createIterator(@NotNull ParameterFormatterContext context, @NotNull BitSet bitSet)
  {
    List<Message.WithSpaces> bitMessages = new ArrayList<>();

    for(var bit = bitSet.nextSetBit(0); bit >= 0; bit = bitSet.nextSetBit(bit + 1))
    {
      context.getMapMessage(bit, NUMBER_TYPE).ifPresent(bitMessages::add);

      if (bit == MAX_VALUE)
        break;
    }

    if (!bitMessages.isEmpty())
      switch(context.getConfigValueString("bitset").orElse("lsb1st"))
      {
        case "lsb1st":
          return new TextIterator(context, bitMessages);

        case "msb1st":
          return new TextIterator(context, bitMessages.reversed());
      }

    return emptyIterator();
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link BitSet} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(BitSet.class));
  }




  /**
   * Iterator that formats each bit message using the configured list value message and yields the resulting
   * non-empty text elements.
   */
  private static final class TextIterator extends AbstractTextIterator
  {
    private final ParameterFormatterContext context;
    private final Message valueMessage;
    private final SingletonParameters parameters;
    private final Iterator<Message.WithSpaces> messageIterator;


    /**
     * Creates a new text iterator for the given bit messages.
     *
     * @param context      formatter context, not {@code null}
     * @param bitMessages  the bit messages to iterate over, not {@code null}
     */
    private TextIterator(@NotNull ParameterFormatterContext context, @NotNull List<Message.WithSpaces> bitMessages)
    {
      this.context = context;

      messageIterator = bitMessages.iterator();
      valueMessage = context
          .getConfigValueMessage(CONFIG_VALUE)
          .orElse(DEFAULT_VALUE_MESSAGE);

      parameters = new SingletonParameters(context.getLocale(), "value");

      initIterator();
    }


    /** {@inheritDoc} */
    @Override
    protected Text prepareNextText()
    {
      while(messageIterator.hasNext())
      {
        // format the message associated with the bit number
        parameters.setValue(context.format(messageIterator.next()).getText());

        // format the list value
        final var formattedValue = valueMessage.format(context.getMessageAccessor(), parameters);

        if (!formattedValue.isEmpty())
          return noSpaceText(formattedValue);
      }

      return null;
    }
  }
}
