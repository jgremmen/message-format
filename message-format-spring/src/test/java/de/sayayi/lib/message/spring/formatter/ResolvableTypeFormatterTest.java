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
package de.sayayi.lib.message.spring.formatter;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.parameter.runtime.TypeFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.core.ResolvableType.*;


/**
 * Tests {@link ResolvableTypeFormatter}: delegation of the underlying type, including generic type arguments, to the
 * type formatter and its {@code type} configuration key, the fallback to the default formatter without type formatter,
 * unresolved type variables, and the handling of {@link ResolvableType#NONE} and {@code null}.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Spring resolvable type formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ResolvableTypeFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init() {
    messageSupport = createDefaultMessageSupport(new ResolvableTypeFormatter());
  }


  @Test
  @DisplayName("Formattable types")
  void formattableTypes() {
    assertFormatterForType(new ResolvableTypeFormatter(), ResolvableType.class);
  }


  @Test
  @DisplayName("Registered as service")
  void registeredAsService()
  {
    var formatters = new DefaultFormatterService().getFormatters(null, ResolvableType.class, null);

    assertInstanceOf(ResolvableTypeFormatter.class, formatters[0]);
  }


  @Test
  @DisplayName("Class resolvable types are formatted as type")
  void formatClassType()
  {
    assertEquals("String", messageSupport.message("%{t}").with("t", forClass(String.class)).format());
    assertEquals("long", messageSupport.message("%{t}").with("t", forClass(long.class)).format());
    assertEquals("java.time.LocalDate", messageSupport.message("%{t}").with("t", forClass(LocalDate.class)).format());
    assertEquals("LocalDate[]", messageSupport
        .message("%{t,type:c}")
        .with("t", forArrayComponent(forClass(LocalDate.class)))
        .format());
  }


  @Test
  @DisplayName("Generic type arguments are formatted")
  void genericTypes() throws Exception
  {
    var mapType = forClassWithGenerics(Map.class, String.class, LocalDate.class);

    assertEquals("Map<String, java.time.LocalDate>", messageSupport.message("%{t}").with("t", mapType).format());
    assertEquals("Map<String, LocalDate>", messageSupport.message("%{t,type:c}").with("t", mapType).format());

    var fieldType = forField(Fixture.class.getDeclaredField("dates"));

    assertEquals("List<Set<LocalDate>>", messageSupport.message("%{t,type:c}").with("t", fieldType).format());
  }


  @ParameterizedTest(name = "{0} with type config {1}")
  @DisplayName("Type configuration is applied by the type formatter")
  @CsvSource(delimiter = '|', nullValues = "-", textBlock = """
      map      | -   | Map<String, List<java.time.LocalDate>>
      map      | ''  | java.util.Map<java.lang.String, java.util.List<java.time.LocalDate>>
      map      | j   | java.util.Map<String, java.util.List<java.time.LocalDate>>
      map      | u   | Map<java.lang.String, List<java.time.LocalDate>>
      map      | c   | Map<String, List<LocalDate>>
      fixtures | -   | de.sayayi.lib.message.spring.formatter.ResolvableTypeFormatterTest.Fixture[]
      fixtures | c   | Fixture[]
      value    | -   | T
      value    | juv | <T extends CharSequence>
      """)
  void formatWithTypeConfig(String fieldName, String typeConfig, String expected) throws Exception
  {
    var resolvableType = forField(Fixture.class.getDeclaredField(fieldName));
    var formatted = messageSupport
        .message(typeConfig == null ? "%{t}" : "%{t,type:'" + typeConfig + "'}")
        .with("t", resolvableType)
        .format();

    assertEquals(expected, formatted);
    assertEquals(
        TypeFormatter.toString(getType(resolvableType), typeConfig == null ? "ju" : typeConfig),
        formatted,
        "must match the type formatter");
  }


  @Test
  @DisplayName("Formatting is delegated to the type formatter")
  void delegationToTypeFormatter() throws Exception
  {
    var resolvableType = forField(Fixture.class.getDeclaredField("map"));
    var type = getType(resolvableType);

    // without type formatter, the underlying type is formatted by the default formatter
    assertEquals(type.toString(), createMessageSupport(new ResolvableTypeFormatter())
        .message("%{t,type:c}")
        .with("t", resolvableType)
        .format());

    assertEquals("Map<String, List<LocalDate>>", messageSupport
        .message("%{t,type:c}")
        .with("t", resolvableType)
        .format());
  }


  @Test
  @DisplayName("Type variables are formatted as declared")
  void typeVariable() throws Exception
  {
    var fieldType = forField(Fixture.class.getDeclaredField("value"), StringFixture.class);

    assertEquals(String.class, fieldType.resolve());
    assertEquals("T", messageSupport.message("%{t}").with("t", fieldType).format());
  }


  @Test
  @DisplayName("NONE is empty")
  void none()
  {
    assertEquals("", messageSupport.message("%{t}").with("t", NONE).format());
    assertEquals("unknown", messageSupport.message("%{t,empty:'unknown'}").with("t", NONE).format());
    assertEquals("String", messageSupport
        .message("%{t,empty:'unknown'}")
        .with("t", forClass(String.class))
        .format());
  }


  @Test
  @DisplayName("Null is matched by the null key")
  void nullMatchesNullKey()
  {
    assertEquals("unknown", messageSupport
        .message("%{t,null:'unknown'}")
        .with("t", null)
        .format());
  }




  @Contract(pure = true)
  private static @NotNull Type getType(@NotNull ResolvableType resolvableType) {
    return resolvableType.getType();
  }




  @SuppressWarnings("unused")
  private static class Fixture<T extends CharSequence>
  {
    private List<Set<LocalDate>> dates;
    private Map<String,List<LocalDate>> map;
    @SuppressWarnings("rawtypes")
    private Fixture[] fixtures;
    private T value;
  }




  private static final class StringFixture extends Fixture<String> {
  }
}
