/*
 * Copyright 2020 Jeroen Gremmen
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

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.formatter.parameter.named.ClassifierFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link SupplierFormatter}: formatting, map key comparison, size and classifiers of the supplied value, and the
 * single evaluation of a supplier passed as a parameter.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
@DisplayName("Supplier formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class SupplierFormatterTest extends AbstractFormatterTest
{
  private final MessageSupport messageSupport = createDefaultMessageSupport(new ClassifierFormatter());


  @Test
  @DisplayName("Formattable types include Supplier")
  void formattableTypes() {
    assertFormatterForType(new SupplierFormatter(), Supplier.class);
  }


  @Test
  @DisplayName("Supplied value is formatted according to its type")
  void formatSuppliedValue()
  {
    final Supplier<List<String>> names = () -> List.of("alice", "bob", "carol");

    assertEquals(
        "Users: alice, bob and carol",
        messageSupport
            .message("Users: %{names,list-sep-last:' and '}")
            .with("names", names)
            .format());
  }


  static Stream<Arguments> suppliedValues()
  {
    return Stream.of(
        arguments("%{v,empty:'no comment'}", (Supplier<String>)() -> "", "no comment"),
        arguments("%{v,1:one,:other}", (Supplier<Integer>)() -> 1, "one"),
        arguments("%{v,true:yes,false:no}", (Supplier<Boolean>)() -> true, "yes"),
        arguments("%{v,format:choice,'abc':ABC,:other}", (Supplier<String>)() -> "abc", "ABC"),
        arguments("%{v,format:size}", (Supplier<List<String>>)() -> List.of("a", "b", "c"), "3"));
  }


  @ParameterizedTest(name = "{0} -> {2}")
  @DisplayName("Map keys and size are based on the supplied value")
  @MethodSource("suppliedValues")
  void mapKeysAndSize(@Language("MessageFormat") String formatString, Supplier<?> supplier, String expected)
  {
    assertEquals(
        expected,
        messageSupport
            .message(formatString)
            .with("v", supplier)
            .format());
  }


  @Test
  @DisplayName("Classifiers of the supplier and of the supplied value")
  void classifiers()
  {
    final Supplier<List<String>> list = () -> List.of("a", "b");

    assertEquals(
        "deferred",
        messageSupport
          .message("%{v,format:classifier,'supplier':'deferred','list':'a list'}")
          .with("v", list)
          .format());
    assertEquals(
        "a list",
        messageSupport
          .message("%{v,format:classifier,'string':'text','list':'a list'}")
          .with("v", list)
          .format());
  }


  @Test
  @DisplayName("Supplier passed as a parameter is evaluated once")
  void supplierEvaluatedOnce()
  {
    var calls = new AtomicInteger();
    final Supplier<List<String>> list = () -> {
      calls.incrementAndGet();
      return List.of("a", "b");
    };

    assertEquals(
        "a, b 2 some",
        messageSupport
          .message("%{l} %{l,format:size} %{l,empty:none,!empty:some}")
          .with("l", list)
          .format());
    assertEquals(
        1,
        calls.get());
  }
}
