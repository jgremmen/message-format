/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.named.extra;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.runtime.LongSupplierFormatter;
import de.sayayi.lib.message.formatter.runtime.OptionalIntFormatter;
import de.sayayi.lib.message.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.parameter.value.ConfigValueNumber;
import de.sayayi.lib.message.parameter.value.ConfigValueString;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.OptionalInt;
import java.util.function.LongSupplier;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.*;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class BitsFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormatterConfig()
  {
    final BitsFormatter formatter = new BitsFormatter();

    assertTrue(formatter.getFormattableTypes().isEmpty());
    assertEquals("bits", formatter.getName());
  }


  @Test
  void testByte()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new BitsFormatter()), NO_CACHE_INSTANCE)
        .getAccessor();

    assertEquals(noSpaceText("11111111"), format(accessor, (byte)0xff, "bits"));
    assertEquals(noSpaceText("00000000"), format(accessor, (byte)0, "bits"));
    assertEquals(noSpaceText("10101010"), format(accessor, (byte)0xaa, "bits"));
    assertEquals(noSpaceText("01010101"), format(accessor, (byte)0x55, "bits"));
    assertEquals(noSpaceText("10101"), format(accessor, (byte)0x15,
        singletonMap(new ConfigKeyName("bits"), new ConfigValueString("auto")), "bits"));
    assertEquals(noSpaceText("0"), format(accessor, (byte)0,
        singletonMap(new ConfigKeyName("bits"), new ConfigValueString("auto")), "bits"));
    assertEquals(noSpaceText("101"), format(accessor, (byte)0x15,
        singletonMap(new ConfigKeyName("bits"), new ConfigValueNumber(3)), "bits"));
    assertEquals(nullText(), format(accessor, (Object)null, "bits"));
  }


  @Test
  void testShort()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new BitsFormatter()), NO_CACHE_INSTANCE)
        .getAccessor();

    assertEquals(noSpaceText("1111111111111111"), format(accessor, (short)0xffff, "bits"));
    assertEquals(noSpaceText("0000000000000000"), format(accessor, (short)0, "bits"));
    assertEquals(noSpaceText("1010101010101010"), format(accessor, (short)0xaaaa, "bits"));
    assertEquals(noSpaceText("0101010101010101"), format(accessor, (short)0x5555, "bits"));
    assertEquals(noSpaceText("101010101"), format(accessor, (short)0x155,
        singletonMap(new ConfigKeyName("bits"), new ConfigValueString("auto")), "bits"));
    assertEquals(noSpaceText("0"), format(accessor, (short)0,
        singletonMap(new ConfigKeyName("bits"), new ConfigValueString("auto")), "bits"));
    assertEquals(noSpaceText("010110"), format(accessor, (byte)0x456,
        singletonMap(new ConfigKeyName("bits"), new ConfigValueNumber(6)), "bits"));
  }


  @Test
  void testBitInteger()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new BitsFormatter()), NO_CACHE_INSTANCE)
        .getAccessor();

    assertEquals(noSpaceText("101111000110000101001110"), format(accessor, new BigInteger("12345678"),
        singletonMap(new ConfigKeyName("bits"), new ConfigValueString("auto")), "bits"));
    assertEquals(noSpaceText("0"), format(accessor, BigInteger.ZERO,
        singletonMap(new ConfigKeyName("bits"), new ConfigValueString("auto")), "bits"));
    assertEquals(noSpaceText("01001110"), format(accessor, new BigInteger("12345678"),
        singletonMap(new ConfigKeyName("bits"), new ConfigValueNumber(8)), "bits"));
    assertEquals(noSpaceText("1010110101000111000101000100111010000000100001000101111000111101100"),
        format(accessor, new BigInteger("99887766554433221100"),
            singletonMap(new ConfigKeyName("bits"), new ConfigValueNumber(67)), "bits"));
  }


  @Test
  void testWrapper()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new BitsFormatter(), new LongSupplierFormatter(),
            new OptionalIntFormatter()), NO_CACHE_INSTANCE)
        .getAccessor();

    assertEquals(noSpaceText("101111000110000101001110"), format(accessor, (LongSupplier)() -> 12345678,
        singletonMap(new ConfigKeyName("bits"), new ConfigValueString("auto")), "bits"));
    assertEquals(noSpaceText("01001110"), format(accessor, OptionalInt.of(12345678),
        singletonMap(new ConfigKeyName("bits"), new ConfigValueNumber(8)), "bits"));
    assertEquals(nullText(), format(accessor, (Object)null, "bits"));
    assertEquals(emptyText(), format(accessor, OptionalInt.empty(), "bits"));
  }
}
