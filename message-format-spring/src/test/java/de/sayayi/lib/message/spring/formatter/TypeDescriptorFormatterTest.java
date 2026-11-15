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
import org.springframework.core.convert.TypeDescriptor;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.core.convert.TypeDescriptor.valueOf;


/**
 * Tests {@link TypeDescriptorFormatter}: delegation of the described type, including generic type arguments, to the
 * type formatter and its {@code type} configuration key, the fallback to the default formatter without type formatter,
 * and the handling of {@code null}.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Spring type descriptor formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class TypeDescriptorFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init() {
    messageSupport = createDefaultMessageSupport(new TypeDescriptorFormatter());
  }


  @Test
  @DisplayName("Formattable types")
  void formattableTypes() {
    assertFormatterForType(new TypeDescriptorFormatter(), TypeDescriptor.class);
  }


  @Test
  @DisplayName("Registered as service")
  void registeredAsService()
  {
    var formatters = new DefaultFormatterService().getFormatters(null, TypeDescriptor.class, null);

    assertInstanceOf(TypeDescriptorFormatter.class, formatters[0]);
  }


  @Test
  @DisplayName("Class type descriptors are formatted as type")
  void formatClassType()
  {
    assertEquals(
        "String",
        messageSupport
            .message("%{t}")
            .with("t", valueOf(String.class))
            .format());
    assertEquals(
        "int",
        messageSupport
            .message("%{t}")
            .with("t", valueOf(int.class))
            .format());
    assertEquals(
        "java.time.LocalDate",
        messageSupport
            .message("%{t}")
            .with("t", valueOf(LocalDate.class))
            .format());
    assertEquals(
        "LocalDate[]",
        messageSupport
            .message("%{t,type:c}").with("t", valueOf(LocalDate[].class)).format());
  }


  @Test
  @DisplayName("Generic type arguments are formatted")
  void genericTypes() throws Exception
  {
    var listOfDates = TypeDescriptor.collection(List.class, valueOf(LocalDate.class));

    assertEquals(
        "List<java.time.LocalDate>",
        messageSupport
            .message("%{t}")
            .with("t", listOfDates)
            .format());
    assertEquals(
        "List<LocalDate>",
        messageSupport
            .message("%{t,type:c}")
            .with("t", listOfDates)
            .format());

    var fieldType = new TypeDescriptor(Fixture.class.getDeclaredField("sets"));

    assertEquals(
        "Map<String, Set<LocalDate>>",
        messageSupport
            .message("%{t,type:c}")
            .with("t", fieldType)
            .format());
  }


  @ParameterizedTest(name = "{0} with type config {1}")
  @DisplayName("Type configuration is applied by the type formatter")
  @CsvSource(delimiter = '|', nullValues = "-", textBlock = """
      map      | -   | Map<String, List<java.time.LocalDate>>
      map      | ''  | java.util.Map<java.lang.String, java.util.List<java.time.LocalDate>>
      map      | j   | java.util.Map<String, java.util.List<java.time.LocalDate>>
      map      | u   | Map<java.lang.String, List<java.time.LocalDate>>
      map      | c   | Map<String, List<LocalDate>>
      fixtures | -   | de.sayayi.lib.message.spring.formatter.TypeDescriptorFormatterTest.Fixture[]
      fixtures | c   | Fixture[]
      value    | -   | T
      value    | juv | <T extends CharSequence>
      """)
  void formatWithTypeConfig(String fieldName, String typeConfig, String expected) throws Exception
  {
    var typeDescriptor = new TypeDescriptor(Fixture.class.getDeclaredField(fieldName));
    var formatted = messageSupport
        .message(typeConfig == null ? "%{t}" : "%{t,type:'" + typeConfig + "'}")
        .with("t", typeDescriptor)
        .format();

    assertEquals(expected, formatted);
    assertEquals(
        TypeFormatter.toString(getType(typeDescriptor), typeConfig == null ? "ju" : typeConfig),
        formatted,
        "must match the type formatter");
  }


  @Test
  @DisplayName("Formatting is delegated to the type formatter")
  void delegationToTypeFormatter() throws Exception
  {
    var typeDescriptor = new TypeDescriptor(Fixture.class.getDeclaredField("map"));

    // without type formatter, the underlying type is formatted by the default formatter
    assertEquals(
        getType(typeDescriptor).toString(),
        createMessageSupport(new TypeDescriptorFormatter())
            .message("%{t,type:c}")
            .with("t", typeDescriptor)
            .format());

    assertEquals(
        "Map<String, List<LocalDate>>",
        messageSupport
            .message("%{t,type:c}")
            .with("t", typeDescriptor)
            .format());
  }


  @Test
  @DisplayName("Null is matched by the null key")
  void nullMatchesNullKey()
  {
    assertEquals(
        "unknown",
        messageSupport
            .message("%{t,null:'unknown'}")
            .with("t", null)
            .format());
  }




  @Contract(pure = true)
  private static @NotNull Type getType(@NotNull TypeDescriptor typeDescriptor) {
    return typeDescriptor.getResolvableType().getType();
  }




  @SuppressWarnings("unused")
  private static class Fixture<T extends CharSequence>
  {
    private Map<String,Set<LocalDate>> sets;
    private Map<String,List<LocalDate>> map;
    @SuppressWarnings("rawtypes")
    private Fixture[] fixtures;
    private T value;
  }
}
