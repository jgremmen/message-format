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
 * The {@code bitset} configuration key controls the output mode, which either renders the labels of the set bits or a
 * binary string.
 * <p>
 * In {@code lsb-set}/{@code msb-set} mode, each set bit is mapped to a message via a number-keyed map entry in the
 * parameter configuration. The resulting messages are formatted individually and joined into a single text string.
 * Only bits that have a matching number-keyed map entry are included in the output; unmatched bits are silently
 * skipped. Separator, truncation and overflow behavior are controlled by the list configuration keys inherited from
 * {@link AbstractListFormatter}.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the bit set formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code bitset}</td>
 *     <td>{@code lsb-set}, {@code msb-set}, {@code lsb-bits}, {@code msb-bits}</td>
 *     <td>{@code lsb-set}</td>
 *     <td>
 *       The {@code -set} values render the labels of the set bits and the {@code -bits} values all bits as a
 *       binary string, each with the least ({@code lsb}) or most ({@code msb}) significant bit first.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@code bit0}</td>
 *     <td>string</td>
 *     <td>{@code 0}</td>
 *     <td>Binary string mode only: text representation for an unset bit.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code bit1}</td>
 *     <td>string</td>
 *     <td>{@code 1}</td>
 *     <td>Binary string mode only: text representation for a set bit.</td>
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
 *   <tr>
 *     <td>{@code list-value}</td>
 *     <td>message</td>
 *     <td>{@code %{value}}</td>
 *     <td>Message used to format the text of each set bit, which is available as parameter {@code value}.</td>
 *   </tr>
 * </table>
 * <p>
 * The list configuration keys only apply to the labels of the set bits.
 * <p>
 * <b>Examples:</b>
 * <p>
 * Label the set bits. Bits without a matching number key are skipped:
 * <pre>{@code
 * var permissions = BitSet.valueOf(new long[] { 0b10101 });
 *
 * messageSupport
 *     .message("%{p,list-sep-last:' and ',0:'read',1:'write',2:'execute',4:'admin'}")
 *     .with("p", permissions)
 *     .format();
 * // "read, execute and admin"
 * }</pre>
 * <p>
 * Reverse the order of the labels and format each of them individually:
 * <pre>{@code
 * var permissions = BitSet.valueOf(new long[] { 0b101 });
 *
 * messageSupport
 *     .message("%{p,bitset:'msb-set',list-value:'[%{value}]',list-sep:' ',0:'read',2:'execute'}")
 *     .with("p", permissions)
 *     .format();
 * // "[execute] [read]"
 * }</pre>
 * <p>
 * Render all bits up to the highest set bit as a binary string, optionally with custom characters:
 * <pre>{@code
 * var bits = BitSet.valueOf(new long[] { 0b101001 });
 *
 * messageSupport.message("%{b,bitset:'msb-bits'}").with("b", bits).format();
 * // "101001"
 *
 * messageSupport.message("%{b,bitset:'lsb-bits',bit0:'-',bit1:'x'}").with("b", bits).format();
 * // "x--x-x"
 * }</pre>
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
   * Iterator that formats each bit message using the configured list value message and yields the resulting non-empty
   * text elements.
   *
   * @since 0.22.0
   */
  private static final class TextIterator extends AbstractTextIterator
  {
    /** Formatter context used to format the bit messages. */
    private final ParameterFormatterContext context;

    /** Message used to format each element. */
    private final Message valueMessage;

    /** Reusable parameters holding the element currently being formatted. */
    private final SingletonParameters parameters;

    /** Iterator providing the messages of the set bits. */
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
