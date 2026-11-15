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

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.parameter.runtime.BitSetFormatter;
import de.sayayi.lib.message.formatter.parameter.runtime.NumberFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.stream.Stream;

import static java.math.BigInteger.ONE;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link BitmaskFormatter}: bit order (lsb and msb first) for all integral types and
 * {@link java.math.BigInteger}, including negative values and high bits.
 *
 * @author Jeroen Gremmen
 * @since 0.22.0
 */
@DisplayName("'bitmask' formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@TestInstance(PER_CLASS)
final class BitmaskFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeAll
  void init()
  {
    messageSupport = MessageSupportFactory
        .create(createFormatterService(new BitmaskFormatter(), new BitSetFormatter(), new NumberFormatter()).seal())
        .setLocale(ROOT);
  }


  @SuppressWarnings("UnnecessaryUnicodeEscape")
  static Stream<Arguments> bitmasks()
  {
    // 0x8000_0000_0000_00A5L = bits 0, 2, 5, 7, 63 are set
    final var longValue = 0x8000_0000_0000_00A5L;
    // 0x8000_00C3 = bits 0, 1, 6, 7, 31 are set
    final var intValue = (int)0x8000_00C3L;
    // 0x800A = bits 1, 3, 15 are set
    final var shortValue = (short)0x800A;
    // 0x95 = 0b1001_0101 = bits 0, 2, 4, 7 are set
    final var byteValue = (byte)0x95;
    // '\u8041' = bits 0, 6, 15 are set
    final var charValue = '\u8041';
    // 2^64 + 2^1 + 2^0 = a value exceeding Long.MAX_VALUE with bits 0, 1, 64 set
    final var bigValue = ONE.shiftLeft(64).or(BigInteger.valueOf(3));

    return Stream.of(
        arguments("Long value with lsb1st ordering",
            "%{v,format:bitmask,bitset:lsb-set,list-sep-last:' and ',0:b0,2:b2,5:b5,7:b7,63:b63}", longValue,
            "b0, b2, b5, b7 and b63"),
        arguments("Long value with msb1st ordering",
            "%{v,format:bitmask,bitset:msb-set,0:b0,2:b2,5:b5,7:b7,63:b63}", longValue,
            "b63, b7, b5, b2, b0"),
        arguments("Int value with lsb1st ordering",
            "%{v,format:bitmask,bitset:lsb-set,0:b0,1:b1,6:b6,7:b7,31:b31}", intValue,
            "b0, b1, b6, b7, b31"),
        arguments("Int value with msb1st ordering",
            "%{v,format:bitmask,bitset:msb-set,0:b0,1:b1,6:b6,7:b7,31:b31}", intValue,
            "b31, b7, b6, b1, b0"),
        arguments("Short value with lsb1st ordering",
            "%{v,format:bitmask,bitset:lsb-set,0:b0,1:b1,2:b2,3:b3,15:b15}", shortValue,
            "b1, b3, b15"),
        arguments("Short value with msb1st ordering",
            "%{v,format:bitmask,bitset:msb-set,0:b0,1:b1,2:b2,3:b3,15:b15}", shortValue,
            "b15, b3, b1"),
        arguments("Byte value with lsb1st ordering",
            "%{v,format:bitmask,bitset:lsb-set,0:b0,2:b2,4:b4,7:b7}", byteValue,
            "b0, b2, b4, b7"),
        arguments("Byte value with msb1st ordering",
            "%{v,format:bitmask,bitset:msb-set,0:b0,2:b2,4:b4,7:b7}", byteValue,
            "b7, b4, b2, b0"),
        arguments("Char value with lsb1st ordering",
            "%{v,format:bitmask,bitset:lsb-set,0:b0,1:b1,6:b6,7:b7,15:b15,16:b16}", charValue,
            "b0, b6, b15"),
        arguments("Char value with msb1st ordering",
            "%{v,format:bitmask,bitset:msb-set,0:b0,1:b1,6:b6,7:b7,15:b15,16:b16}", charValue,
            "b15, b6, b0"),
        arguments("BigInteger value exceeding long range with lsb1st ordering",
            "%{v,format:bitmask,bitset:lsb-set,0:b0,1:b1,2:b2,64:b64}", bigValue,
            "b0, b1, b64"),
        arguments("BigInteger value exceeding long range with msb1st ordering",
            "%{v,format:bitmask,bitset:msb-set,0:b0,1:b1,2:b2,64:b64}", bigValue,
            "b64, b1, b0"),
        // -1 as int = 0xFFFFFFFF = all 32 bits set
        arguments("Negative int value preserves unsigned bit pattern",
            "%{v,format:bitmask,bitset:lsb-set,0:b0,7:b7,15:b15,31:b31}", -1,
            "b0, b7, b15, b31"),
        // -1 as byte = 0xFF = all 8 bits set
        arguments("Negative byte value preserves unsigned bit pattern",
            "%{v,format:bitmask,bitset:lsb-set,0:b0,1:b1,2:b2,3:b3,4:b4,5:b5,6:b6,7:b7}", (byte)-1,
            "b0, b1, b2, b3, b4, b5, b6, b7"),
        // -1 as short = 0xFFFF = all 16 bits set
        arguments("Negative short value preserves unsigned bit pattern",
            "%{v,format:bitmask,bitset:lsb-set,0:b0,7:b7,15:b15}", (short)-1,
            "b0, b7, b15"),
        // 2^100 + 2^0 = bits 0 and 100 set
        arguments("BigInteger with high bit set far beyond long range",
            "%{v,format:bitmask,bitset:msb-set,0:b0,100:b100}", ONE.shiftLeft(100).or(ONE),
            "b100, b0")
    );
  }


  @ParameterizedTest(name = "{0}")
  @DisplayName("Bits are formatted in the configured order")
  @MethodSource("bitmasks")
  void bitmask(@SuppressWarnings("unused") String name, @Language("MessageFormat") String formatString,
                   Object value, String expected)
  {
    assertEquals(
        expected,
        messageSupport
            .message(formatString)
            .with("v", value)
            .format());
  }
}
