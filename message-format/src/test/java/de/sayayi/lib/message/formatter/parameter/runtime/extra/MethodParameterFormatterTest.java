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
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


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
  private final MessageSupport messageSupport =
      createDefaultMessageSupport(new MethodParameterFormatter(), new MethodFormatter());


  @Test
  @DisplayName("Formattable types include Parameter")
  void formattableTypes() {
    assertFormatterForType(new MethodParameterFormatter(), Parameter.class);
  }


  static Stream<Arguments> formattedParameters() throws ReflectiveOperationException
  {
    var getParameter = List.class.getMethod("get", int.class).getParameters()[0];
    var addAllParameter = List.class.getMethod("addAll", Collection.class).getParameters()[0];

    return Stream.of(
        arguments("%{p}", getParameter, "int arg0"),
        arguments("%{p,parameter:name}", getParameter, "arg0"),
        arguments("Type: %{p,parameter:class}", getParameter, "Type: int"),
        arguments("%{p,parameter:class}", addAllParameter, "Collection<? extends E>")
    );
  }


  @ParameterizedTest(name = "{0} -> {2}")
  @DisplayName("Format parameter, its name and its type")
  @MethodSource("formattedParameters")
  void formatParameterNameAndType(@Language("MessageFormat") String formatString, Parameter parameter, String expected)
  {
    assertEquals(expected, messageSupport
        .message(formatString)
        .with("p", parameter)
        .format());
  }


  @Test
  @DisplayName("Format method parameter list")
  void parameterList() throws ReflectiveOperationException
  {
    var method = String.class.getMethod("replace", char.class, char.class);

    assertEquals(
        "replace(char, char)",
        messageSupport
            .message("%{m,method:name}(%{params,list-value:'%{value,parameter:class}'})")
            .with("m", method)
            .with("params", method.getParameters())
            .format());
  }


  @Test
  @DisplayName("Compare parameter name with string keys")
  void stringKey() throws ReflectiveOperationException
  {
    var parameter = List.class.getMethod("get", int.class).getParameters()[0];

    assertEquals(
        "first",
        messageSupport
            .message("%{p,format:choice,'arg0':'first',:'other'}")
            .with("p", parameter)
            .format());
  }
}
