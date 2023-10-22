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

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("Field formatter")
class FieldFormatterTest extends AbstractFormatterTest
{
  private MessageSupport context;


  @BeforeEach
  void init() {
    context = MessageSupportFactory.create(createFormatterService(new FieldFormatter()), NO_CACHE_INSTANCE);
  }


  @Test
  void testFormattableTypes() {
    assertFormatterForType(new FieldFormatter(), Field.class);
  }


  @Test
  void testFormatV1() throws Exception
  {
    val v1 = Dummy.class.getDeclaredField("v1");

    assertEquals("boolean[]", context.message("%{f,field:T}").with("f", v1).format());
    assertEquals("v1", context.message("%{f,field:N}").with("f", v1).format());
    assertEquals("private static boolean[] v1", context.message("%{f}").with("f", v1).format());
    assertEquals("boolean[] v1", context.message("%{f,field:TN}").with("f", v1).format());
  }


  @Test
  void testFormatV2() throws Exception
  {
    val v2 = Dummy.class.getDeclaredField("v2");

    assertEquals("Map<String, Integer>", context.message("%{f,field:Tju}").with("f", v2).format());
    assertEquals("v2", context.message("%{f,field:N}").with("f", v2).format());
    assertEquals("public final java.util.Map<String, Integer> v2",
        context.message("%{f,field:'jTMN'}").with("f", v2).format());
    assertEquals("java.util.Map<java.lang.String, java.lang.Integer> v2",
        context.message("%{f,field:TN}").with("f", v2).format());
  }




  @SuppressWarnings("unused")
  static class Dummy
  {
    private static boolean[] v1;
    public final Map<String,Integer> v2 = new HashMap<>();
  }
}
