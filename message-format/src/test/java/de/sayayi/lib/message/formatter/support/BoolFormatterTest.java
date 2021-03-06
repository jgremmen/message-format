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
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.internal.part.TextPart;
import org.junit.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
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
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE,
        "de-DE");
    final Parameters parameters = context.noParameters();

    assertEquals(new TextPart("wahr"), formatter.format(context, Boolean.TRUE, null, parameters, null));
    assertEquals(new TextPart("falsch"), formatter.format(context, 0.0d, null, parameters, null));
    assertEquals(new TextPart("wahr"), formatter.format(context, -0.0001f, null, parameters, null));
    assertEquals(new TextPart("falsch"), formatter.format(context, "FALSE", null, parameters, null));
    assertEquals(new TextPart("wahr"), formatter.format(context, "TrUe", null, parameters, null));
    assertEquals(new TextPart("wahr"), formatter.format(context, -4, null, parameters, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new BoolFormatter());
    final MessageContext context = new MessageContext(formatterRegistry, NO_CACHE_INSTANCE, ENGLISH);

    final Parameters parameters = context.parameters()
        .with("a", Boolean.FALSE)
        .with("b", Boolean.TRUE)
        .with("c", Integer.valueOf(1234))
        .with("d", Integer.valueOf(0))
        .with("e", Double.valueOf(3.14d));
    final Message msg = context.getMessageFactory()
        .parse("%{a} %{b} %{c} %{c,bool} %{d,bool,{true:'yes',false:'no'}} %{e}");

    assertEquals("false true 1234 true no 3.14", msg.format(context, parameters));
  }


  @Test
  public void testNamedFormatter()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new BoolFormatter());
    final MessageContext context = new MessageContext(formatterRegistry, NO_CACHE_INSTANCE, GERMAN);

    final Message msg = context.getMessageFactory().parse("%{b,bool,{null:'<unknown>',true:'yes',false:'no'}}");

    assertEquals("<unknown>", msg.format(context, context.parameters().with("b", null)));
    assertEquals("yes", msg.format(context, context.parameters().with("b", true)));
    assertEquals("no", msg.format(context, context.parameters().with("b", false)));
  }
}
