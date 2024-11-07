/*
 * Copyright 2024 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
@DisplayName("Stream formatter")
class StreamFormatterTest extends AbstractFormatterTest
{
  @Test
  @DisplayName("Formattable types")
  void testFormattableTypes() {
    assertFormatterForType(new StreamFormatter(), Stream.class);
  }


  private static Stream<Arguments> crossTableParameters()
  {
    return Stream.of(
        Arguments.of("Empty stream with max size 0", new String[0], null, 0, null, ""),
        Arguments.of("Stream with last separator", new String[] { "A", "B", "C" }, " and ", null, null, "A, B and C"),
        Arguments.of("Stream with max size 2 and more value", new String[] { "A", "B", "C" }, null, 2, "...", "A, B, ..."),
        Arguments.of("Stream with max size 2 and last separator", new String[] { "A", "B", "C" }, " and ", 2, null, "A and B"),
        Arguments.of("Stream with max size 1 and last separator", new String[] { "A", "B", "C" }, " and ", 1, null, "A"),
        Arguments.of("Non-empty stream with max size 0 and last separator", new String[] { "A", "B", "C" }, null, 0, null, ""),
        Arguments.of("Non-empty stream with max size 0 and more value", new String[] { "A", "B", "C" }, null, 0, "...", "..."),
        Arguments.of("Stream without last separator and more value", new String[] { "A", "B", "C" }, null, null, null, "A, B, C"),
        Arguments.of("Stream with max size 2", new String[] { "A", "B", "C" }, null, 2, null, "A, B"),
        Arguments.of("Stream with max size 1", new String[] { "A", "B", "C" }, null, 1, null, "A"),
        Arguments.of("Non-empty stream with max size 0", new String[] { "A", "B", "C" }, null, 0, null, ""),
        Arguments.of("Stream with empty element and max size 2", new String[] { "A", "", "C", "D" }, null, 2, null, "A, C")
    );
  }


  @DisplayName("Max size, last separator and more value")
  @ParameterizedTest(name = "{0}")
  @MethodSource("crossTableParameters")
  void crossTable(@NotNull String name, String[] array, String listSepLast, Integer listMaxSize,
                  String listValueMore, @NotNull String result)
  {
    val messageFormat = new StringBuilder("%{stream");
    if (listSepLast != null)
      messageFormat.append(",list-sep-last:\"").append(listSepLast).append('"');
    if (listMaxSize != null)
      messageFormat.append(",list-max-size:").append(listMaxSize);
    if (listValueMore != null)
      messageFormat.append(",list-value-more:\"").append(listValueMore).append('"');
    messageFormat.append("}");

    val message = MessageSupportFactory
        .create(createFormatterService(new StreamFormatter()), NO_CACHE_INSTANCE)
        .message(messageFormat.toString())
        .with("stream", Stream.of(array));

    assertEquals(result, message.format());
  }
}
