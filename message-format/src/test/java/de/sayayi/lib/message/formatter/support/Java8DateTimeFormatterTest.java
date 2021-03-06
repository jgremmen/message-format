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
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Locale.FRANCE;
import static java.util.Locale.GERMANY;
import static java.util.Locale.UK;
import static org.junit.Assert.assertEquals;


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
    Java8DateTimeFormatter formatter = new Java8DateTimeFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE,
        GERMANY);
    final Parameters noParameters = context.noParameters();
    LocalDate date = LocalDate.of(1972, 8, 17);

    assertEquals(new TextPart("17.08.72"), formatter.format(context, date, "short", noParameters, null));
    assertEquals(new TextPart("17.08.1972"), formatter.format(context, date, "medium", noParameters, null));
    assertEquals(new TextPart("17. August 1972"), formatter.format(context, date, "long", noParameters, null));
    assertEquals(new TextPart("Donnerstag, 17. August 1972"), formatter.format(context, date, "full", noParameters, null));
    assertEquals(new TextPart("17.08.1972"), formatter.format(context, date, "date", noParameters, null));

    assertEquals(Text.EMPTY, formatter.format(context, date, "time", noParameters, null));
  }


  @Test
  public void testLocalTime()
  {
    Java8DateTimeFormatter formatter = new Java8DateTimeFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE,
        GERMANY);
    final Parameters noParameters = context.noParameters();
    LocalTime time = LocalTime.of(16, 34, 11, 672000000);

    assertEquals(new TextPart("16:34"), formatter.format(context, time, "short", noParameters, null));
    assertEquals(new TextPart("16:34:11"), formatter.format(context, time, "medium", noParameters, null));
    assertEquals(new TextPart("16:34:11"), formatter.format(context, time, "long", noParameters, null));
    assertEquals(new TextPart("16:34 Uhr"), formatter.format(context, time, "full", noParameters, null));
    assertEquals(new TextPart("16:34:11"), formatter.format(context, time, "time", noParameters, null));

    assertEquals(Text.EMPTY, formatter.format(context, time, "date", noParameters, null));
  }


  @Test
  public void testDateTime()
  {
    Java8DateTimeFormatter formatter = new Java8DateTimeFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE,
        UK);
    final Parameters noParameters = context.noParameters();
    LocalDateTime datetime = LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000);

    assertEquals(new TextPart("17/08/72 02:40"), formatter.format(context, datetime, "short", noParameters, null));
    assertEquals(new TextPart("17-Aug-1972 02:40:23"), formatter.format(context, datetime, "medium", noParameters, null));
    assertEquals(new TextPart("17 August 1972 02:40:23 CET"), formatter.format(context, datetime, "long", noParameters, null));
    assertEquals(new TextPart("Thursday, 17 August 1972 02:40:23 o'clock CET"), formatter.format(context, datetime, "full", noParameters, null));

    assertEquals(new TextPart("17-Aug-1972"), formatter.format(context, datetime, "date", noParameters, null));
    assertEquals(new TextPart("02:40:23"), formatter.format(context, datetime, "time", noParameters, null));
  }


  @Test
  public void testCustomPattern()
  {
    Java8DateTimeFormatter formatter = new Java8DateTimeFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE,
        FRANCE);
    final Parameters noParameters = context.noParameters();
    LocalDateTime datetime = LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000);

    assertEquals(new TextPart("17 août"),
        formatter.format(context, datetime, null, noParameters, new DataString("dd MMMM")));
    assertEquals(new TextPart("jeu. jeudi"),
        formatter.format(context, datetime, null, noParameters, new DataString("EEE EEEE")));
    assertEquals(new TextPart("02:40:23,833"),
        formatter.format(context, datetime, null, noParameters, new DataString("HH:mm:ss,SSS")));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterService formatterRegistry = new GenericFormatterService();
    formatterRegistry.addFormatter(new Java8DateTimeFormatter());
    final MessageContext context = new MessageContext(formatterRegistry, NO_CACHE_INSTANCE);

    final Parameters parameters = context.parameters()
        .with("a", LocalDate.of(1972, 8, 17))
        .with("b", LocalTime.of(16, 45, 9, 123))
        .with("c", LocalDateTime.of(2019, 2, 19, 14, 23, 1, 9))
        .withLocale("nl");
    final Message msg = context.getMessageFactory().parse("%{a} %{b,short} %{c,time} %{c,'yyyy-MM-dd MMM'}");

    assertEquals("17-aug-1972 16:45 14:23:01 2019-02-19 feb", msg.format(context, parameters));
  }
}
