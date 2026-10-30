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
package de.sayayi.lib.message.formatter.parameter.named.extra;

import de.sayayi.lib.message.formatter.parameter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.BitSet;


/**
 * Named parameter formatter that converts integral numeric values into a {@link BitSet} and delegates the actual
 * formatting to the registered {@link BitSet} formatter (e.g.
 * {@link de.sayayi.lib.message.formatter.parameter.runtime.BitSetFormatter BitSetFormatter}).
 * <p>
 * This formatter accepts all Java integral types ({@code byte}, {@code short}, {@code int}, {@code long} and their
 * boxed counterparts), {@code char}/{@link Character} and {@link BigInteger}. The value is converted to a
 * {@link BitSet} representation and then formatted using the {@code BitSet} formatter registered with the message
 * support. A formatter for type {@code BitSet} (such as {@code BitSetFormatter}) <b>must</b> be registered for this
 * named formatter to work properly.
 * <p>
 * The formatter is selected by specifying the name {@code "bitmask"} in a message parameter, for example:<br>
 * {@code %{flags,format:bitmask,0:'bit 0',1:'bit 1',2:'bit 2'}}.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the bitmask formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code bitset}</td>
 *     <td>{@code lsb-set}, {@code msb-set}, {@code lsb-bits}, {@code msb-bits}</td>
 *     <td>{@code lsb-set}</td>
 *     <td>Selects labels for the set bits or a binary string, each with the least or most significant bit first.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code bit0}</td>
 *     <td>string</td>
 *     <td>{@code 0}</td>
 *     <td>Text for an unset bit in binary string mode.</td>
 *   </tr>
 *   <tr><td>{@code bit1}</td><td>string</td><td>{@code 1}</td><td>Text for a set bit in binary string mode.</td></tr>
 *   <tr>
 *     <td>{@code list-sep}</td>
 *     <td>string</td>
 *     <td>{@code ", "}</td>
 *     <td>Separator between the labels of the set bits.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code list-sep-last}</td>
 *     <td>string</td>
 *     <td>value of {@code list-sep}</td>
 *     <td>Separator before the last label.</td>
 *   </tr>
 * </table>
 * <p>
 * All configuration keys are evaluated by the {@code BitSet} formatter, which supports additional list configuration
 * keys.
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is not part of the {@link de.sayayi.lib.message.formatter.DefaultFormatterService
 * DefaultFormatterService} and must be registered explicitly:
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new BitmaskFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * Label the bits of a flag value. Set bits without a matching number key are skipped:
 * <pre>{@code
 * messageSupport
 *     .message("Permissions: %{flags,format:bitmask,list-sep-last:' and ',0:'read',1:'write',2:'execute'}")
 *     .with("flags", 0b1101)
 *     .format();
 * // "Permissions: read and execute"
 * }</pre>
 * <p>
 * Render the bit pattern as a string. Negative values are treated as unsigned, so all bits of the type are shown:
 * <pre>{@code
 * messageSupport
 *     .message("%{mask,format:bitmask,bitset:'msb-bits',bit0:'.',bit1:'x'}")
 *     .with("mask", (byte)-107)
 *     .format();
 * // "x..x.x.x"
 * }</pre>
 * <p>
 * {@link BigInteger} values are not limited to 64 bits:
 * <pre>{@code
 * messageSupport
 *     .message("%{v,format:bitmask,bitset:'msb-set',0:'low',100:'high'}")
 *     .with("v", BigInteger.ONE.shiftLeft(100).or(BigInteger.ONE))
 *     .format();
 * // "high, low"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.21.1
 *
 * @see de.sayayi.lib.message.formatter.parameter.runtime.BitSetFormatter BitSetFormatter
 */
public final class BitmaskFormatter extends AbstractParameterFormatter<Object>
    implements NamedParameterFormatter
{
  /**
   * {@inheritDoc}
   *
   * @return  {@code "bitmask"}
   */
  @Override
  public @NotNull String getName() {
    return "bitmask";
  }


  /** {@inheritDoc} */
  @Override
  public boolean canFormat(@NotNull Class<?> type) {
    return type == NULL_TYPE || isIntegralType(type);
  }


  /**
   * Tells whether the given type is an integral numeric type supported by this formatter.
   *
   * @param type  the type to check, not {@code null}
   *
   * @return  {@code true} if the type is a supported integral type, {@code false} otherwise
   */
  @Contract(pure = true)
  private boolean isIntegralType(@NotNull Class<?> type)
  {
    return
        type == long.class || type == Long.class ||
        type == int.class || type == Integer.class ||
        type == short.class || type == Short.class ||
        type == byte.class || type == Byte.class ||
        type == char.class ||  type == Character.class ||
        BigInteger.class.isAssignableFrom(type);
  }


  /**
   * {@inheritDoc}
   * <p>
   * Converts the integral value to a {@link BitSet} and delegates formatting to the registered {@code BitSet}
   * formatter.
   */
  @Override
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Object value)
  {
    return context.format(
        switch(value) {
          case Long _long -> BitSet.valueOf(new long[] { _long });
          case Integer _int -> BitSet.valueOf(new long[] { _int & 0xFFFFFFFFL });
          case Short _short -> BitSet.valueOf(new long[] { _short & 0xFFFF });
          case Byte _byte -> BitSet.valueOf(new long[] { _byte & 0xFF });
          case Character _char -> BitSet.valueOf(new long[] { _char });
          default -> toBitSet((BigInteger)value);
        },
        BitSet.class);
  }


  /**
   * Converts a {@link BigInteger} value to a {@link BitSet}.
   *
   * @param bigInteger  the value to convert, not {@code null}
   *
   * @return  a {@code BitSet} representing the same bits, never {@code null}
   */
  @Contract(pure = true)
  private @NotNull BitSet toBitSet(@NotNull BigInteger bigInteger)
  {
    final var bytes = bigInteger.toByteArray(); // big-endian, two's complement

    // reverse to little-endian (BitSet.valueOf expects little-endian)
    for(int i = 0, n = bytes.length - 1; i < n; i++, n--)
    {
      final var tmp = bytes[i];
      bytes[i] = bytes[n];
      bytes[n] = tmp;
    }

    return BitSet.valueOf(bytes);
  }
}
