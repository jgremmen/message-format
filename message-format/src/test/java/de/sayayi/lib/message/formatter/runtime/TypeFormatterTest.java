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
package de.sayayi.lib.message.formatter.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
class TypeFormatterTest
{
  @Test
  void testToStringNative()
  {
    Assertions.assertEquals("boolean", TypeFormatter.toString(boolean.class, ""));
    assertEquals("byte", TypeFormatter.toString(byte.class, ""));
    assertEquals("char", TypeFormatter.toString(char.class, ""));
    assertEquals("short", TypeFormatter.toString(short.class, ""));
    assertEquals("int", TypeFormatter.toString(int.class, ""));
    assertEquals("long", TypeFormatter.toString(long.class, ""));
    assertEquals("float", TypeFormatter.toString(float.class, ""));
    assertEquals("double", TypeFormatter.toString(double.class, ""));
  }


  @Test
  void testToStringNativeArray()
  {
    assertEquals("boolean[]", TypeFormatter.toString(boolean[].class, ""));
    assertEquals("byte[]", TypeFormatter.toString(byte[].class, ""));
    assertEquals("char[][]", TypeFormatter.toString(char[][].class, ""));
    assertEquals("short[]", TypeFormatter.toString(short[].class, ""));
    assertEquals("int[]", TypeFormatter.toString(int[].class, ""));
    assertEquals("long[][][]", TypeFormatter.toString(long[][][].class, ""));
    assertEquals("float[]", TypeFormatter.toString(float[].class, ""));
    assertEquals("double[]", TypeFormatter.toString(double[].class, ""));
  }


  @Test
  void testToStringJavaClass()
  {
    assertEquals("java.lang.reflect.Method", TypeFormatter.toString(Method.class, ""));
    assertEquals("reflect.Method", TypeFormatter.toString(Method.class, "j"));
    assertEquals("Method", TypeFormatter.toString(Method.class, "c"));
  }


  @Test
  void testToStringProjectClass()
  {
    assertEquals("de.sayayi.lib.message.formatter.runtime.TypeFormatter",
        TypeFormatter.toString(TypeFormatter.class, ""));
    assertEquals("TypeFormatter",
        TypeFormatter.toString(TypeFormatter.class, "c"));
  }


  @Test
  void testToStringNestedClass()
  {
    class Test {
    }

    assertEquals(TypeFormatterTest.class.getName() + '.' + Test.class.getSimpleName(),
        TypeFormatter.toString(Test.class, ""));
    assertEquals(Test.class.getSimpleName(), TypeFormatter.toString(Test.class, "c"));
  }


  @Test
  void testToStringParameterized()
  {
    //noinspection OptionalGetWithoutIsPresent
    final Method method = ReflectionUtils.findMethod(Collections.class, "unmodifiableMap", Map.class).get();

    assertEquals("java.util.Map<K, V>", TypeFormatter.toString(method.getGenericReturnType(), ""));
    assertEquals("Map<K, V>", TypeFormatter.toString(method.getGenericReturnType(), "u"));
    assertEquals("Map<K, V>", TypeFormatter.toString(method.getGenericReturnType(), "c"));

    final Type arg0 = method.getGenericParameterTypes()[0];
    assertEquals("java.util.Map<? extends K, ? extends V>", TypeFormatter.toString(arg0, ""));
    assertEquals("Map<? extends K, ? extends V>", TypeFormatter.toString(arg0, "u"));
    assertEquals("Map<? extends K, ? extends V>", TypeFormatter.toString(arg0, "c"));
  }


  @Test
  void testToStringTypeVariable()
  {
    //noinspection OptionalGetWithoutIsPresent
    final Method method = ReflectionUtils.findMethod(TypeFormatterTest.class, "internalMethod2", Iterable.class).get();
    final Type type = method.getGenericParameterTypes()[0];

    assertEquals("T", TypeFormatter.toString(type, ""));

    assertEquals("<T extends Iterable<String> & Enumeration<String>>",
        TypeFormatter.toString(type, "Tju"));
  }


  @Test
  void testToStringWildcard()
  {
    //noinspection OptionalGetWithoutIsPresent
    final Method method1 = ReflectionUtils.findMethod(Collections.class, "copy", List.class, List.class).get();

    final Type m1arg0 = method1.getGenericParameterTypes()[0];
    assertEquals("java.util.List<? super T>", TypeFormatter.toString(m1arg0, ""));
    assertEquals("List<? super T>", TypeFormatter.toString(m1arg0, "u"));
    assertEquals("List<? super T>", TypeFormatter.toString(m1arg0, "c"));

    //noinspection OptionalGetWithoutIsPresent
    final Method method2 = ReflectionUtils.findMethod(Collections.class, "shuffle", List.class).get();

    final Type m2arg0 = method2.getGenericParameterTypes()[0];
    assertEquals("java.util.List<?>", TypeFormatter.toString(m2arg0, ""));
    assertEquals("List<?>", TypeFormatter.toString(m2arg0, "u"));
    assertEquals("List<?>", TypeFormatter.toString(m2arg0, "c"));
  }


  @Test
  void testToStringGenericArray()
  {
    //noinspection OptionalGetWithoutIsPresent
    final Method method = ReflectionUtils.findMethod(TypeFormatterTest.class, "internalMethod1").get();
    final Type type = method.getGenericReturnType();

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
}