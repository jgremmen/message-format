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

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.MessagePart.Text;
import lombok.val;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.TimeZone;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("Temporal formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class TemporalFormatterTest extends AbstractFormatterTest
{
  @BeforeAll
  static void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
  }


  @Test
  void testFormattableTypes()
  {
    assertFormatterForType(new TemporalFormatter(), LocalDate.class);
    assertFormatterForType(new TemporalFormatter(), LocalTime.class);
    assertFormatterForType(new TemporalFormatter(), LocalDateTime.class);
  }


  @Test
  void testLocalDate()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new TemporalFormatter()), NO_CACHE_INSTANCE)
        .setLocale(GERMANY)
        .getMessageAccessor();
    val date = LocalDate.of(1972, 8, 17);

    assertEquals(noSpaceText("17.08.72"),
        format(messageAccessor, date, Map.of("date", new TypedValueString("short")), Map.of()));
    assertEquals(noSpaceText("17.08.1972"),
        format(messageAccessor, date, Map.of("date", new TypedValueString("medium")), Map.of()));
    assertEquals(noSpaceText("17. August 1972"),
        format(messageAccessor, date, Map.of("date", new TypedValueString("long")), Map.of()));
    assertEquals(noSpaceText("Donnerstag, 17. August 1972"),
        format(messageAccessor, date, Map.of("date", new TypedValueString("full")), Map.of()));
    assertEquals(noSpaceText("17.08.1972"),
        format(messageAccessor, date, Map.of("date", new TypedValueString("date")), Map.of()));

    assertEquals(emptyText(), format(messageAccessor, date,
        Map.of("date", new TypedValueString("time")), Map.of()));
  }


  @Test
  void testLocalTime()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new TemporalFormatter()), NO_CACHE_INSTANCE)
        .setLocale(GERMANY)
        .getMessageAccessor();
    val time = LocalTime.of(16, 34, 11, 672000000);

    assertEquals(new TextPart("16:34"), format(messageAccessor, time,
        Map.of("date", new TypedValueString("short")), Map.of()));
    assertEquals(new TextPart("16:34:11"), format(messageAccessor, time,
        Map.of("date", new TypedValueString("medium")), Map.of()));
    assertEquals(new TextPart("16:34:11 MEZ"), format(messageAccessor, time,
        Map.of("date", new TypedValueString("long")), Map.of()));
    assertEquals(new TextPart("16:34:11 Mitteleuropäische Zeit"), format(messageAccessor, time,
        Map.of("date", new TypedValueString("full")), Map.of()));
    assertEquals(new TextPart("16:34:11"), format(messageAccessor, time,
        Map.of("date", new TypedValueString("time")), Map.of()));

    assertEquals(Text.EMPTY, format(messageAccessor, time,
        Map.of("date", new TypedValueString("date")), Map.of()));
  }


  @Test
  void testDateTime()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new TemporalFormatter()), NO_CACHE_INSTANCE)
        .setLocale(UK)
        .getMessageAccessor();
    val datetime = LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000);

    assertEquals(new TextPart("17/08/1972, 02:40"),
        format(messageAccessor, datetime, Map.of("date", new TypedValueString("short")), Map.of()));
    assertEquals(new TextPart("17 Aug 1972, 02:40:23"),
        format(messageAccessor, datetime, Map.of("date", new TypedValueString("medium")), Map.of()));
    assertEquals(new TextPart("17 August 1972, 02:40:23 CET"),
        format(messageAccessor, datetime, Map.of("date", new TypedValueString("long")), Map.of()));
    assertEquals(new TextPart("Thursday, 17 August 1972, 02:40:23 Central European Standard Time"),
        format(messageAccessor, datetime, Map.of("date", new TypedValueString("full")), Map.of()));

    assertEquals(new TextPart("17 Aug 1972"),
        format(messageAccessor, datetime, Map.of("date", new TypedValueString("date")), Map.of()));
    assertEquals(new TextPart("02:40:23"),
        format(messageAccessor, datetime, Map.of("date", new TypedValueString("time")), Map.of()));
  }


  @Test
  void testCustomPattern()
  {
    val messageAccessor = MessageSupportFactory
        .create(createFormatterService(new TemporalFormatter()), NO_CACHE_INSTANCE)
        .setLocale(FRANCE)
        .getMessageAccessor();
    val datetime = LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000);

    assertEquals(new TextPart("17 août"),
        format(messageAccessor, datetime, Map.of("date", new TypedValueString("dd MMMM")), Map.of()));
    assertEquals(new TextPart("jeu. jeudi"),
        format(messageAccessor, datetime, Map.of("date", new TypedValueString("EEE EEEE")), Map.of()));
    assertEquals(new TextPart("02:40:23,833"),
        format(messageAccessor, datetime, Map.of("date", new TypedValueString("HH:mm:ss,SSS")), Map.of()));
  }


  @Test
  void testFormatter()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new TemporalFormatter()), NO_CACHE_INSTANCE);

    assertEquals("17 aug 1972 16:45 14:23:01 2019-02-19 feb", messageSupport
        .message("%{a} %{b,date:'short'} %{c,date:'time'} %{c,date:'yyyy-MM-dd MMM'}")
        .with("a", LocalDate.of(1972, 8, 17))
        .with("b", LocalTime.of(16, 45, 9, 123))
        .with("c", LocalDateTime.of(2019, 2, 19, 14, 23, 1, 9))
        .locale("nl")
        .format());
  }
}
