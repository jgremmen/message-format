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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
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
    val messageAccessor = MessageSupportFactory
        .create(new GenericFormatterService(), NO_CACHE_INSTANCE)
        .getMessageAccessor();

    assertEquals(noSpaceText("text"), format(messageAccessor, " text "));
    assertEquals(noSpaceText("RUNTIME"), format(messageAccessor, RetentionPolicy.RUNTIME));
    assertEquals(noSpaceText("hello"), format(messageAccessor, new Object() {
      @Override
      public String toString() {
        return " hello";
      }
    }));
  }


  @Test
  public void testFormatter()
  {
    val messageSupport = MessageSupportFactory.create(new GenericFormatterService(), NO_CACHE_INSTANCE);

    assertEquals("This is a test 1234", messageSupport
        .message("This is %{a} %{b} %{c}")
        .with("a", " a test ")
        .with("b", null)
        .with("c", 1234)
        .format());
  }


  @Test
  public void testFormatterWithMap()
  {
    val messageSupport = MessageSupportFactory.create(
        new GenericFormatterService(), NO_CACHE_INSTANCE);
    val parameters = new HashMap<String,Object>();

    parameters.put("empty", "");
    parameters.put("null", null);
    parameters.put("spaces", "  ");
    parameters.put("text", "hello  ");

    assertEquals("",
        messageSupport.message("%{empty,!empty:nok}").with(parameters).format());
    assertEquals("ok",
        messageSupport.message("%{empty,empty:ok}").with(parameters).format());
    assertEquals("ok",
        messageSupport.message("%{null,empty:nok,null:ok}").with(parameters).format());
    assertEquals("ok",
        messageSupport.message("%{null,empty:ok}").with(parameters).format());
    assertEquals("ok",
        messageSupport.message("%{spaces,empty:ok}").with(parameters).format());
    assertEquals("ok",
        messageSupport.message("%{spaces,!null:ok}").with(parameters).format());
    assertEquals("hello!",
        messageSupport.message("%{text,null:nok,!empty:'%{text}!'}").with(parameters).format());
    assertEquals("hello!",
        messageSupport.message("%{text,!null:'%{text}!'}").with(parameters).format());
  }
}
