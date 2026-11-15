/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.named.extra;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.exception.MessageFormatException;
import de.sayayi.lib.message.formatter.parameter.named.extra.GeoFormatter.Format;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Stream;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link GeoFormatter}: splitting into degrees, minutes and seconds, formatting of coordinates, parsing of format
 * strings, and handling of non finite values and invalid patterns.
 *
 * @author Jeroen Gremmen
 * @since 0.3.1
 */
@DisplayName("'geo' formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("UnnecessaryUnicodeEscape")
final class GeoFormatterTest extends AbstractFormatterTest
{
  private final MessageSupport messageSupport = createMessageSupport(ENGLISH, new GeoFormatter());


  private static double dms(int degree, int minutes) {
    return degree + minutes / 60.0;
  }


  private static double dms(int degree, int minutes, int seconds, int milliseconds) {
    return degree + (minutes + (seconds + milliseconds / 1000.0) / 60.0) / 60.0;
  }


  @Test
  @DisplayName("Rounds degree values")
  void dmsSplitterDegree()
  {
    assertDms(GeoFormatter.dmsSplitter(new Format(), dms(124, 30)), 125.0, 0.0, 0.0);
    assertDms(GeoFormatter.dmsSplitter(new Format(), dms(127, 29)), 127.0, 0.0, 0.0);
  }


  static Stream<Arguments> minutePrecisions()
  {
    return Stream.of(
        arguments(0, dms(4, 30, 30, 0), 4.0, 31.0, 0.0),
        arguments(3, dms(4, 30, 10, 0), 4.0, 30.167, 0.0),
        arguments(2, dms(4, 30, 10, 0), 4.0, 30.17, 0.0),
        arguments(1, dms(4, 30, 10, 0), 4.0, 30.2, 0.0));
  }


  @ParameterizedTest(name = "{0} minute digits -> {2}, {3}, {4}")
  @DisplayName("Rounds minute values to the configured precision")
  @MethodSource("minutePrecisions")
  void dmsSplitterMinute(int minuteDigits, double value, double degree, double minutes, double seconds)
  {
    assertDms(GeoFormatter.dmsSplitter(new Format(null, minuteDigits, -1), value), degree, minutes, seconds);
  }


  static Stream<Arguments> predefinedLongitudeFormats()
  {
    return Stream.of(
        arguments("short-longitude", ROOT, dms(4, 48), "4\u00b048'E"),
        arguments("longitude", ROOT, -dms(18, 59, 59, 501), "19\u00b00'0\"W"),
        arguments("medium-longitude", GERMANY, dms(18, 59, 59, 891), "18\u00b059'59,9\"E"),
        arguments("long-longitude", UK, -dms(18, 59, 59, 891), "18\u00b059'59.891\"W"));
  }


  @ParameterizedTest(name = "{0} -> {3}")
  @DisplayName("Formats longitude values using predefined formats")
  @MethodSource("predefinedLongitudeFormats")
  void formatLongitude(String geoFormat, Locale locale, double value, String expected)
  {
    assertEquals(
        noSpaceText(expected),
        format(createMessageAccessor(locale, new GeoFormatter()), value, "geo", geoFormat, "geo"));
  }


  @Test
  @DisplayName("Formats coordinates using predefined and custom formats")
  void formatter()
  {
    var parameters = new HashMap<String,Object>();

    parameters.put("lat", dms(51, 34, 9, 0));
    parameters.put("lon", dms(4, 48));

    assertEquals(
        "coordinates 4\u00b048'0\"E, 51\u00b034'9\"N",
        messageSupport
            .message("coordinates %{lon,geo:longitude}, %{lat,geo:latitude}")
            .with(parameters)
            .format());

    assertEquals(
        "coordinates 4\u00b048.0' E, 51\u00b034'9.000\"N",
        messageSupport
            .message("coordinates %{lon,geo:'dM LO'}, %{lat,geo:long-latitude}")
            .with(parameters)
            .format());

    assertEquals(
        "51\u00b034'09\"N",
        messageSupport
            .message("%{lat,geo:'d0m0sLA'}")
            .with(parameters)
            .format());

    assertEquals(
        "",
        messageSupport
            .message("%{test,format:geo,geo:'d0m0sLA'}")
            .with(parameters)
            .format());
  }


  @Test
  @DisplayName("Formats coordinates using the default format dms")
  void defaultFormat()
  {
    for(var n = 0; n < 2; n++)
    {
      assertEquals(
          "51\u00b034'9\"",
          messageSupport
              .message("%{lat,format:geo}")
              .with("lat", dms(51, 34, 9, 0))
              .format());
      assertEquals(
          messageSupport
              .message("%{lat,geo:dms}")
              .with("lat", dms(4, 48, 30, 0))
              .format(),
          messageSupport
              .message("%{lat,format:geo}")
              .with("lat", dms(4, 48, 30, 0))
              .format());
    }
  }


  @Test
  @DisplayName("Formats non-finite values as empty text")
  void nonFiniteValues()
  {
    for(var value: new Object[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Float.NaN })
    {
      assertEquals(
          "",
          messageSupport
              .message("%{lat,geo:latitude}")
              .with("lat", value)
              .format());

      assertEquals(
          "unknown",
          messageSupport
              .message("%{lat,geo:'dM LA',empty:'unknown'}")
              .with("lat", value)
              .format());
    }
  }


  @Test
  @DisplayName("Rejects an invalid custom format pattern")
  void invalidFormatPattern()
  {
    var exception = assertThrows(
        MessageFormatException.class,
        () -> messageSupport
            .message("%{lat,geo:'invalid'}")
            .with("lat", dms(51, 34, 9, 0))
            .format());
    assertInstanceOf(
        IllegalArgumentException.class,
        exception.getCause());
  }


  static Stream<Arguments> formatPatterns()
  {
    return Stream.of(
        arguments("d 0mSS LA", false, true, false, true, 0, 2, true, false),
        arguments("d0m0s", null, false, false, false, 0, 0, true, true),
        arguments("dmLO", true, false, false, false, 0, -1, false, false),
        arguments("d 0MMM 0SSS ", null, true, true, false, 0, 3, true, true),
        arguments("d LA", false, true, false, false, -1, -1, false, false));
  }


  @ParameterizedTest(name = "''{0}''")
  @DisplayName("Parses custom format patterns")
  @MethodSource("formatPatterns")
  void parseFormatString(String pattern, Boolean longitude, boolean separatorAfterDegree,
                             boolean separatorAfterMinute, boolean separatorAfterSecond, int minuteDigits,
                             int secondDigits, boolean zeroPadMinutes, boolean zeroPadSeconds)
  {
    var fmt = GeoFormatter.parseFormatString(pattern);

    assertEquals(longitude, fmt.longitude);
    assertEquals(separatorAfterDegree, fmt.separatorAfterDegree);
    assertEquals(separatorAfterMinute, fmt.separatorAfterMinute);
    assertEquals(separatorAfterSecond, fmt.separatorAfterSecond);
    assertEquals(minuteDigits, fmt.minuteDigits);
    assertEquals(secondDigits, fmt.secondDigits);
    assertEquals(zeroPadMinutes, fmt.zeroPadMinutes);
    assertEquals(zeroPadSeconds, fmt.zeroPadSeconds);
  }


  private static void assertDms(double[] actual, double degree, double minutes, double seconds)
  {
    assertEquals(degree, actual[0], 1e-3);
    assertEquals(minutes, actual[1], 1e-3);
    assertEquals(seconds, actual[2], 1e-3);
  }
}
