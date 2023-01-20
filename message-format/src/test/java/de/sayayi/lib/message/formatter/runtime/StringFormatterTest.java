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
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.formatter.named.StringFormatter;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.lang.annotation.RetentionPolicy;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class StringFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    assertFormatterForType(new StringFormatter(), CharSequence.class);
    assertFormatterForType(new StringFormatter(), char[].class);
  }


  @Test
  public void testFormat()
  {
    val context = new MessageContext(new GenericFormatterService(), NO_CACHE_INSTANCE);

    assertEquals(noSpaceText("text"), format(context, " text "));
    assertEquals(noSpaceText("RUNTIME"), format(context, RetentionPolicy.RUNTIME));
    assertEquals(noSpaceText("hello"), format(context, new Object() {
      @Override
      public String toString() {
        return " hello";
      }
    }));
  }


  @Test
  public void testFormatter()
  {
    val context = new MessageContext(new GenericFormatterService(), NO_CACHE_INSTANCE);
    val parameters = context.parameters()
        .with("a", " a test ")
        .with("b", null)
        .with("c", 1234);
    val msg = context.getMessageFactory().parse("This is %{a} %{b} %{c}");

    assertEquals("This is a test 1234", msg.format(context, parameters));
  }


  @Test
  public void testFormatterWithMap()
  {
    val messageFactory = NO_CACHE_INSTANCE;
    val context = new MessageContext(new GenericFormatterService(), messageFactory);

    val parameters = context.parameters()
        .with("empty", "")
        .with("null", null)
        .with("spaces", "  ")
        .with("text", "hello  ");

    assertEquals("", messageFactory.parse("%{empty,!empty:nok}").format(context, parameters));
    assertEquals("ok", messageFactory.parse("%{empty,empty:ok}").format(context, parameters));
    assertEquals("ok", messageFactory.parse("%{null,empty:nok,null:ok}").format(context, parameters));
    assertEquals("ok", messageFactory.parse("%{null,empty:ok}").format(context, parameters));
    assertEquals("ok", messageFactory.parse("%{spaces,empty:ok}").format(context, parameters));
    assertEquals("ok", messageFactory.parse("%{spaces,!null:ok}").format(context, parameters));
    assertEquals("hello!", messageFactory.parse("%{text,null:nok,!empty:'%{text}!'}")
        .format(context, parameters));
    assertEquals("hello!", messageFactory.parse("%{text,!null:'%{text}!'}").format(context, parameters));
  }
}