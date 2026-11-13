/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;

import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link MethodParameterFormatter}: formattable types, the {@code parameter} configuration values and string
 * key comparison.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Method parameter formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MethodParameterFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init()
  {
    val formatterService = new DefaultFormatterService();
    formatterService.addFormatter(new MethodParameterFormatter());
    formatterService.addFormatter(new MethodFormatter());

    messageSupport = MessageSupportFactory.create(formatterService);
  }


  @Test
  void testFormattableTypes() {
    assertFormatterForType(new MethodParameterFormatter(), Parameter.class);
  }


  @Test
  @DisplayName("Format parameter, its name and its type")
  void testFormat() throws ReflectiveOperationException
  {
    val parameter = List.class.getMethod("get", int.class).getParameters()[0];

    assertEquals("int arg0", messageSupport.message("%{p}").with("p", parameter).format());
    assertEquals("arg0", messageSupport.message("%{p,parameter:name}").with("p", parameter).format());
    assertEquals("Type: int", messageSupport.message("Type: %{p,parameter:class}").with("p", parameter).format());
    assertEquals("Collection<? extends E>", messageSupport
        .message("%{p,parameter:class}")
        .with("p", List.class.getMethod("addAll", Collection.class).getParameters()[0])
        .format());
  }


  @Test
  @DisplayName("Format method parameter list")
  void testParameterList() throws ReflectiveOperationException
  {
    val method = String.class.getMethod("replace", char.class, char.class);

    assertEquals("replace(char, char)", messageSupport
        .message("%{m,method:name}(%{params,list-value:'%{value,parameter:class}'})")
        .with("m", method)
        .with("params", method.getParameters())
        .format());
  }


  @Test
  @DisplayName("Compare parameter name with string keys")
  void testStringKey() throws ReflectiveOperationException
  {
    val parameter = List.class.getMethod("get", int.class).getParameters()[0];

    assertEquals("first", messageSupport
        .message("%{p,format:choice,'arg0':'first',:'other'}")
        .with("p", parameter)
        .format());
  }
}
