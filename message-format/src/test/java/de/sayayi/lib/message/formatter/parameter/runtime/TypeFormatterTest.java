/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.platform.commons.util.ReflectionUtils.findMethod;


/**
 * Tests {@link TypeFormatter}: textual representation of native, array, Java, project and nested classes as well as
 * parameterized types, type variables, wildcards and generic arrays.
 *
 * @author Jeroen Gremmen
 * @since 0.6.0
 */
@DisplayName("Type formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class TypeFormatterTest
{
  static Stream<Arguments> primitiveTypeNames()
  {
    return Stream.of(
        arguments(boolean.class, "boolean"),
        arguments(byte.class, "byte"),
        arguments(char.class, "char"),
        arguments(short.class, "short"),
        arguments(int.class, "int"),
        arguments(long.class, "long"),
        arguments(float.class, "float"),
        arguments(double.class, "double"),

        // arrays
        arguments(boolean[].class, "boolean[]"),
        arguments(byte[].class, "byte[]"),
        arguments(char[][].class, "char[][]"),
        arguments(short[].class, "short[]"),
        arguments(int[].class, "int[]"),
        arguments(long[][][].class, "long[][][]"),
        arguments(float[].class, "float[]"),
        arguments(double[].class, "double[]"));
  }


  @ParameterizedTest(name = "{1}")
  @DisplayName("Primitive types and arrays of primitive types are formatted with their simple name")
  @MethodSource("primitiveTypeNames")
  void toStringPrimitive(Class<?> type, String expected)
  {
    assertEquals(
        expected,
        TypeFormatter.toString(type, ""));
  }


  @Test
  @DisplayName("JDK classes are formatted with the full name, without the java.lang. prefix or with the simple name")
  void toStringJavaClass()
  {
    assertEquals("java.lang.reflect.Method", TypeFormatter.toString(Method.class, ""));
    assertEquals("reflect.Method", TypeFormatter.toString(Method.class, "j"));
    assertEquals("Method", TypeFormatter.toString(Method.class, "c"));
  }


  @Test
  @DisplayName("Project classes are formatted with the full or the simple name")
  void toStringProjectClass()
  {
    assertEquals(
        "de.sayayi.lib.message.formatter.parameter.runtime.TypeFormatter",
        TypeFormatter.toString(TypeFormatter.class, ""));
    assertEquals(
        "TypeFormatter",
        TypeFormatter.toString(TypeFormatter.class, "c"));
  }


  @Test
  @DisplayName("Nested classes are formatted with the enclosing class name, separated by a dot")
  void toStringNestedClass()
  {
    class Test {
    }

    assertEquals(
        TypeFormatterTest.class.getName() + '.' + Test.class.getSimpleName(),
        TypeFormatter.toString(Test.class, ""));
    assertEquals(
        Test.class.getSimpleName(),
        TypeFormatter.toString(Test.class, "c"));
  }


  @Test
  @DisplayName("Parameterized types are formatted with their type arguments")
  void toStringParameterized()
  {
    //noinspection OptionalGetWithoutIsPresent
    var method = findMethod(Collections.class, "unmodifiableMap", Map.class).get();

    assertEquals("java.util.Map<K, V>", TypeFormatter.toString(method.getGenericReturnType(), ""));
    assertEquals("Map<K, V>", TypeFormatter.toString(method.getGenericReturnType(), "u"));
    assertEquals("Map<K, V>", TypeFormatter.toString(method.getGenericReturnType(), "c"));

    final var arg0 = method.getGenericParameterTypes()[0];
    assertEquals("java.util.Map<? extends K, ? extends V>", TypeFormatter.toString(arg0, ""));
    assertEquals("Map<? extends K, ? extends V>", TypeFormatter.toString(arg0, "u"));
    assertEquals("Map<? extends K, ? extends V>", TypeFormatter.toString(arg0, "c"));
  }


  @Test
  @DisplayName("Type variables are formatted by name, or with their bounds using the v flag")
  void toStringTypeVariable()
  {
    //noinspection OptionalGetWithoutIsPresent
    var method = findMethod(TypeFormatterTest.class, "internalMethod2", Iterable.class).get();
    var type = method.getGenericParameterTypes()[0];

    assertEquals("T", TypeFormatter.toString(type, ""));

    assertEquals(
        "<T extends Iterable<String> & Enumeration<String>>",
        TypeFormatter.toString(type, "juv"));
  }


  @Test
  @DisplayName("Type variables within bounds are not expanded")
  void toStringRecursiveTypeVariable()
  {
    //noinspection OptionalGetWithoutIsPresent
    var method = findMethod(TypeFormatterTest.class, "internalMethod3", Comparable.class, List.class).get();
    var types = method.getGenericParameterTypes();

    // recursive bound, previously failed with a StackOverflowError
    assertEquals(
        "<T extends Comparable<T>>",
        TypeFormatter.toString(types[0], "juv"));
    assertEquals(
        "<T extends java.lang.Comparable<T>>",
        TypeFormatter.toString(types[0], "v"));

    // bound referencing another bounded type variable
    assertEquals(
        "<U extends List<T>>",
        TypeFormatter.toString(types[1], "juv"));
  }


  @Test
  @DisplayName("Wildcard types are formatted with their super or extends bound")
  void toStringWildcard()
  {
    //noinspection OptionalGetWithoutIsPresent
    var method1 = findMethod(Collections.class, "copy", List.class, List.class).get();

    var m1arg0 = method1.getGenericParameterTypes()[0];
    assertEquals("java.util.List<? super T>", TypeFormatter.toString(m1arg0, ""));
    assertEquals("List<? super T>", TypeFormatter.toString(m1arg0, "u"));
    assertEquals("List<? super T>", TypeFormatter.toString(m1arg0, "c"));

    //noinspection OptionalGetWithoutIsPresent
    var method2 = findMethod(Collections.class, "shuffle", List.class).get();

    var m2arg0 = method2.getGenericParameterTypes()[0];
    assertEquals("java.util.List<?>", TypeFormatter.toString(m2arg0, ""));
    assertEquals("List<?>", TypeFormatter.toString(m2arg0, "u"));
    assertEquals("List<?>", TypeFormatter.toString(m2arg0, "c"));
  }


  @Test
  @DisplayName("Generic array types are formatted with their component type followed by brackets")
  void toStringGenericArray()
  {
    //noinspection OptionalGetWithoutIsPresent
    var method = findMethod(TypeFormatterTest.class, "internalMethod1").get();
    var type = method.getGenericReturnType();

    assertEquals("java.util.Optional<? extends java.lang.Number>[]", TypeFormatter.toString(type, ""));
    assertEquals("java.util.Optional<? extends Number>[]", TypeFormatter.toString(type, "j"));
    assertEquals("Optional<? extends java.lang.Number>[]", TypeFormatter.toString(type, "u"));
    assertEquals("Optional<? extends Number>[]", TypeFormatter.toString(type, "ju"));
    assertEquals("Optional<? extends Number>[]", TypeFormatter.toString(type, "c"));
  }


  @SuppressWarnings("unused")
  private static Optional<? extends Number>[] internalMethod1() {
    return null;
  }


  @SuppressWarnings("unused")
  private static <T extends Iterable<String> & Enumeration<String>> void internalMethod2(@SuppressWarnings("unused") T dummy) {
  }


  @SuppressWarnings("unused")
  private static <T extends Comparable<T>, U extends List<T>> void internalMethod3(T t, U u) {
  }
}
