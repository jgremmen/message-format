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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.GenericFormatterService;
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
    final StringFormatter formatter = new StringFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    final Parameters noParameters = context.noParameters();

    assertEquals(noSpaceText("text"), formatter.format(context, " text ", noParameters, null));
    assertEquals(noSpaceText("RUNTIME"), formatter.format(context, RetentionPolicy.RUNTIME, noParameters, null));
    assertEquals(noSpaceText("hello"), formatter.format(context, new Object() {
      @Override
      public String toString() {
        return " hello";
      }
    }, noParameters, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new StringFormatter());
    final MessageContext context = new MessageContext(formatterRegistry, NO_CACHE_INSTANCE);

    final Parameters parameters = context.parameters()
        .with("a", " a test ")
        .with("b", null)
        .with("c", Integer.valueOf(1234));
    final Message msg = context.getMessageFactory().parse("This is %{a} %{b} %{c}");

    assertEquals("This is a test 1234", msg.format(context, parameters));
  }


  @Test
  public void testFormatterWithMap()
  {
    final MessageFactory messageFactory = NO_CACHE_INSTANCE;
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    final MessageContext context = new MessageContext(formatterRegistry, messageFactory);

    final Parameters parameters = context.parameters()
        .with("empty", "")
        .with("null", null)
        .with("spaces", "  ")
        .with("text", "hello  ");

    assertEquals("", messageFactory.parse("%{empty,!empty:'nok'}").format(context, parameters));
    assertEquals("ok", messageFactory.parse("%{empty,empty:'ok'}").format(context, parameters));
    assertEquals("ok", messageFactory.parse("%{null,empty:'nok',null:'ok'}").format(context, parameters));
    assertEquals("ok", messageFactory.parse("%{null,empty:'ok'}").format(context, parameters));
    assertEquals("ok", messageFactory.parse("%{spaces,empty:'ok'}").format(context, parameters));
    assertEquals("ok", messageFactory.parse("%{spaces,!null:'ok'}").format(context, parameters));
    assertEquals("hello!", messageFactory.parse("%{text,null:'nok',!empty:'%{text}!'}").format(context, parameters));
    assertEquals("hello!", messageFactory.parse("%{text,!null:'%{text}!'}").format(context, parameters));
  }
}