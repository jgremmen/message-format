/*
 * Copyright 2020 Jeroen Gremmen
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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import static de.sayayi.lib.message.MessageFactory.parse;
import static java.util.Locale.FRANCE;
import static java.util.Locale.GERMANY;
import static java.util.Locale.UK;
import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class JodaDateTimeFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    assertFormatterForType(new JodaDateTimeFormatter(), LocalDate.class);
    assertFormatterForType(new JodaDateTimeFormatter(), LocalTime.class);
    assertFormatterForType(new JodaDateTimeFormatter(), DateTime.class);
  }


  @Test
  public void testLocalDate()
  {
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final Parameters context = ParameterFactory.createFor(GERMANY).noParameters();
    LocalDate date = new LocalDate(1972, 8, 17);

    assertEquals("17.08.72", formatter.format(date, "short", context, null));
    assertEquals("17.08.1972", formatter.format(date, "medium", context, null));
    assertEquals("17. August 1972", formatter.format(date, "long", context, null));
    assertEquals("Donnerstag, 17. August 1972", formatter.format(date, "full", context, null));
    assertEquals("17.08.1972", formatter.format(date, "date", context, null));

    assertEquals("", formatter.format(date, "time", context, null));
  }


  @Test
  public void testLocalTime()
  {
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final Parameters noParameters = ParameterFactory.createFor(GERMANY).noParameters();
    LocalTime time = new LocalTime(16, 34, 11, 672);

    assertEquals("16:34", formatter.format(time, "short", noParameters, null));
    assertEquals("16:34:11", formatter.format(time, "medium", noParameters, null));
    assertEquals("16:34:11", formatter.format(time, "long", noParameters, null));
    assertEquals("16:34 Uhr", formatter.format(time, "full", noParameters, null));
    assertEquals("16:34:11", formatter.format(time, "time", noParameters, null));

    assertEquals("", formatter.format(time, "date", noParameters, null));
  }


  @Test
  public void testDateTime()
  {
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final Parameters noParameters = ParameterFactory.createFor(UK).noParameters();
    DateTime datetime = new DateTime(1972, 8, 17, 2, 40, 23, 833);

    assertEquals("17/08/72 02:40", formatter.format(datetime, "short", noParameters, null));
    assertEquals("17-Aug-1972 02:40:23", formatter.format(datetime, "medium", noParameters, null));
    assertEquals("17 August 1972 02:40:23 CET", formatter.format(datetime, "long", noParameters, null));
    assertEquals("Thursday, 17 August 1972 02:40:23 o'clock CET", formatter.format(datetime, "full", noParameters, null));

    assertEquals("17-Aug-1972", formatter.format(datetime, "date", noParameters, null));
    assertEquals("02:40:23", formatter.format(datetime, "time", noParameters, null));
  }


  @Test
  public void testCustomPattern()
  {
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final Parameters noParameters = ParameterFactory.createFor(FRANCE).noParameters();
    DateTime datetime = new DateTime(1972, 8, 17, 2, 40, 23, 833);

    assertEquals("17 août",
        formatter.format(datetime, null, noParameters, new DataString("dd MMMM")));
    assertEquals("jeu. jeudi",
        formatter.format(datetime, null, noParameters, new DataString("EEE EEEE")));
    assertEquals("02:40:23,833",
        formatter.format(datetime, null, noParameters, new DataString("HH:mm:ss,SSS")));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new JodaDateTimeFormatter());
    final ParameterFactory factory = ParameterFactory.createFor(formatterRegistry);

    final Parameters parameters = factory
        .with("a", new LocalDate(1972, 8, 17))
        .with("b", new LocalTime(16, 45, 9, 123))
        .with("c", new DateTime(2019, 2, 19, 14, 23, 1, 9))
        .withLocale("nl");
    final Message msg = parse("%{a} %{b,short} %{c,time} %{c,'yyyy-MM-dd MMM'}");

    assertEquals("17-aug-1972 16:45 14:23:01 2019-02-19 feb", msg.format(parameters));
  }


  @Test
  public void testMap()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new JodaDateTimeFormatter());
    final ParameterFactory factory = ParameterFactory.createFor(formatterRegistry);

    assertEquals("2020", parse("%{d,{!null:'%{d,\'yyyy\'}'}}").format(
        factory.with("d", new LocalDate(2020, 1, 1))));

    assertEquals("empty", parse("%{d,{empty:'empty'}}").format(factory.with("d", null)));

    assertEquals("empty", parse("%{d,time,{empty:'empty'}}").format(
        factory.with("d", LocalDate.now())));
  }
}
