/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.spring.formatter;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.spel.SpelEvaluationException;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * @author Jeroen Gremmen
 * @since 0.8.3
 */
@DisplayName("Spring expression language formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class SpELFormatterTest extends AbstractFormatterTest
{
  private static MessageSupport messageSupport;


  @BeforeAll
  static void init()
  {
    final var formatterService = new DefaultFormatterService();

    formatterService.addFormatter(new SpELFormatter(
        new DefaultConversionService(), SpELFormatterTest.class.getClassLoader()));

    messageSupport = MessageSupportFactory.create(formatterService);
  }


  @Test
  @DisplayName("Extract value from bean with optional format selector")
  void testFormat()
  {
    val map = new TreeMap<String,Integer>();
    map.put("A", 0);
    map.put("C", 34);
    map.put("D", -8);

    assertEquals("34", messageSupport
        .message("%{map,spel-expr:'entrySet().toArray()[1].value'}")
        .with("map", map)
        .format());

    assertEquals("no", messageSupport
        .message("%{map,format:spel,spel-expr:'entrySet().toArray()[0].value',spel-format:bool,false:no}")
        .with("map", map)
        .format());

    assertEquals("negative", messageSupport
        .message("%{map,spel-expr:'entrySet().toArray()[2].value',spel-format:choice,<0:negative}")
        .with("map", map)
        .format());
  }


  @Test
  @DisplayName("Instance methods, properties and variables are available")
  void testReadOnlyDataAccess()
  {
    assertEquals("ALICE", messageSupport
        .message("%{name,spel-expr:'toUpperCase()'}")
        .with("name", "alice")
        .format());

    assertEquals("5", messageSupport
        .message("%{name,spel-expr:'bytes.length'}")
        .with("name", "alice")
        .format());

    assertEquals("42", messageSupport
        .message("%{a,spel-expr:'#b + 1'}")
        .with("a", 1)
        .with("b", 41)
        .format());
  }


  @ParameterizedTest(name = "[{index}] {0}")
  @DisplayName("Type references, static methods, constructors and assignments are rejected")
  @ValueSource(strings = {
      "T(java.lang.System).getProperty(\"java.version\")",
      "T(java.lang.Math).PI",
      "getClass().forName(\"java.lang.Runtime\")",
      "getClass().getMethods()",
      "new java.lang.StringBuilder(\"x\")",
      "@systemProperties",
      "#b = 1",
      "bytes = null"
  })
  void testRestrictedExpressions(String expression)
  {
    val message = messageSupport.message("%{v,spel-expr:\"" + expression.replace("\"", "'") + "\"}");

    val exception = assertThrows(MessageFormatException.class, () -> message
        .with("v", "value")
        .with("b", 41)
        .format());

    assertInstanceOf(SpelEvaluationException.class, exception.getCause());
  }
}
