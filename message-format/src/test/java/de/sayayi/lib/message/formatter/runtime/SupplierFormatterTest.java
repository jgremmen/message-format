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
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.map.MapKeyName;
import de.sayayi.lib.message.data.map.MapValueString;
import de.sayayi.lib.message.formatter.GenericFormatterService;
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
public class SupplierFormatterTest
{
  @Test
  public void testBooleanSupplier()
  {
    GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new BoolFormatter());
    registry.addFormatter(new BooleanSupplierFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, "de-DE");
    Parameters noParameters = context.noParameters();

    Object value = (BooleanSupplier) () -> true;

    assertEquals(noSpaceText("wahr"), registry.getFormatter(null, value.getClass())
        .format(context, value, null, noParameters, null));
  }


  @Test
  public void testLongSupplier()
  {
    GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new NumberFormatter());
    registry.addFormatter(new LongSupplierFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, "en");
    Parameters noParameters = context.noParameters();

    Object value = (LongSupplier) () -> 1234567890L;

    assertEquals(noSpaceText("1,234,567,890"), registry.getFormatter(null, value.getClass())
        .format(context, value, null, noParameters,
            new DataMap(singletonMap(new MapKeyName("number"), new MapValueString("###,###,###,###")))));
  }
}