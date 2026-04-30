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
import org.junit.jupiter.api.*;

import java.math.BigInteger;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.math.BigInteger.ONE;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;


/**
 * @author Jeroen Gremmen
 * @since 0.21.1
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
        .create(
            createFormatterService(new BitmaskFormatter(), new BitSetFormatter(), new NumberFormatter()),
            NO_CACHE_INSTANCE)
        .setLocale(ROOT);
  }


  @Test
  @DisplayName("Long value with lsb1st ordering")
  void testLongLsb1st()
  {
    // 0x8000_0000_0000_00A5L = bits 0, 2, 5, 7, 63 are set
    assertEquals("b0, b2, b5, b7 and b63", messageSupport
        .message("%{v,format:bitmask,bitset:lsb1st,list-sep-last:' and ',0:b0,2:b2,5:b5,7:b7,63:b63}")
        .with("v", 0x8000_0000_0000_00A5L)
        .format());
  }


  @Test
  @DisplayName("Long value with msb1st ordering")
  void testLongMsb1st()
  {
    // 0x8000_0000_0000_00A5L = bits 0, 2, 5, 7, 63 are set
    assertEquals("b63, b7, b5, b2, b0", messageSupport
        .message("%{v,format:bitmask,bitset:msb1st,0:b0,2:b2,5:b5,7:b7,63:b63}")
        .with("v", 0x8000_0000_0000_00A5L)
        .format());
  }


  @Test
  @DisplayName("Int value with lsb1st ordering")
  void testIntLsb1st()
  {
    // 0x8000_00C3 = bits 0, 1, 6, 7, 31 are set
    assertEquals("b0, b1, b6, b7, b31", messageSupport
        .message("%{v,format:bitmask,bitset:lsb1st,0:b0,1:b1,6:b6,7:b7,31:b31}")
        .with("v", (int)0x8000_00C3L)
        .format());
  }


  @Test
  @DisplayName("Int value with msb1st ordering")
  void testIntMsb1st()
  {
    // 0x8000_00C3 = bits 0, 1, 6, 7, 31 are set
    assertEquals("b31, b7, b6, b1, b0", messageSupport
        .message("%{v,format:bitmask,bitset:msb1st,0:b0,1:b1,6:b6,7:b7,31:b31}")
        .with("v", (int)0x8000_00C3L)
        .format());
  }


  @Test
  @DisplayName("Short value with lsb1st ordering")
  void testShortLsb1st()
  {
    // 0x800A = bits 1, 3, 15 are set
    assertEquals("b1, b3, b15", messageSupport
        .message("%{v,format:bitmask,bitset:lsb1st,0:b0,1:b1,2:b2,3:b3,15:b15}")
        .with("v", (short)0x800A)
        .format());
  }


  @Test
  @DisplayName("Short value with msb1st ordering")
  void testShortMsb1st()
  {
    // 0x800A = bits 1, 3, 15 are set
    assertEquals("b15, b3, b1", messageSupport
        .message("%{v,format:bitmask,bitset:msb1st,0:b0,1:b1,2:b2,3:b3,15:b15}")
        .with("v", (short)0x800A)
        .format());
  }


  @Test
  @DisplayName("Byte value with lsb1st ordering")
  void testByteLsb1st()
  {
    // 0x95 = 0b1001_0101 = bits 0, 2, 4, 7 are set
    assertEquals("b0, b2, b4, b7", messageSupport
        .message("%{v,format:bitmask,bitset:lsb1st,0:b0,2:b2,4:b4,7:b7}")
        .with("v", (byte)0x95)
        .format());
  }


  @Test
  @DisplayName("Byte value with msb1st ordering")
  void testByteMsb1st()
  {
    // 0x95 = 0b1001_0101 = bits 0, 2, 4, 7 are set
    assertEquals("b7, b4, b2, b0", messageSupport
        .message("%{v,format:bitmask,bitset:msb1st,0:b0,2:b2,4:b4,7:b7}")
        .with("v", (byte)0x95)
        .format());
  }


  @Test
  @DisplayName("Char value with lsb1st ordering")
  @SuppressWarnings("UnnecessaryUnicodeEscape")
  void testCharLsb1st()
  {
    // '\u8041' = bits 0, 6, 15 are set
    assertEquals("b0, b6, b15", messageSupport
        .message("%{v,format:bitmask,bitset:lsb1st,0:b0,1:b1,6:b6,7:b7,15:b15,16:b16}")
        .with("v", '\u8041')
        .format());
  }


  @Test
  @DisplayName("Char value with msb1st ordering")
  @SuppressWarnings("UnnecessaryUnicodeEscape")
  void testCharMsb1st()
  {
    // '\u8041' = bits 0, 6, 15 are set
    assertEquals("b15, b6, b0", messageSupport
        .message("%{v,format:bitmask,bitset:msb1st,0:b0,1:b1,6:b6,7:b7,15:b15,16:b16}")
        .with("v", '\u8041')
        .format());
  }


  @Test
  @DisplayName("BigInteger value exceeding long range with lsb1st ordering")
  void testBigIntegerLsb1st()
  {
    // 2^64 + 2^1 + 2^0 = a value exceeding Long.MAX_VALUE with bits 0, 1, 64 set
    final var bigValue = ONE.shiftLeft(64).or(BigInteger.valueOf(3));

    assertEquals("b0, b1, b64", messageSupport
        .message("%{v,format:bitmask,bitset:lsb1st,0:b0,1:b1,2:b2,64:b64}")
        .with("v", bigValue)
        .format());
  }


  @Test
  @DisplayName("BigInteger value exceeding long range with msb1st ordering")
  void testBigIntegerMsb1st()
  {
    // 2^64 + 2^1 + 2^0 = a value exceeding Long.MAX_VALUE with bits 0, 1, 64 set
    final var bigValue = ONE.shiftLeft(64).or(BigInteger.valueOf(3));

    assertEquals("b64, b1, b0", messageSupport
        .message("%{v,format:bitmask,bitset:msb1st,0:b0,1:b1,2:b2,64:b64}")
        .with("v", bigValue)
        .format());
  }


  @Test
  @DisplayName("Negative int value preserves unsigned bit pattern")
  void testNegativeInt()
  {
    // -1 as int = 0xFFFFFFFF = all 32 bits set
    assertEquals("b0, b7, b15, b31", messageSupport
        .message("%{v,format:bitmask,bitset:lsb1st,0:b0,7:b7,15:b15,31:b31}")
        .with("v", -1)
        .format());
  }


  @Test
  @DisplayName("Negative byte value preserves unsigned bit pattern")
  void testNegativeByte()
  {
    // -1 as byte = 0xFF = all 8 bits set
    assertEquals("b0, b1, b2, b3, b4, b5, b6, b7", messageSupport
        .message("%{v,format:bitmask,bitset:lsb1st,0:b0,1:b1,2:b2,3:b3,4:b4,5:b5,6:b6,7:b7}")
        .with("v", (byte)-1)
        .format());
  }


  @Test
  @DisplayName("Negative short value preserves unsigned bit pattern")
  void testNegativeShort()
  {
    // -1 as short = 0xFFFF = all 16 bits set
    assertEquals("b0, b7, b15", messageSupport
        .message("%{v,format:bitmask,bitset:lsb1st,0:b0,7:b7,15:b15}")
        .with("v", (short)-1)
        .format());
  }


  @Test
  @DisplayName("BigInteger with high bit set far beyond long range")
  void testBigIntegerHighBit()
  {
    // 2^100 + 2^0 = bits 0 and 100 set
    final var bigValue = ONE.shiftLeft(100).or(ONE);

    assertEquals("b100, b0", messageSupport
        .message("%{v,format:bitmask,bitset:msb1st,0:b0,100:b100}")
        .with("v", bigValue)
        .format());
  }
}
