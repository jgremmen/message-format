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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.internal.TextPart;
import org.junit.Test;

import java.lang.annotation.RetentionPolicy;

import static de.sayayi.lib.message.MessageFactory.parse;
import static org.junit.Assert.assertEquals;


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
    final Parameters noParameters = ParameterFactory.DEFAULT.noParameters();

    assertEquals(new TextPart("text"), formatter.format(" text ", null, noParameters, null));
    assertEquals(new TextPart("RUNTIME"), formatter.format(RetentionPolicy.RUNTIME, null, noParameters, null));
    assertEquals(new TextPart("hello"), formatter.format(new Object() {
      @Override
      public String toString() {
        return " hello";
      }
    }, null, noParameters, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new StringFormatter());
    final ParameterFactory factory = ParameterFactory.createFor(formatterRegistry);

    final Parameters parameters = factory
        .with("a", " a test ")
        .with("b", null)
        .with("c", Integer.valueOf(1234));
    final Message msg = parse("This is %{a} %{b} %{c}");

    assertEquals("This is a test 1234", msg.format(parameters));
  }


  @Test
  public void testFormatterWithMap()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    final ParameterFactory factory = ParameterFactory.createFor(formatterRegistry);

    final Parameters parameters = factory
        .with("empty", "")
        .with("null", null)
        .with("spaces", "  ")
        .with("text", "hello  ");

    assertEquals("", parse("%{empty,{!empty:'nok'}}").format(parameters));
    assertEquals("ok", parse("%{empty,{empty:'ok'}}").format(parameters));
    assertEquals("ok", parse("%{null,{empty:'nok',null:'ok'}}").format(parameters));
    assertEquals("ok", parse("%{null,{empty:'ok'}}").format(parameters));
    assertEquals("ok", parse("%{spaces,{empty:'ok'}}").format(parameters));
    assertEquals("ok", parse("%{spaces,{!null:'ok'}}").format(parameters));
    assertEquals("hello!", parse("%{text,{null:'nok',!empty:'%{text}!'}}").format(parameters));
    assertEquals("hello!", parse("%{text,{!null:'%{text}!'}}").format(parameters));
  }
}
