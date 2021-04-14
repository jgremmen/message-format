/*
 * Copyright 2021 Jeroen Gremmen
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
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class MethodFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes() {
    assertFormatterForType(new MethodFormatter(), Method.class);
  }


  @Test
  public void testFormat() throws Exception
  {
    GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new MethodFormatter());
    registry.addFormatter(new ClassFormatter());

    ParameterFactory factory = ParameterFactory.createFor(Locale.UK);

    Message message = MessageFactory.parse("%{m} %{m,name} %{m,return-type} %{m,class}");

    assertEquals("protected static " + Set.class.getName() + " " + MethodFormatterTest.class.getName() +
        ".dummy() dummy " + Set.class.getName() + "<" + String.class.getName() + "> " +
        MethodFormatterTest.class.getName(), message.format(factory.with("m",
        MethodFormatterTest.class.getDeclaredMethod("dummy"))));
  }


  protected static Set<String> dummy() {
    return null;
  }
}
