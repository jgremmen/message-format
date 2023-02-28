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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.parameter.value.ConfigValueString;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.emptyText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static java.util.Collections.singletonMap;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class Java8DateTimeFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    assertFormatterForType(new Java8DateTimeFormatter(), LocalDate.class);
    assertFormatterForType(new Java8DateTimeFormatter(), LocalTime.class);
    assertFormatterForType(new Java8DateTimeFormatter(), LocalDateTime.class);
  }


  @Test
  public void testLocalDate()
  {
    val context = new MessageContext(createFormatterService(new Java8DateTimeFormatter()), NO_CACHE_INSTANCE, GERMANY);
    val date = LocalDate.of(1972, 8, 17);

    assertEquals(noSpaceText("17.08.72"),
        format(context, date, singletonMap(new ConfigKeyName("date"), new ConfigValueString("short"))));
    assertEquals(noSpaceText("17.08.1972"),
        format(context, date, singletonMap(new ConfigKeyName("date"), new ConfigValueString("medium"))));
    assertEquals(noSpaceText("17. August 1972"),
        format(context, date, singletonMap(new ConfigKeyName("date"), new ConfigValueString("long"))));
    assertEquals(noSpaceText("Donnerstag, 17. August 1972"),
        format(context, date, singletonMap(new ConfigKeyName("date"), new ConfigValueString("full"))));
    assertEquals(noSpaceText("17.08.1972"),
        format(context, date, singletonMap(new ConfigKeyName("date"), new ConfigValueString("date"))));

    assertEquals(emptyText(), format(context, date, singletonMap(new ConfigKeyName("date"), new ConfigValueString("time"))));
  }


  @Test
  public void testLocalTime()
  {
    val context = new MessageContext(createFormatterService(new Java8DateTimeFormatter()), NO_CACHE_INSTANCE, GERMANY);
    val time = LocalTime.of(16, 34, 11, 672000000);

    assertEquals(new TextPart("16:34"), format(context, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("short"))));
    assertEquals(new TextPart("16:34:11"), format(context, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("medium"))));
    assertEquals(new TextPart("16:34:11"), format(context, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("long"))));
    assertEquals(new TextPart("16:34 Uhr"), format(context, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("full"))));
    assertEquals(new TextPart("16:34:11"), format(context, time,
        singletonMap(new ConfigKeyName("date"), new ConfigValueString("time"))));

    assertEquals(Text.EMPTY, format(context, time, singletonMap(new ConfigKeyName("date"), new ConfigValueString("date"))));
  }


  @Test
  public void testDateTime()
  {
    val context = new MessageContext(createFormatterService(new Java8DateTimeFormatter()), NO_CACHE_INSTANCE, UK);
    val datetime = LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000);

    assertEquals(new TextPart("17/08/72 02:40"),
        format(context, datetime, singletonMap(new ConfigKeyName("date"), new ConfigValueString("short"))));
    assertEquals(new TextPart("17-Aug-1972 02:40:23"),
        format(context, datetime, singletonMap(new ConfigKeyName("date"), new ConfigValueString("medium"))));
    assertEquals(new TextPart("17 August 1972 02:40:23 CET"),
        format(context, datetime, singletonMap(new ConfigKeyName("date"), new ConfigValueString("long"))));
    assertEquals(new TextPart("Thursday, 17 August 1972 02:40:23 o'clock CET"),
        format(context, datetime, singletonMap(new ConfigKeyName("date"), new ConfigValueString("full"))));

    assertEquals(new TextPart("17-Aug-1972"),
        format(context, datetime, singletonMap(new ConfigKeyName("date"), new ConfigValueString("date"))));
    assertEquals(new TextPart("02:40:23"),
        format(context, datetime, singletonMap(new ConfigKeyName("date"), new ConfigValueString("time"))));
  }


  @Test
  public void testCustomPattern()
  {
    val context = new MessageContext(createFormatterService(new Java8DateTimeFormatter()), NO_CACHE_INSTANCE, FRANCE);
    val datetime = LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000);

    assertEquals(new TextPart("17 août"),
        format(context, datetime, singletonMap(new ConfigKeyName("date"), new ConfigValueString("dd MMMM"))));
    assertEquals(new TextPart("jeu. jeudi"),
        format(context, datetime, singletonMap(new ConfigKeyName("date"), new ConfigValueString("EEE EEEE"))));
    assertEquals(new TextPart("02:40:23,833"),
        format(context, datetime, singletonMap(new ConfigKeyName("date"), new ConfigValueString("HH:mm:ss,SSS"))));
  }


  @Test
  public void testFormatter()
  {
    val context = new MessageContext(createFormatterService(new Java8DateTimeFormatter()), NO_CACHE_INSTANCE);
    val parameters = context.parameters()
        .with("a", LocalDate.of(1972, 8, 17))
        .with("b", LocalTime.of(16, 45, 9, 123))
        .with("c", LocalDateTime.of(2019, 2, 19, 14, 23, 1, 9))
        .withLocale("nl");
    val msg = context.getMessageFactory()
        .parse("%{a} %{b,date:'short'} %{c,date:'time'} %{c,date:'yyyy-MM-dd MMM'}");

    assertEquals("17-aug-1972 16:45 14:23:01 2019-02-19 feb", msg.format(context, parameters));
  }
}
