/*
 * Copyright 2022 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.TextPartFactory.nullText;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
class BooleanSupplierFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertFormatterForType(new BooleanSupplierFormatter(), BooleanSupplier.class);
  }


  @Test
  public void testFormat()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new BooleanSupplierFormatter()), NO_CACHE_INSTANCE)
        .setLocale(ROOT)
        .getMessageAccessor();

    assertEquals(nullText(), format(messageAccessor, null));
    assertEquals(noSpaceText("true"), format(messageAccessor, (BooleanSupplier)() -> true));
    assertEquals(noSpaceText("false"), format(messageAccessor, (BooleanSupplier)() -> false));
  }
}
