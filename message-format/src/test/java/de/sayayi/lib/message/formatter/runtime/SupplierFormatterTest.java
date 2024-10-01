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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.named.BoolFormatter;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("Supplier formatter")
class SupplierFormatterTest extends AbstractFormatterTest
{
  @Test
  void testBooleanSupplier()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new BoolFormatter(), new BooleanSupplierFormatter()),
            NO_CACHE_INSTANCE)
        .getMessageAccessor();

    assertEquals(noSpaceText("true"), format(messageAccessor, (BooleanSupplier)() -> true));
  }


  @Test
  void testLongSupplier()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new NumberFormatter(), new LongSupplierFormatter()),
            NO_CACHE_INSTANCE)
        .setLocale("en")
        .getMessageAccessor();

    assertEquals(noSpaceText("1,234,567,890"),
        format(messageAccessor, (LongSupplier)() -> 1234567890L,
            Map.of(new ConfigKeyName("number"), new ConfigValueString("###,###,###,###"))));
  }
}
