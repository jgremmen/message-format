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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.formatter.support.GeoFormatter.Format;
import de.sayayi.lib.message.internal.part.TextPart;
import org.junit.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
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
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE,
        ROOT);
    final GeoFormatter formatter = new GeoFormatter();

    // short-longitude
    assertEquals(new TextPart("4°48'E"), formatter.format(context, dms(4, 48), null,
        context.noParameters(), new DataString("short-longitude")));

    // longitude
    assertEquals(new TextPart("19°0'0\"W"),
        formatter.format(context, -dms(18, 59, 59, 501), null,
        context.noParameters(), new DataString("longitude")));

    // medium-longitude
    assertEquals(new TextPart("18°59'59,9\"E"),
        formatter.format(context, dms(18, 59, 59, 891), null,
        context.parameters().withLocale(GERMANY), new DataString("medium-longitude")));

    // long-longitude
    assertEquals(new TextPart("18°59'59.891\"W"),
        formatter.format(context, -dms(18, 59, 59, 891), null,
            context.parameters().withLocale(UK), new DataString("long-longitude")));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new GeoFormatter());
    final MessageContext context = new MessageContext(formatterRegistry, NO_CACHE_INSTANCE, ENGLISH);

    final Parameters parameters = context.parameters()
        .with("lat", dms(51, 34, 9, 0))
        .with("lon", dms(4, 48));

    assertEquals("coordinates 4°48'0\"E, 51°34'9\"N",
        context.getMessageFactory().parse("coordinates %{lon,geo,'longitude'}, %{lat,geo,'latitude'}")
            .format(context, parameters));

    assertEquals("coordinates 4°48.0' E, 51°34'9.000\"N",
        context.getMessageFactory().parse("coordinates %{lon,geo,'dM LO'}, %{lat,geo,'long-latitude'}")
            .format(context, parameters));

    assertEquals("51°34'09\"N", context.getMessageFactory().parse("%{lat,geo,'d0m0sLA'}")
        .format(context, parameters));
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
