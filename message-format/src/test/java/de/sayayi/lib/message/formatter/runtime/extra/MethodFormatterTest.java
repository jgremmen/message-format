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
package de.sayayi.lib.message.formatter.runtime.extra;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Locale.UK;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("Method formatter")
class MethodFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormattableTypes() {
    assertFormatterForType(new MethodFormatter(), Method.class);
  }


  @Test
  void testFormat() throws Exception
  {
    val message = MessageSupportFactory
        .create(createFormatterService(new MethodFormatter()), NO_CACHE_INSTANCE)
        .setLocale(UK)
        .message("%{m} %{m,method:'name'} %{m,method:'return-type'} %{m,method:'class'}");

    assertEquals("protected static " + Set.class.getName() + " " + MethodFormatterTest.class.getName() +
        ".dummy() dummy " + Set.class.getSimpleName() + "<" + String.class.getSimpleName() + "> " +
        MethodFormatterTest.class.getName(), message.with("m",
        MethodFormatterTest.class.getDeclaredMethod("dummy")).format());
  }


  protected static Set<String> dummy() {
    return null;
  }
}
