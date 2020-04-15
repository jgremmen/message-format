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
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.parse;
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
    final Parameters parameters = ParameterFactory.createFor(ROOT).noParameters();

    Assert.assertEquals(new TextPart("java.lang.String"),
        formatter.format(String.class, null, parameters, null));
    Assert.assertEquals(new TextPart("java.lang"),
        formatter.format(String.class, "package", parameters, null));
    Assert.assertEquals(new TextPart("String"),
        formatter.format(String.class, "name", parameters, null));
    Assert.assertEquals(new TextPart("double"),
        formatter.format(double.class, null, parameters, null));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new ClassFormatter());
    ParameterFactory factory = ParameterFactory.createFor(ROOT, formatterRegistry);

    final Parameters parameters = factory
        .with("a", Map.class)
        .with("b", long.class)
        .with("c", int[].class)
        .with("d", null);
    final Message msg = parse("%{a} %{b,package,{empty:'?',null:'#'}} %{c,name} %{d,{null:'-'}}");

    assertEquals("java.util.Map # int[] -", msg.format(parameters));
  }


  @Test
  public void testConfigFormat()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new ClassFormatter());
    formatterRegistry.addFormatter(new PackageFormatter());
    ParameterFactory factory = ParameterFactory.createFor(ROOT, formatterRegistry);

    final Parameters parameters = factory.with("class", Map.class);

    assertEquals("java.util", parse("%{class,{format:'package'}}").format(parameters));
    assertEquals("java.util", parse("%{class,'package'}").format(parameters));
    assertEquals("java.util", parse("%{class,package}").format(parameters));
  }
}
