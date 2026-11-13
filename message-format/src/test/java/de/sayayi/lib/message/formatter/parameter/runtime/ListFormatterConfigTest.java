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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests the list configuration of {@link AbstractListFormatter} (separators, maximum size, more value, unique values,
 * {@code null} and empty values) for both of its implementations, {@link ArrayFormatter} and {@link IterableFormatter}.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("List formatter configuration")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ListFormatterConfigTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init() {
    messageSupport = MessageSupportFactory.create(createFormatterService(new ArrayFormatter(), new IterableFormatter()));
  }


  /** Returns the containers handled by the list formatters: an array and an iterable. */
  private static Stream<Arguments> containers()
  {
    return Stream.of(
        arguments("array", (Function<List<?>,Object>)List::toArray),
        arguments("iterable", (Function<List<?>,Object>)list -> list));
  }


  @DisplayName("Separators")
  @ParameterizedTest(name = "{0}")
  @MethodSource("containers")
  void testSeparator(@SuppressWarnings("unused") String name, @NotNull Function<List<?>,Object> container)
  {
    val values = container.apply(List.of(1, 2, 3, 4, 5));

    assertEquals("1, 2, 3, 4 and 5", messageSupport
        .message("%{c,list-sep:', ',list-sep-last:' and '}")
        .with("c", values)
        .format());
    assertEquals("1.2.3.4.5", messageSupport
        .message("%{c,list-sep:'.'}")
        .with("c", values)
        .format());
  }


  @DisplayName("Empty and null values")
  @ParameterizedTest(name = "{0}")
  @MethodSource("containers")
  void testEmptyOrNull(@SuppressWarnings("unused") String name, @NotNull Function<List<?>,Object> container)
  {
    val message = messageSupport.message("%{c,null:null,empty:empty}");

    assertEquals("null", message.with("c", null).format());
    assertEquals("empty", message.with("c", container.apply(List.of())).format());
  }


  @DisplayName("Null elements are skipped")
  @ParameterizedTest(name = "{0}")
  @MethodSource("containers")
  void testNullElements(@SuppressWarnings("unused") String name, @NotNull Function<List<?>,Object> container)
  {
    assertEquals("Test, true, -6", messageSupport
        .message("%{c}")
        .with("c", container.apply(asList("Test", true, null, -6)))
        .format());
  }


  @DisplayName("Unique values with max size and more value")
  @ParameterizedTest(name = "{0}")
  @MethodSource("containers")
  void testUnique(@SuppressWarnings("unused") String name, @NotNull Function<List<?>,Object> container)
  {
    assertEquals("1, 2, ...", messageSupport
        .message("%{c,list-max-size:2,list-value-more:'...',list-unique:true}")
        .with("c", container.apply(List.of(1, 1, 2, 2, 3, 3, 4, 4)))
        .format());
  }


  private static Stream<Arguments> crossTableParameters()
  {
    val rows = Stream.of(
        arguments("Empty with max size 0", List.of(), null, 0, null, ""),
        arguments("Last separator", List.of(" A ", "B", "C"), " and ", null, null, "A, B and C"),
        arguments("Max size 2 and more value", List.of("A", "B", "C"), null, 2, "...", "A, B, ..."),
        arguments("Max size 2, more value and 3 overflow elements", List.of("A", "B", "C", "D", "E"), null, 2, "...", "A, B, ..."),
        arguments("Max size 1, more value and last separator", List.of("A", "B", "C", "D"), " and ", 1, "...", "A, ..."),
        arguments("Max size equal to length and more value", List.of("A", "B", "C"), null, 3, "...", "A, B, C"),
        arguments("Max size 2 and last separator", List.of("A", "B", "C"), " and ", 2, null, "A and B"),
        arguments("Max size 1 and last separator", List.of("A", "B", "C"), " and ", 1, null, "A"),
        arguments("Non-empty with max size 0 and last separator", List.of("A", "B", "C"), null, 0, null, ""),
        arguments("Non-empty with max size 0 and more value", List.of("A", "B", "C"), null, 0, "...", "..."),
        arguments("Without last separator and more value", List.of("A", "B", "C"), null, null, null, "A, B, C"),
        arguments("Max size 2", List.of("A", "B", "C"), null, 2, null, "A, B"),
        arguments("Max size 1", List.of("A", "B", "C"), null, 1, null, "A"),
        arguments("Non-empty with max size 0", List.of("A", "B", "C"), null, 0, null, ""),
        arguments("Empty element and max size 2", List.of("A", "", "C", "D"), null, 2, null, "A, C")
    ).toList();

    return containers().flatMap(container -> rows.stream().map(row -> {
      val c = container.get();
      val r = row.get();

      return arguments(c[0] + ": " + r[0], c[1], r[1], r[2], r[3], r[4], r[5]);
    }));
  }


  @DisplayName("Max size, last separator and more value")
  @ParameterizedTest(name = "{0}")
  @MethodSource("crossTableParameters")
  void crossTable(@SuppressWarnings("unused") String name, @NotNull Function<List<?>,Object> container,
                  @NotNull List<?> values, String listSepLast, Integer listMaxSize, String listValueMore,
                  @NotNull String result)
  {
    val messageFormat = new StringBuilder("%{c");
    if (listSepLast != null)
      messageFormat.append(",list-sep-last:\"").append(listSepLast).append('"');
    if (listMaxSize != null)
      messageFormat.append(",list-max-size:").append(listMaxSize);
    if (listValueMore != null)
      messageFormat.append(",list-value-more:\"").append(listValueMore).append('"');
    messageFormat.append("}");

    assertEquals(result, messageSupport
        .message(messageFormat.toString())
        .with("c", container.apply(values))
        .format());
  }
}
