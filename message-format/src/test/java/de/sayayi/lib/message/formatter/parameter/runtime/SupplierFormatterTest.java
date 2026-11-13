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
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.parameter.named.ClassifierFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;


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
  private MessageSupport messageSupport;


  @BeforeEach
  void init()
  {
    val formatterService = new DefaultFormatterService();
    formatterService.addFormatter(new ClassifierFormatter());

    messageSupport = MessageSupportFactory.create(formatterService);
  }


  @Test
  void testFormattableTypes() {
    assertFormatterForType(new SupplierFormatter(), Supplier.class);
  }


  @Test
  @DisplayName("Supplied value is formatted according to its type")
  void testFormat()
  {
    final Supplier<List<String>> names = () -> List.of("alice", "bob", "carol");

    assertEquals("Users: alice, bob and carol", messageSupport
        .message("Users: %{names,list-sep-last:' and '}")
        .with("names", names)
        .format());
  }


  @Test
  @DisplayName("Map keys and size are based on the supplied value")
  void testMapKeysAndSize()
  {
    final Supplier<String> empty = () -> "";
    final Supplier<Integer> one = () -> 1;
    final Supplier<Boolean> yes = () -> true;
    final Supplier<String> text = () -> "abc";
    final Supplier<List<String>> list = () -> List.of("a", "b", "c");

    assertEquals("no comment", messageSupport
        .message("%{c,empty:'no comment'}")
        .with("c", empty)
        .format());
    assertEquals("one", messageSupport
        .message("%{n,1:one,:other}")
        .with("n", one)
        .format());
    assertEquals("yes", messageSupport
        .message("%{b,true:yes,false:no}")
        .with("b", yes)
        .format());
    assertEquals("ABC", messageSupport
        .message("%{s,format:choice,'abc':ABC,:other}")
        .with("s", text)
        .format());
    assertEquals("3", messageSupport
        .message("%{l,format:size}")
        .with("l", list)
        .format());
  }


  @Test
  @DisplayName("Classifiers of the supplier and of the supplied value")
  void testClassifiers()
  {
    final Supplier<List<String>> list = () -> List.of("a", "b");

    assertEquals("deferred", messageSupport
        .message("%{v,format:classifier,'supplier':'deferred','list':'a list'}")
        .with("v", list)
        .format());
    assertEquals("a list", messageSupport
        .message("%{v,format:classifier,'string':'text','list':'a list'}")
        .with("v", list)
        .format());
  }


  @Test
  @DisplayName("Supplier passed as a parameter is evaluated once")
  void testSupplierEvaluatedOnce()
  {
    val calls = new AtomicInteger();
    final Supplier<List<String>> list = () -> {
      calls.incrementAndGet();
      return List.of("a", "b");
    };

    assertEquals("a, b 2 some", messageSupport
        .message("%{l} %{l,format:size} %{l,empty:none,!empty:some}")
        .with("l", list)
        .format());
    assertEquals(1, calls.get());
  }
}
