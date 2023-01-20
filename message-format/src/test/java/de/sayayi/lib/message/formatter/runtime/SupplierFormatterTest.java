/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.data.map.MapKeyName;
import de.sayayi.lib.message.data.map.MapValueString;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.named.BoolFormatter;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class SupplierFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testBooleanSupplier()
  {
    val context = new MessageContext(
        createFormatterService(new BoolFormatter(), new BooleanSupplierFormatter()),
        NO_CACHE_INSTANCE, "de-DE");

    assertEquals(noSpaceText("wahr"), format(context, (BooleanSupplier) () -> true));
  }


  @Test
  public void testLongSupplier()
  {
    val context = new MessageContext(
        createFormatterService(new NumberFormatter(), new LongSupplierFormatter()), NO_CACHE_INSTANCE, "en");

    assertEquals(noSpaceText("1,234,567,890"), format(context, (LongSupplier) () -> 1234567890L,
        singletonMap(new MapKeyName("number"), new MapValueString("###,###,###,###"))));
  }
}