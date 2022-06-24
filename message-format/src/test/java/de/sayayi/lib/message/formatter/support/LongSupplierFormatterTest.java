/*
 * Copyright 2022 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.function.LongSupplier;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
class LongSupplierFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertFormatterForType(new LongSupplierFormatter(), LongSupplier.class);
  }


  @Test
  public void testFormat()
  {
    final LongSupplierFormatter formatter = new LongSupplierFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(),
        NO_CACHE_INSTANCE, Locale.ROOT);
    final MessageContext.Parameters parameters = context.noParameters();

    assertEquals(nullText(), formatter.format(context, null, null, parameters, null));
    assertEquals(noSpaceText("0"),
        formatter.format(context, (LongSupplier)() -> 0, null, parameters, null));
    assertEquals(noSpaceText(Long.toString(MAX_VALUE)),
        formatter.format(context, (LongSupplier)() -> MAX_VALUE, null, parameters, null));
    assertEquals(noSpaceText(Long.toString(MIN_VALUE)),
        formatter.format(context, (LongSupplier)() -> MIN_VALUE, null, parameters, null));
    assertEquals(noSpaceText("123456789"),
        formatter.format(context, (LongSupplier)() -> 123456789L, null, parameters, null));
  }
}