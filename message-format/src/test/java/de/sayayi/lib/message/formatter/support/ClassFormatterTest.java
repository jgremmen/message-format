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
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Locale.ROOT;
import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class ClassFormatterTest
{
  @Test
  public void testFormat()
  {
    final ClassFormatter formatter = new ClassFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE,
        ROOT);
    final Parameters parameters = context.noParameters();

    Assert.assertEquals(new TextPart("java.lang.String"),
        formatter.format(context, String.class, null, parameters, null));
    Assert.assertEquals(new TextPart("java.lang"),
        formatter.format(context, String.class, "package", parameters, null));
    Assert.assertEquals(new TextPart("String"),
        formatter.format(context, String.class, "name", parameters, null));
    Assert.assertEquals(new TextPart("double"),
        formatter.format(context, double.class, null, parameters, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new ClassFormatter());
    final MessageContext context = new MessageContext(formatterRegistry, NO_CACHE_INSTANCE, ROOT);

    final Parameters parameters = context.parameters()
        .with("a", Map.class)
        .with("b", long.class)
        .with("c", int[].class)
        .with("d", null);
    final Message msg = context.getMessageFactory()
        .parse("%{a} %{b,package,{empty:'?',null:'#'}} %{c,name} %{d,{null:'-'}}");

    assertEquals("java.util.Map # int[] -", msg.format(context, parameters));
  }


  @Test
  public void testConfigFormat()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new ClassFormatter());
    formatterRegistry.addFormatter(new PackageFormatter());

    final MessageContext context = new MessageContext(formatterRegistry, NO_CACHE_INSTANCE, ROOT);
    final Parameters parameters = context.parameters().with("class", Map.class);

    assertEquals("java.util", context.getMessageFactory().parse("%{class,{format:'package'}}")
        .format(context, parameters));
    assertEquals("java.util", context.getMessageFactory().parse("%{class,'package'}")
        .format(context, parameters));
    assertEquals("java.util", context.getMessageFactory().parse("%{class,package}")
        .format(context, parameters));
  }
}
