/*
 * Copyright 2023 Jeroen Gremmen
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
import de.sayayi.lib.message.formatter.named.StringFormatter;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.8.4
 */

class OptionalFormatterTest extends AbstractFormatterTest
{
  @Test
  void testEmptyNull()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new OptionalFormatter(), new StringFormatter()),
            NO_CACHE_INSTANCE)
        .setLocale(ROOT);

    assertEquals("empty", messageSupport
        .message("%{opt,empty:empty,null:null}")
        .with("opt", Optional.empty())
        .format());


    assertEquals("null", messageSupport
        .message("%{opt,empty:empty,null:null}")
        .with("opt", null)
        .format());
  }
}
