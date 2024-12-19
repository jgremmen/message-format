/*
 * Copyright 2021 Jeroen Gremmen
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("Iterable formatter")
public class IterableFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertFormatterForType(new IterableFormatter(), Iterable.class);
  }


  @Test
  public void testObjectArray()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new IterableFormatter()), NO_CACHE_INSTANCE)
        .setLocale("de-DE")
        .getMessageAccessor();

    assertEquals(noSpaceText("Test, true, -6"),
        format(messageAccessor, asList("Test", true, null, -6)));
  }


  @Test
  public void testEmptyOrNullCollection()
  {
    val message = MessageSupportFactory
        .create(createFormatterService(new IterableFormatter()), NO_CACHE_INSTANCE)
        .message("%{c,null:null,empty:empty}");

    assertEquals("null", message.with("c", null).format());
    assertEquals("empty", message.with("c", Set.of()).format());
  }


  @Test
  public void testSeparator()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new IterableFormatter()), NO_CACHE_INSTANCE);

    assertEquals("1, 2, 3, 4 and 5", messageSupport
        .message("%{c,list-sep:', ',list-sep-last:' and '}").with("c", List.of(1, 2, 3, 4, 5)).format());

    assertEquals("1.2.3.4.5", messageSupport
        .message("%{c,list-sep:'.'}").with("c", List.of(1, 2, 3, 4, 5)).format());
  }
}
