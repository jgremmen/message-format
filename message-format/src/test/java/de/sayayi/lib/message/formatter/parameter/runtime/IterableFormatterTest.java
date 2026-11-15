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

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.parameter.named.SizeFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static de.sayayi.lib.message.util.PackTestSupport.packRoundTrip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link IterableFormatter}: formattable types, escaped list values and non-collection iterables. The list
 * configuration shared with {@link ArrayFormatter} is tested by {@link ListFormatterConfigTest}.
 *
 * @author Jeroen Gremmen
 * @since 0.6.0
 */
@DisplayName("Iterable formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class IterableFormatterTest extends AbstractFormatterTest
{
  @Test
  @DisplayName("Formattable types include Iterable")
  void formattableTypes() {
    assertFormatterForType(new IterableFormatter(), Iterable.class);
  }


  @Test
  @DisplayName("Escaped parameter in string list value stays text")
  void listValueEscapedParameter() throws IOException
  {
    var formatterService = createFormatterService(new IterableFormatter());
    var messageSupport = MessageSupportFactory.create(formatterService);

    assertEquals(
        "<1>, <2>",
        messageSupport
            .message("%{c,list-value:'<%{value}>'}")
            .with("c", List.of(1, 2))
            .format());
    assertEquals(
        "<%{value}>, <%{value}>",
        messageSupport
            .message("%{c,list-value:'<\\%{value}>'}")
            .with("c", List.of(1, 2))
            .format());

    // the string value survives a pack round trip
    messageSupport.addMessage("MSG", "%{c,list-value:'<\\%{value}>'}");

    var importedMessageSupport = packRoundTrip(
        messageSupport,
        () -> MessageSupportFactory.create(formatterService));

    assertEquals(
        "<%{value}>, <%{value}>",
        importedMessageSupport
            .code("MSG")
            .with("c", List.of(1, 2))
            .format());
  }


  static Stream<Arguments> oneShotIterables()
  {
    return Stream.of(
        // the empty keys are matched against the formatted text
        arguments("empty iterable is empty", "%{c,empty:'none',!empty:'some'}", new OneShotIterable(), "none"),
        arguments("non-empty iterable is not empty", "%{c,empty:'none',!empty:'some'}",
            new OneShotIterable("a", "b"), "some"),
        arguments("iterable is formatted", "%{c}", new OneShotIterable("a", "b"), "a, b"),
        arguments("iterable has no size", "%{c,format:size,empty:'unknown'}", new OneShotIterable("a", "b"),
            "unknown"),

        // collections still report their size and emptiness
        arguments("empty collection is empty", "%{c,empty:'none',!empty:'some'}", List.of(), "none"),
        arguments("non-empty collection is not empty", "%{c,empty:'none',!empty:'some'}", List.of("a"), "some"),
        arguments("collection has a size", "%{c,format:size}", List.of("a", "b"), "2"));
  }


  @ParameterizedTest(name = "{0}")
  @DisplayName("Non-collection iterables are iterated for formatting only")
  @MethodSource("oneShotIterables")
  void oneShotIterable(String name, @Language("MessageFormat") String formatString, Iterable<String> iterable,
                           String expected)
  {
    assertEquals(
        expected,
        createMessageSupport(new IterableFormatter(), new SizeFormatter())
            .message(formatString)
            .with("c", iterable)
            .format());
  }


  @Test
  @DisplayName("One-shot test iterable can be iterated only once")
  void oneShotIterableIteratesOnce()
  {
    var iterable = new OneShotIterable("a");
    iterable.iterator();

    assertThrows(IllegalStateException.class, iterable::iterator);
  }




  private static final class OneShotIterable implements Iterable<String>
  {
    private final List<String> elements;
    private boolean iterated;


    private OneShotIterable(String... elements) {
      this.elements = List.of(elements);
    }


    @Override
    public @NotNull Iterator<String> iterator()
    {
      if (iterated)
        throw new IllegalStateException("already iterated");

      iterated = true;

      return elements.iterator();
    }
  }
}
