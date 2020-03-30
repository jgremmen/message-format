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
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import org.junit.Test;

import static de.sayayi.lib.message.MessageFactory.parse;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class BoolFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    assertFormatterForType(new BoolFormatter(), boolean.class);
    assertFormatterForType(new BoolFormatter(), Boolean.class);
  }


  @Test
  public void testFormat()
  {
    final BoolFormatter formatter = new BoolFormatter();
    final Parameters parameters = ParameterFactory.createFor("de-DE").noParameters();

    assertEquals("wahr", formatter.format(Boolean.TRUE, null, parameters, null));
    assertEquals("falsch", formatter.format(0.0d, null, parameters, null));
    assertEquals("wahr", formatter.format(-0.0001f, null, parameters, null));
    assertEquals("falsch", formatter.format("FALSE", null, parameters, null));
    assertEquals("wahr", formatter.format("TrUe", null, parameters, null));
    assertEquals("wahr", formatter.format(-4, null, parameters, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new BoolFormatter());
    ParameterFactory factory = ParameterFactory.createFor(ENGLISH, formatterRegistry);

    final Parameters parameters = factory
        .with("a", Boolean.FALSE)
        .with("b", Boolean.TRUE)
        .with("c", Integer.valueOf(1234))
        .with("d", Integer.valueOf(0))
        .with("e", Double.valueOf(3.14d));
    final Message msg = parse("%{a} %{b} %{c} %{c,bool} %{d,bool,{true->'yes',false->'no'}} %{e}");

    assertEquals("false true 1234 true no 3.14", msg.format(parameters));
  }


  @Test
  public void testNamedFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new BoolFormatter());
    ParameterFactory factory = ParameterFactory.createFor(GERMAN, formatterRegistry);

    final Message msg = parse("%{b,bool,{'null'->'<unknown>',true->'yes','no'}}");

    assertEquals("<unknown>", msg.format(factory.with("b", null)));
    assertEquals("yes", msg.format(factory.with("b", true)));
    assertEquals("no", msg.format(factory.with("b", false)));
  }
}
