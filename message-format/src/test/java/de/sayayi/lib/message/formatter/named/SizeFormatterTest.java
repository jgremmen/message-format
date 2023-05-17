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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.runtime.ArrayFormatter;
import de.sayayi.lib.message.formatter.runtime.ByteArrayFormatter;
import de.sayayi.lib.message.formatter.runtime.IterableFormatter;
import de.sayayi.lib.message.formatter.runtime.MapFormatter;
import lombok.val;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Locale.UK;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
class SizeFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormat()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new SizeFormatter(), new IterableFormatter(),
            new ArrayFormatter(), new MapFormatter()), NO_CACHE_INSTANCE)
        .setLocale(UK);
    val message = messageSupport
        .message("%{c,size} %{c,size,0:'empty',1:'singleton',:'multiple'}").getMessage();

    assertEquals("0 empty", messageSupport.message(message)
        .with("c", emptyList()).format());

    assertEquals("1 singleton", messageSupport.message(message)
        .with("c", singletonMap("a", "b")).format());

    assertEquals("4 multiple", messageSupport.message(message)
        .with("c", new int[] { 4, -45, 8, 1 }).format());
  }


  @Test
  void testFormatNoSizeQueryable()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new SizeFormatter()), NO_CACHE_INSTANCE)
        .setLocale(UK);

    assertEquals("", messageSupport.message("%{c,size}").with("c", true).format());
  }


  @Test
  void testFormatDefaultFormatter()
  {
    val messageSupport = MessageSupportFactory.create(
        createFormatterService(new SizeFormatter(), new ByteArrayFormatter()), NO_CACHE_INSTANCE);

    assertEquals("2", messageSupport.message("%{c,size}")
        .with("c", new byte[] { 'a', 'b' }).format());
  }
}
