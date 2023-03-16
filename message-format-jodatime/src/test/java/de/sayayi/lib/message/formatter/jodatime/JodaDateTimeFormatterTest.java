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
package de.sayayi.lib.message.formatter.jodatime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.parameter.value.ConfigValueString;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePart.Text.EMPTY;
import static java.util.Collections.singletonMap;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class JodaDateTimeFormatterTest extends AbstractFormatterTest
{
  @Test
  void testLocalDate()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new JodaDateTimeFormatter()), NO_CACHE_INSTANCE)
        .setLocale(GERMANY)
        .getAccessor();
    val date = new LocalDate(1972, 8, 17);

    assertEquals(new TextPart("17.08.72"), format(accessor, date,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("short"))));
    assertEquals(new TextPart("17.08.1972"), format(accessor, date,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("medium"))));
    assertEquals(new TextPart("17. August 1972"), format(accessor, date,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("long"))));
    assertEquals(new TextPart("Donnerstag, 17. August 1972"), format(accessor, date,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("full"))));
    assertEquals(new TextPart("17.08.1972"), format(accessor, date,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("date"))));

    assertEquals(EMPTY, format(accessor, date,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("time"))));
  }


  @Test
  void testLocalTime()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new JodaDateTimeFormatter()), NO_CACHE_INSTANCE)
        .setLocale(GERMANY)
        .getAccessor();
    val time = new LocalTime(16, 34, 11, 672);

    assertEquals(new TextPart("16:34"), format(accessor, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("short"))));
    assertEquals(new TextPart("16:34:11"), format(accessor, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("medium"))));
    assertEquals(new TextPart("16:34:11"), format(accessor, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("long"))));
    assertEquals(new TextPart("16:34 Uhr"), format(accessor, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("full"))));
    assertEquals(new TextPart("16:34:11"), format(accessor, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("time"))));

    assertEquals(EMPTY, format(accessor, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("date"))));
  }


  @Test
  void testDateTime()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new JodaDateTimeFormatter()), NO_CACHE_INSTANCE)
        .setLocale(UK)
        .getAccessor();
    val datetime = new DateTime(1972, 8, 17, 2, 40, 23, 833);

    assertEquals(new TextPart("17/08/72 02:40"), format(accessor, datetime,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("short"))));
    assertEquals(new TextPart("17-Aug-1972 02:40:23"), format(accessor, datetime,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("medium"))));
    assertEquals(new TextPart("17 August 1972 02:40:23 CET"), format(accessor, datetime,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("long"))));
    assertEquals(new TextPart("Thursday, 17 August 1972 02:40:23 o'clock CET"), format(accessor, datetime,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("full"))));

    assertEquals(new TextPart("17-Aug-1972"), format(accessor, datetime,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("date"))));
    assertEquals(new TextPart("02:40:23"), format(accessor, datetime,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("time"))));
  }


  @Test
  void testCustomPattern()
  {
    val accessor = MessageSupportFactory
        .create(createFormatterService(new JodaDateTimeFormatter()), NO_CACHE_INSTANCE)
        .setLocale(FRANCE)
        .getAccessor();
    val datetime = new DateTime(1972, 8, 17, 2, 40, 23, 833);

    assertEquals(new TextPart("17 août"), format(accessor, datetime,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("dd MMMM"))));
    assertEquals(new TextPart("jeu. jeudi"), format(accessor, datetime,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("EEE EEEE"))));
    assertEquals(new TextPart("02:40:23,833"), format(accessor, datetime,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("HH:mm:ss,SSS"))));
  }


  @Test
  void testFormatter()
  {
    val message = MessageSupportFactory
        .create(createFormatterService(new JodaDateTimeFormatter()), NO_CACHE_INSTANCE)
        .message("%{a} %{b,date:'short'} %{c,date:'time'} %{c,date:'yyyy-MM-dd MMM'}");

    assertEquals("17-aug-1972 16:45 14:23:01 2019-02-19 feb", message
        .with("a", new LocalDate(1972, 8, 17))
        .with("b", new LocalTime(16, 45, 9, 123))
        .with("c", new DateTime(2019, 2, 19, 14, 23, 1, 9))
        .locale("nl")
        .format());
  }


  @Test
  void testMap()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new JodaDateTimeFormatter()), NO_CACHE_INSTANCE);

    assertEquals("2020", messageSupport
        .message("%{d,!null:'%{d,date:'yyyy'}'}")
        .with("d", new LocalDate(2020, 1, 1))
        .format());

    assertEquals("empty", messageSupport
        .message("%{d,empty:'empty'}")
        .with("d", null)
        .format());

    assertEquals("empty", messageSupport
        .message("%{d,date:'time',empty:'empty'}")
        .with("d", LocalDate.now())
        .format());
  }
}
