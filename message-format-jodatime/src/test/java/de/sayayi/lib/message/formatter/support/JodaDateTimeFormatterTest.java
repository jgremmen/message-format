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
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import static java.util.Locale.FRANCE;
import static java.util.Locale.GERMANY;
import static java.util.Locale.UK;
import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class JodaDateTimeFormatterTest
{
  @Test
  public void testLocalDate()
  {
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final MessageContext messageContext = new MessageContext(DefaultFormatterService.getSharedInstance(),
        MessageFactory.NO_CACHE_INSTANCE, GERMANY);
    final Parameters context = messageContext.noParameters();
    LocalDate date = new LocalDate(1972, 8, 17);

    assertEquals(new TextPart("17.08.72"), formatter.format(messageContext, date, "short", context, null));
    assertEquals(new TextPart("17.08.1972"), formatter.format(messageContext, date, "medium", context, null));
    assertEquals(new TextPart("17. August 1972"), formatter.format(messageContext, date, "long", context, null));
    assertEquals(new TextPart("Donnerstag, 17. August 1972"), formatter.format(messageContext, date, "full", context, null));
    assertEquals(new TextPart("17.08.1972"), formatter.format(messageContext, date, "date", context, null));

    assertEquals(Text.EMPTY, formatter.format(messageContext, date, "time", context, null));
  }


  @Test
  public void testLocalTime()
  {
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(),
        MessageFactory.NO_CACHE_INSTANCE, GERMANY);
    final Parameters noParameters = context.noParameters();
    LocalTime time = new LocalTime(16, 34, 11, 672);

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
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(),
        MessageFactory.NO_CACHE_INSTANCE, UK);
    final Parameters noParameters = context.noParameters();
    DateTime datetime = new DateTime(1972, 8, 17, 2, 40, 23, 833);

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
    final JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(),
        MessageFactory.NO_CACHE_INSTANCE, FRANCE);
    final Parameters noParameters = context.noParameters();
    DateTime datetime = new DateTime(1972, 8, 17, 2, 40, 23, 833);

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
    formatterRegistry.addFormatter(new JodaDateTimeFormatter());
    final MessageContext context = new MessageContext(formatterRegistry, MessageFactory.NO_CACHE_INSTANCE);

    final Parameters parameters = context.parameters()
        .with("a", new LocalDate(1972, 8, 17))
        .with("b", new LocalTime(16, 45, 9, 123))
        .with("c", new DateTime(2019, 2, 19, 14, 23, 1, 9))
        .withLocale("nl");
    final Message msg = context.getMessageFactory().parse("%{a} %{b,short} %{c,time} %{c,'yyyy-MM-dd MMM'}");

    assertEquals("17-aug-1972 16:45 14:23:01 2019-02-19 feb", msg.format(context, parameters));
  }


  @Test
  public void testMap()
  {
    final DefaultFormatterService formatterRegistry = new DefaultFormatterService();
    final MessageContext context = new MessageContext(formatterRegistry, MessageFactory.NO_CACHE_INSTANCE);

    assertEquals("2020", context.getMessageFactory().parse("%{d,{!null:'%{d,'yyyy'}'}}").format(
        context, context.parameters().with("d", new LocalDate(2020, 1, 1))));

    assertEquals("empty", context.getMessageFactory().parse("%{d,{empty:'empty'}}")
        .format(context, context.parameters().with("d", null)));

    assertEquals("empty", context.getMessageFactory().parse("%{d,time,{empty:'empty'}}")
        .format(context, context.parameters().with("d", LocalDate.now())));
  }
}
