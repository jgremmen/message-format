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

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;

import java.util.BitSet;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;


/**
 * @author Jeroen Gremmen
 * @since 0.21.1
 */
@DisplayName("BitSet formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@TestInstance(PER_CLASS)
final class BitSetFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeAll
  void init()
  {
    messageSupport = MessageSupportFactory
        .create(createFormatterService(new BitSetFormatter()).seal(), NO_CACHE_INSTANCE)
        .setLocale(ROOT);
  }


  @Test
  @DisplayName("Empty BitSet")
  void testEmptyBitSet()
  {
    assertEquals("", messageSupport
        .message("%{bs,0:zero,1:one,2:two}")
        .with("bs", new BitSet())
        .format());
  }


  @Test
  @DisplayName("Set bits with no matching map keys are skipped")
  void testNoMatchingKeys()
  {
    val bitSet = new BitSet();
    bitSet.set(5);
    bitSet.set(10);
    bitSet.set(15);

    // map keys 0, 1, 2 don't match any set bits (5, 10, 15)
    assertEquals("", messageSupport
        .message("%{bs,0:zero,1:one,2:two}")
        .with("bs", bitSet)
        .format());
  }


  @Test
  @DisplayName("Only bits with matching map keys are included in output")
  void testPartiallyMatchingKeys()
  {
    val bitSet = new BitSet();

    bitSet.set(1);
    bitSet.set(3);
    bitSet.set(5);
    bitSet.set(7);

    // only bits 1, 3, 5 have matching map keys; bit 7 is skipped
    assertEquals("one, three, five", messageSupport
        .message("%{bs,1:one,3:three,5:five}")
        .with("bs", bitSet)
        .format());
  }


  @Test
  @DisplayName("Unique filter removes duplicate values")
  void testUniqueValues()
  {
    val bitSet = new BitSet();

    bitSet.set(1);
    bitSet.set(2);
    bitSet.set(3);
    bitSet.set(5);
    bitSet.set(8);

    // bits 1, 3, 5 all map to 'same'; with list-unique:true duplicates are suppressed
    assertEquals("same, two, eight", messageSupport
        .message("%{bs,list-unique:true,(1,3,5):same,2:two,8:eight}")
        .with("bs", bitSet)
        .format());
  }


  @Test
  @DisplayName("Most significant set bit first ordering (msb-set)")
  void testMsbSetOrdering()
  {
    val bitSet = new BitSet();

    bitSet.set(0);
    bitSet.set(2);
    bitSet.set(4);
    bitSet.set(6);
    bitSet.set(8);

    assertEquals("eight, six, four, two, zero", messageSupport
        .message("%{bs,bitset:msb-set,0:zero,2:two,4:four,6:six,8:eight}")
        .with("bs", bitSet)
        .format());
  }


  @Test
  @DisplayName("Least significant set bit first ordering (lsb-set)")
  void testLsbSetOrdering()
  {
    val bitSet = new BitSet();

    bitSet.set(0);
    bitSet.set(2);
    bitSet.set(4);
    bitSet.set(6);
    bitSet.set(8);

    assertEquals("zero, two, four, six, eight", messageSupport
        .message("%{bs,bitset:lsb-set,0:zero,2:two,4:four,6:six,8:eight}")
        .with("bs", bitSet)
        .format());
  }


  @Test
  @DisplayName("LSB bits with default true/false messages")
  void testLsbBitsDefault()
  {
    val bitSet = new BitSet();

    bitSet.set(0);
    bitSet.set(3);
    bitSet.set(5);

    // bits: 0=1, 1=0, 2=0, 3=1, 4=0, 5=1 → "100101"
    assertEquals("100101", messageSupport
        .message("%{bs,bitset:lsb-bits}")
        .with("bs", bitSet)
        .format());
  }


  @Test
  @DisplayName("MSB bits with default true/false messages")
  void testMsbBitsDefault()
  {
    val bitSet = new BitSet();

    bitSet.set(0);
    bitSet.set(3);
    bitSet.set(5);

    // bits: 5=1, 4=0, 3=1, 2=0, 1=0, 0=1 → "101001"
    assertEquals("101001", messageSupport
        .message("%{bs,bitset:msb-bits}")
        .with("bs", bitSet)
        .format());
  }


  @Test
  @DisplayName("LSB bits with custom true/false messages")
  void testLsbBitsCustom()
  {
    val bitSet = new BitSet();

    bitSet.set(1);
    bitSet.set(2);
    bitSet.set(4);

    // bits: 0=X, 1=O, 2=O, 3=X, 4=O → "XOOXO"
    assertEquals("XOOXO", messageSupport
        .message("%{bs,bitset:lsb-bits,bit0:X,bit1:O}")
        .with("bs", bitSet)
        .format());
  }


  @Test
  @DisplayName("MSB bits with custom true/false messages")
  void testMsbBitsCustom()
  {
    val bitSet = new BitSet();

    bitSet.set(1);
    bitSet.set(2);
    bitSet.set(4);

    // bits: 4=O, 3=X, 2=O, 1=O, 0=X → "OXOOX"
    assertEquals("OXOOX", messageSupport
        .message("%{bs,bitset:msb-bits,bit0:X,bit1:O}")
        .with("bs", bitSet)
        .format());
  }
}
