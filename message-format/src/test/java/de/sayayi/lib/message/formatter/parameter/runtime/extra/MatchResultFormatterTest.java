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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link MatchResultFormatter}: formattable types and formatting of match results, including access by group
 * number and group name.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@DisplayName("Matcher formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MatchResultFormatterTest extends AbstractFormatterTest
{
  private static final Pattern VERSION_PATTERN = Pattern.compile("[vV]?(?<major>\\d+)(\\.(?<minor>\\d+))?");

  private MessageSupport context;


  @BeforeEach
  void init() {
    context = createMessageSupport(new MatchResultFormatter());
  }


  @Test
  @DisplayName("Formattable types")
  void testFormattableTypes() {
    assertFormatterForType(new MatchResultFormatter(), Matcher.class);
  }


  static Stream<Arguments> groupNumbers()
  {
    return Stream.of(
        arguments("%{m,matcher:0}", "v5.12"),
        arguments("%{m,matcher:1}", "5"),
        arguments("%{m,matcher:2}", ".12"),
        arguments("%{m,matcher:3}", "12"),
        arguments("%{m,matcher:-1}", ""),
        arguments("%{m,matcher:800}", "")
    );
  }


  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("Formats a group selected by number")
  @MethodSource("groupNumbers")
  void testFormatGroupNumber(@Language("MessageFormat") String formatString, String expected)
  {
    final var matcher = VERSION_PATTERN.matcher("v5.12");

    assertEquals(
        expected,
        context
            .message(formatString)
            .with("m", matcher)
            .format());
  }


  static Stream<Arguments> groupNames()
  {
    return Stream.of(
        arguments("%{m,matcher:'major'}", "5"),
        arguments("%{m,matcher:'minor'}", "12"),
        arguments("%{m,matcher:'haha'}", ""),
        arguments("%{m,matcher:''}", "")
    );
  }


  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("Formats a group selected by name")
  @MethodSource("groupNames")
  void testFormatGroupName(@Language("MessageFormat") String formatString, String expected)
  {
    final var matcher = VERSION_PATTERN.matcher("v5.12");

    assertEquals(
        expected,
        context
            .message(formatString)
            .with("m", matcher)
            .format());
  }


  @Test
  @DisplayName("Formats the whole match by default")
  void testFormat()
  {
    final var matcher = VERSION_PATTERN.matcher("v4.999");

    assertEquals(
        "v4.999",
        context
            .message("%{m}")
            .with("m", matcher)
            .format());
  }
}
