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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class FieldFormatterTest extends AbstractFormatterTest
{
  private MessageContext context;


  @BeforeEach
  public void init() {
    context = new MessageContext(createFormatterService(new FieldFormatter()), NO_CACHE_INSTANCE);
  }


  @Test
  public void testFormattableTypes() {
    assertFormatterForType(new FieldFormatter(), Field.class);
  }


  @Test
  public void testFormatV1() throws Exception
  {
    val v1 = Dummy.class.getDeclaredField("v1");
    val factory = context.getMessageFactory();

    assertEquals("boolean[]", factory.parse("%{f,field:T}")
        .format(context, context.parameters().with("f", v1)));
    assertEquals("v1", factory.parse("%{f,field:N}")
        .format(context, context.parameters().with("f", v1)));
    assertEquals("private static boolean[] v1", factory.parse("%{f}")
        .format(context, context.parameters().with("f", v1)));
    assertEquals("boolean[] v1", factory.parse("%{f,field:TN}")
        .format(context, context.parameters().with("f", v1)));
  }


  @Test
  public void testFormatV2() throws Exception
  {
    val v2 = Dummy.class.getDeclaredField("v2");
    val factory = context.getMessageFactory();

    assertEquals("Map<String, Integer>", factory.parse("%{f,field:Tju}")
        .format(context, context.parameters().with("f", v2)));
    assertEquals("v2", factory.parse("%{f,field:N}")
        .format(context, context.parameters().with("f", v2)));
    assertEquals("public final java.util.Map<String, Integer> v2",
        factory.parse("%{f,field:'jTMN'}")
        .format(context, context.parameters().with("f", v2)));
    assertEquals("java.util.Map<java.lang.String, java.lang.Integer> v2",
        factory.parse("%{f,field:TN}")
        .format(context, context.parameters().with("f", v2)));
  }


  @SuppressWarnings("unused")
  static class Dummy
  {
    private static boolean[] v1;
    public final Map<String,Integer> v2 = new HashMap<>();
  }
}
