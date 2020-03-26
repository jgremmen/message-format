/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.data.ParameterString;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import de.sayayi.lib.message.formatter.support.GeoFormatter.Format;
import org.junit.Test;

import static de.sayayi.lib.message.MessageFactory.parse;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMANY;
import static java.util.Locale.ROOT;
import static java.util.Locale.UK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class GeoFormatterTest extends AbstractFormatterTest
{
  private static double dms(int degree, int minutes) {
    return degree + minutes / 60.0;
  }


  private static double dms(int degree, int minutes, int seconds, int milliseconds) {
    return degree + (minutes + (seconds + milliseconds / 1000.0) / 60.0) / 60.0;
  }


  @Test
  public void testDmsSplitterDegree()
  {
    double[] dms;

    dms = GeoFormatter.dmsSplitter(new Format(), dms(124, 30));
    assertEquals(125.0, dms[0], 1e-3);
    assertEquals(0.0, dms[1], 1e-3);
    assertEquals(0.0, dms[2], 1e-3);

    dms = GeoFormatter.dmsSplitter(new Format(), dms(127, 29));
    assertEquals(127.0, dms[0], 1e-3);
    assertEquals(0.0, dms[1], 1e-3);
    assertEquals(0.0, dms[2], 1e-3);
  }


  @Test
  public void testDmsSplitterMinute()
  {
    double[] dms;

    dms = GeoFormatter.dmsSplitter(
        new Format(null, 0, -1),
        dms(4, 30, 30, 0));
    assertEquals(4.0, dms[0], 1e-3);
    assertEquals(31.0, dms[1], 1e-3);
    assertEquals(0.0, dms[2], 1e-3);

    dms = GeoFormatter.dmsSplitter(
        new Format(null, 3, -1),
        dms(4, 30, 10, 0));
    assertEquals(4.0, dms[0], 1e-3);
    assertEquals(30.167, dms[1], 1e-3);
    assertEquals(0.0, dms[2], 1e-3);

    dms = GeoFormatter.dmsSplitter(
        new Format(null, 2, -1),
        dms(4, 30, 10, 0));
    assertEquals(4.0, dms[0], 1e-3);
    assertEquals(30.17, dms[1], 1e-3);
    assertEquals(0.0, dms[2], 1e-3);

    dms = GeoFormatter.dmsSplitter(
        new Format(null, 1, -1),
        dms(4, 30, 10, 0));
    assertEquals(4.0, dms[0], 1e-3);
    assertEquals(30.2, dms[1], 1e-3);
    assertEquals(0.0, dms[2], 1e-3);
  }


  @Test
  public void testFormatLongitude()
  {
    final GeoFormatter formatter = new GeoFormatter();

    // short-longitude
    assertEquals("4°48'E", formatter.format(dms(4, 48), null,
        ParameterFactory.createFor(ROOT).noParameters(), new ParameterString("short-longitude")));

    // longitude
    assertEquals("19°0'0\"W",
        formatter.format(-dms(18, 59, 59, 501), null,
        ParameterFactory.createFor(ROOT).noParameters(), new ParameterString("longitude")));

    // medium-longitude
    assertEquals("18°59'59,9\"E",
        formatter.format(dms(18, 59, 59, 891), null,
        ParameterFactory.createFor(GERMANY).noParameters(), new ParameterString("medium-longitude")));

    // long-longitude
    assertEquals("18°59'59.891\"W",
        formatter.format(-dms(18, 59, 59, 891), null,
            ParameterFactory.createFor(UK).noParameters(), new ParameterString("long-longitude")));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new GeoFormatter());
    ParameterFactory factory = ParameterFactory.createFor(ENGLISH, formatterRegistry);

    final Parameters parameters = factory
        .with("lat", dms(51, 34, 9, 0))
        .with("lon", dms(4, 48));

    assertEquals("coordinates 4°48'0\"E, 51°34'9\"N",
        parse("coordinates %{lon,geo,'longitude'}, %{lat,geo,'latitude'}").format(parameters));

    assertEquals("coordinates 4°48.0' E, 51°34'9.000\"N",
        parse("coordinates %{lon,geo,'dM LO'}, %{lat,geo,'long-latitude'}").format(parameters));

    assertEquals("51°34'09\"N", parse("%{lat,geo,'d0m0sLA'}").format(parameters));
  }


  @Test
  public void testParseFormatString()
  {
    Format fmt;

    fmt = GeoFormatter.parseFormatString("d 0mSS LA");
    assertEquals(Boolean.FALSE, fmt.longitude);
    assertTrue(fmt.separatorAfterDegree);
    assertFalse(fmt.separatorAfterMinute);
    assertTrue(fmt.separatorAfterSecond);
    assertEquals(0, fmt.minuteDigits);
    assertEquals(2, fmt.secondDigits);
    assertTrue(fmt.zeroPadMinutes);
    assertFalse(fmt.zeroPadSeconds);

    fmt = GeoFormatter.parseFormatString("d0m0s");
    assertNull(fmt.longitude);
    assertFalse(fmt.separatorAfterDegree);
    assertFalse(fmt.separatorAfterMinute);
    assertFalse(fmt.separatorAfterSecond);
    assertEquals(0, fmt.minuteDigits);
    assertEquals(0, fmt.secondDigits);
    assertTrue(fmt.zeroPadMinutes);
    assertTrue(fmt.zeroPadSeconds);

    fmt = GeoFormatter.parseFormatString("dmLO");
    assertEquals(Boolean.TRUE, fmt.longitude);
    assertFalse(fmt.separatorAfterDegree);
    assertFalse(fmt.separatorAfterMinute);
    assertFalse(fmt.separatorAfterSecond);
    assertEquals(0, fmt.minuteDigits);
    assertEquals(-1, fmt.secondDigits);
    assertFalse(fmt.zeroPadMinutes);
    assertFalse(fmt.zeroPadSeconds);

    fmt = GeoFormatter.parseFormatString("d 0MMM 0SSS ");
    assertNull(fmt.longitude);
    assertTrue(fmt.separatorAfterDegree);
    assertTrue(fmt.separatorAfterMinute);
    assertFalse(fmt.separatorAfterSecond);
    assertEquals(0, fmt.minuteDigits);
    assertEquals(3, fmt.secondDigits);
    assertTrue(fmt.zeroPadMinutes);
    assertTrue(fmt.zeroPadSeconds);

    fmt = GeoFormatter.parseFormatString("d LA");
    assertEquals(Boolean.FALSE, fmt.longitude);
    assertTrue(fmt.separatorAfterDegree);
    assertFalse(fmt.separatorAfterMinute);
    assertFalse(fmt.separatorAfterSecond);
    assertEquals(-1, fmt.minuteDigits);
    assertEquals(-1, fmt.secondDigits);
    assertFalse(fmt.zeroPadMinutes);
    assertFalse(fmt.zeroPadSeconds);
  }
}
