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
 * The {@code bitset} configuration key controls the output mode:
 * <ul>
 *   <li>{@code lsb-set} (default) &ndash; renders set bits using their mapped messages, least significant bit first</li>
 *   <li>{@code msb-set} &ndash; renders set bits using their mapped messages, most significant bit first</li>
 *   <li>{@code lsb-bits} &ndash; formats all bits as a binary string, least significant bit first</li>
 *   <li>{@code msb-bits} &ndash; formats all bits as a binary string, most significant bit first</li>
 * </ul>
 * <p>
 * In {@code lsb-set}/{@code msb-set} mode, each set bit is mapped to a message via a number-keyed map entry in the
 * parameter configuration. The resulting messages are formatted individually and joined into a single text string.
 * Only bits that have a matching number-keyed map entry are included in the output; unmatched bits are silently
 * skipped. Separator, truncation and overflow behavior are controlled by the list configuration keys inherited from
 * {@link AbstractListFormatter}.
 * <p>
 * In {@code lsb-bits}/{@code msb-bits} mode, the following additional configuration keys are available:
 * <ul>
 *   <li>{@code bit0} &ndash; text representation for an unset bit (default: {@code 0})</li>
 *   <li>{@code bit1} &ndash; text representation for a set bit (default: {@code 1})</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.21.1
 */
public final class BitSetFormatter extends AbstractListFormatter<BitSet>
{
  /** Configuration key value for least-significant-bit-first set-bit mode. */
  private static final String CONFIG_LSB_SET = "lsb-set";

  /** Configuration key value for most-significant-bit-first set-bit mode. */
  private static final String CONFIG_MSB_SET = "msb-set";

  /**
   * Configuration key value for least-significant-bit-first binary string mode.
   *
   * @see #CONFIG_BIT0
   * @see #CONFIG_BIT1
   */
  private static final String CONFIG_LSB_BITS = "lsb-bits";

  /**
   * Configuration key value for most-significant-bit-first binary string mode.
   *
   * @see #CONFIG_BIT0
   * @see #CONFIG_BIT1
   */
  private static final String CONFIG_MSB_BITS = "msb-bits";

  /**
   * Configuration key for the text representation of an unset bit in binary string mode.
   *
   * @see #CONFIG_LSB_BITS
   * @see #CONFIG_MSB_BITS
   */
  private static final String CONFIG_BIT0 = "bit0";

  /**
   * Configuration key for the text representation of a set bit in binary string mode.
   *
   * @see #CONFIG_LSB_BITS
   * @see #CONFIG_MSB_BITS
   */
  private static final String CONFIG_BIT1 = "bit1";


  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code bit-set} classifier. In set-bit mode ({@code lsb-set}/{@code msb-set}), the
   * {@link ClassifierContext#CLASSIFIER_LIST list} classifier is added as well.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier("bit-set");

    final var bitSetConfig = context.getConfigValueString("bitset").orElse(null);
    if (!CONFIG_LSB_BITS.equals(bitSetConfig) && !CONFIG_MSB_BITS.equals(bitSetConfig))
      super.updateClassifiers(context, value);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Delegates to either the binary string formatting mode or the inherited list-based formatting, depending on the
   * {@code bitset} configuration value.
   */
  @Override
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull BitSet bitSet)
  {
    final var bitSetConfig = context.getConfigValueString("bitset");

    if (bitSetConfig.isPresent())
      switch(bitSetConfig.get())
      {
        case CONFIG_LSB_BITS:
          return formatBool(context, bitSet, true);

        case CONFIG_MSB_BITS:
          return formatBool(context, bitSet, false);
      }

    return super.formatValue(context, bitSet);
  }


  /**
   * Formats the bit set as a binary string, using the configured {@code bit0} and {@code bit1} text representations.
   *
   * @param context  formatter context, not {@code null}
   * @param bitSet   the bit set to format, not {@code null}
   * @param lsb      {@code true} for least-significant-bit-first order,
   *                 {@code false} for most-significant-bit-first order
   *
   * @return  the formatted binary string as a text element, never {@code null}
   */
  private @NotNull Text formatBool(@NotNull ParameterFormatterContext context, @NotNull BitSet bitSet, boolean lsb)
  {
    final var highBit = bitSet.length() - 1;
    final var bit0String = context.getConfigValueString(CONFIG_BIT0).orElse("0");
    final var bit1String = context.getConfigValueString(CONFIG_BIT1).orElse("1");
    final var text = new StringBuilder();

    if (lsb)
    {
      for(var b = 0; b <= highBit; b++)
        text.append(bitSet.get(b) ? bit1String : bit0String);
    }
    else
    {
      for(var b = highBit; b >= 0; b--)
        text.append(bitSet.get(b) ? bit1String : bit0String);
    }

    return noSpaceText(text.toString());
  }


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
      switch(context.getConfigValueString("bitset").orElse(CONFIG_LSB_SET))
      {
        case CONFIG_LSB_SET:
          return new TextIterator(context, bitMessages);

        case CONFIG_MSB_SET:
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
