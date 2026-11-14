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
package de.sayayi.lib.message.icu.formatter;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.Locale.ENGLISH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link ICUPersonFormatter}: rendering of person names with configurable name part length, ordering and
 * visibility, including prefixes, suffixes and missing names.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("'icu-person' formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ICUPersonFormatterTest extends AbstractFormatterTest
{
  private static MessageSupport messageSupport;


  @BeforeAll
  static void init()
  {
    val formatterService = new GenericFormatterService();
    formatterService.addFormatter(new ICUPersonFormatter());

    messageSupport = MessageSupportFactory
        .create(formatterService)
        .setLocale(ENGLISH);
  }


  static Stream<Arguments> personNames()
  {
    final var johnSmith = Map.of("given-name", "John", "family-name", "Smith");
    final var johnMichaelSmith = Map.of("given-name", "John", "middle-name", "Michael", "family-name", "Smith");
    final var drJohnSmith = Map.of("prefix", "Dr.", "given-name", "John", "family-name", "Smith");
    final var johnSmithJr = Map.of("given-name", "John", "family-name", "Smith", "suffix", "Jr.");

    return Stream.of(
        arguments("Full name with defaults",
            "", johnSmith, "John Smith"),
        arguments("Given name as initial, full family name",
            ",given-format:initial,family-format:full", johnSmith, "J. Smith"),
        arguments("Full given name, family name as initial",
            ",given-format:full,family-format:initial", johnSmith, "John S."),
        arguments("Surname-first order",
            ",order:surname-first", johnSmith, "Smith John"),
        arguments("Sorting order",
            ",order:sorting", johnSmith, "Smith, John"),
        arguments("Full name with middle name",
            ",middle-format:full", johnMichaelSmith, "John Michael Smith"),
        arguments("Middle name as initial",
            ",middle-format:initial", johnMichaelSmith, "John M. Smith"),
        arguments("Middle name excluded",
            ",middle-format:none", johnMichaelSmith, "John Smith"),
        arguments("With prefix",
            ",prefix-format:full", drJohnSmith, "Dr. John Smith"),
        arguments("Prefix excluded",
            ",prefix-format:none", drJohnSmith, "John Smith"),
        arguments("With suffix",
            ",suffix-format:full", johnSmithJr, "John Smith Jr."),
        arguments("Suffix excluded",
            ",suffix-format:none", johnSmithJr, "John Smith"),
        arguments("Given name only",
            ",family-format:none", johnSmith, "John"),
        arguments("Family name only",
            ",given-format:none", johnSmith, "Smith"),
        arguments("All initials",
            ",given-format:initial,middle-format:initial,family-format:initial", johnMichaelSmith, "J. M. S."),
        arguments("Sorting order with initials",
            ",order:sorting,given-format:initial,family-format:full", johnSmith, "Smith, J."),
        arguments("Full name with all parts",
            ",middle-format:full,prefix-format:full,suffix-format:full",
            Map.of("prefix", "Mr.", "given-name", "James", "middle-name", "Earl", "family-name", "Carter",
                "suffix", "Jr."),
            "Mr. James Earl Carter Jr."),
        arguments("Surname-first with prefix and suffix",
            ",order:surname-first,middle-format:initial,prefix-format:full,suffix-format:full",
            Map.of("prefix", "Dr.", "given-name", "Jane", "middle-name", "Marie", "family-name", "Doe",
                "suffix", "Ph.D."),
            "Doe Dr. Jane M. Ph.D.")
    );
  }


  @ParameterizedTest(name = "{0}")
  @DisplayName("Person names are formatted according to the configuration")
  @MethodSource("personNames")
  void testPersonName(@SuppressWarnings("unused") String name, String configuration, Map<String,String> names,
                      String expected)
  {
    val msg = messageSupport.message("%{unused,format:icu-person" + configuration + "}");

    names.forEach(msg::with);

    assertEquals(expected, msg.format());
  }


  @Test
  @DisplayName("Null given and family name returns null text")
  void testNullNames()
  {
    val msg = messageSupport
        .message("%{unused,format:icu-person,null:'unknown'}")
        .with("given-name", null)
        .with("family-name", null);

    assertEquals("unknown", msg.format());
  }
}
