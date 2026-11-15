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

import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.util.DefaultTimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.*;
import java.time.temporal.Temporal;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link TemporalFormatter}: formattable types, date, time and date time values, custom patterns, explicit zones,
 * partial temporals and invalid patterns.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
@DisplayName("Temporal formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DefaultTimeZone("Europe/Berlin")
final class TemporalFormatterTest extends AbstractFormatterTest
{
  @Test
  @DisplayName("Reports the supported temporal types")
  void formattableTypes()
  {
    assertFormatterForType(new TemporalFormatter(), LocalDate.class);
    assertFormatterForType(new TemporalFormatter(), LocalTime.class);
    assertFormatterForType(new TemporalFormatter(), LocalDateTime.class);
  }


  static Stream<Arguments> localDateStyles()
  {
    return Stream.of(
        arguments("short", noSpaceText("17.08.72")),
        arguments("medium", noSpaceText("17.08.1972")),
        arguments("long", noSpaceText("17. August 1972")),
        arguments("full", noSpaceText("Donnerstag, 17. August 1972")),
        arguments("date", noSpaceText("17.08.1972")),
        arguments("time", emptyText())
    );
  }


  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("Formats local dates using localized styles")
  @MethodSource("localDateStyles")
  void localDate(String style, Text expected)
  {
    assertEquals(
        expected,
        format(
            createMessageAccessor(GERMANY, new TemporalFormatter()),
            LocalDate.of(1972, 8, 17), "date", style));
  }


  static Stream<Arguments> localTimeStyles()
  {
    return Stream.of(
        arguments("short", new TextPart("16:34")),
        arguments("medium", new TextPart("16:34:11")),
        arguments("long", new TextPart("16:34:11 MEZ")),
        arguments("full", new TextPart("16:34:11 Mitteleuropäische Zeit")),
        arguments("time", new TextPart("16:34:11")),
        arguments("date", Text.EMPTY)
    );
  }


  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("Formats local times using localized styles")
  @MethodSource("localTimeStyles")
  void localTime(String style, Text expected)
  {
    assertEquals(
        expected,
        format(
            createMessageAccessor(GERMANY, new TemporalFormatter()),
            LocalTime.of(16, 34, 11, 672000000), "date", style));
  }


  static Stream<Arguments> dateTimeStyles()
  {
    return Stream.of(
        arguments("short", new TextPart("17/08/1972, 02:40")),
        arguments("medium", new TextPart("17 Aug 1972, 02:40:23")),
        arguments("long", new TextPart("17 August 1972, 02:40:23 CET")),
        arguments("full", new TextPart("Thursday, 17 August 1972, 02:40:23 Central European Standard Time")),
        arguments("date", new TextPart("17 Aug 1972")),
        arguments("time", new TextPart("02:40:23"))
    );
  }


  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("Formats local date-times using localized styles")
  @MethodSource("dateTimeStyles")
  void dateTime(String style, Text expected)
  {
    assertEquals(
        expected,
        format(
            createMessageAccessor(UK, new TemporalFormatter()),
            LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000), "date", style));
  }


  static Stream<Arguments> customPatterns()
  {
    return Stream.of(
        arguments("dd MMMM", new TextPart("17 août")),
        arguments("EEE EEEE", new TextPart("jeu. jeudi")),
        arguments("HH:mm:ss,SSS", new TextPart("02:40:23,833"))
    );
  }


  @ParameterizedTest(name = "{0} -> {1}")
  @DisplayName("Formats temporal values with custom patterns")
  @MethodSource("customPatterns")
  void customPattern(String pattern, Text expected)
  {
    assertEquals(
        expected,
        format(
            createMessageAccessor(FRANCE, new TemporalFormatter()),
            LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000), "date", pattern));
  }


  static Stream<Arguments> explicitZones()
  {
    return Stream.of(
        arguments(ZonedDateTime.of(2024, 1, 15, 12, 30, 0, 0, ZoneId.of("America/New_York")),
            "yyyy-MM-dd HH:mm XXX VV", new TextPart("2024-01-15 12:30 -05:00 America/New_York")),
        arguments(OffsetDateTime.parse("2024-01-15T12:30-05:00"),
            "yyyy-MM-dd HH:mm XXX", new TextPart("2024-01-15 12:30 -05:00")),
        arguments(Instant.parse("2024-01-15T12:30:00Z"),
            "yyyy-MM-dd HH:mm XXX VV", new TextPart("2024-01-15 13:30 +01:00 Europe/Berlin"))
    );
  }


  @ParameterizedTest(name = "{0} with {1} -> {2}")
  @DisplayName("Preserves explicit zones and uses the system zone for instants")
  @MethodSource("explicitZones")
  void preservesExplicitZones(Temporal temporal, String pattern, Text expected)
  {
    assertEquals(
        expected,
        format(
            createMessageAccessor(UK, new TemporalFormatter()),
            temporal, "date", pattern));
  }


  @Test
  @DisplayName("Formats temporal values through the message formatter")
  void formatter()
  {
    assertEquals(
        "17 aug 1972 16:45 14:23:01 2019-02-19 feb",
        createMessageSupport(new TemporalFormatter())
            .message("%{a} %{b,date:'short'} %{c,date:'time'} %{c,date:'yyyy-MM-dd MMM'}")
            .with("a", LocalDate.of(1972, 8, 17))
            .with("b", LocalTime.of(16, 45, 9, 123))
            .with("c", LocalDateTime.of(2019, 2, 19, 14, 23, 1, 9))
            .locale("nl")
            .format());
  }


  @Test
  @DisplayName("Formats partial temporals as empty text if fields are missing")
  void partialTemporalMissingFields()
  {
    var messageAccessor = createMessageAccessor(GERMANY, new TemporalFormatter());

    for(var temporal: new Object[] { Year.of(2024), YearMonth.of(2024, 3) })
    {
      assertEquals(emptyText(), format(messageAccessor, temporal), temporal::toString);

      for(var style: new String[] { "short", "medium", "long", "full", "date", "time" })
      {
        assertEquals(emptyText(), format(messageAccessor, temporal,
            "date", style), () -> temporal + " " + style);
      }
    }

    assertEquals(
        emptyText(),
        format(messageAccessor, LocalTime.of(16, 45), "date", "yyyy"));
    assertEquals(
        emptyText(),
        format(messageAccessor, LocalDate.of(2024, 3, 15), "date", "HH:mm"));
  }


  @Test
  @DisplayName("Formats partial temporals with patterns using supported fields")
  void partialTemporalSupportedFields()
  {
    var messageAccessor = createMessageAccessor(GERMANY, new TemporalFormatter());

    assertEquals(
        new TextPart("2024"),
        format(messageAccessor, Year.of(2024), "date", "yyyy"));
    assertEquals(
        new TextPart("März 2024"),
        format(messageAccessor, YearMonth.of(2024, 3), "date", "MMMM yyyy"));
  }


  @Test
  @DisplayName("Formats as empty text for an invalid pattern")
  void invalidPattern()
  {
    var messageAccessor = createMessageAccessor(new TemporalFormatter());

    assertEquals(
        emptyText(),
        format(messageAccessor, LocalDate.of(2024, 3, 15), "date", "yyyy-bb"));
  }


  @Test
  @DisplayName("Formats messages with partial temporals without throwing")
  void formatterPartialTemporal()
  {
    var messageSupport = createMessageSupport(new TemporalFormatter());

    assertEquals("[] [] [2024]", messageSupport
        .message("[%{y}] [%{ym,date:short}] [%{y,date:'yyyy'}]")
        .with("y", Year.of(2024))
        .with("ym", YearMonth.of(2024, 3))
        .locale("nl")
        .format());
  }
}
