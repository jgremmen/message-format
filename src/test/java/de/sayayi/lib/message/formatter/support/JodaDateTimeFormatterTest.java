package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.parameter.ParameterString;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.base.BaseLocal;

import java.util.Set;

import static java.util.Locale.*;
import static org.junit.Assert.*;


/**
 * @author Jeroen Gremmen
 */
public class JodaDateTimeFormatterTest
{
  @org.junit.Test
  public void testFormattableTypes()
  {
    Set<Class<?>> types = new JodaDateTimeFormatter().getFormattableTypes();
    assertTrue(types.contains(BaseLocal.class));
    assertTrue(types.contains(ReadableDateTime.class));
  }


  @org.junit.Test
  public void testLocalDate()
  {
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final Context context = MessageContext.builder().withLocale(GERMANY).buildContext();
    LocalDate date = new LocalDate(1972, 8, 17);

    assertEquals("17.08.72", formatter.format("a", date, "short", context, null));
    assertEquals("17.08.1972", formatter.format("a", date, "medium", context, null));
    assertEquals("17. August 1972", formatter.format("a", date, "long", context, null));
    assertEquals("Donnerstag, 17. August 1972", formatter.format("a", date, "full", context, null));
    assertEquals("17.08.1972", formatter.format("a", date, "date", context, null));

    assertNull(formatter.format("a", date, "time", context, null));
  }


  @org.junit.Test
  public void testLocalTime()
  {
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final Context context = MessageContext.builder().withLocale(GERMANY).buildContext();
    LocalTime time = new LocalTime(16, 34, 11, 672);

    assertEquals("16:34", formatter.format("a", time, "short", context, null));
    assertEquals("16:34:11", formatter.format("a", time, "medium", context, null));
    assertEquals("16:34:11", formatter.format("a", time, "long", context, null));
    assertEquals("16:34 Uhr", formatter.format("a", time, "full", context, null));
    assertEquals("16:34:11", formatter.format("a", time, "time", context, null));

    assertNull(formatter.format("a", time, "date", context, null));
  }


  @org.junit.Test
  public void testDateTime()
  {
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final Context context = MessageContext.builder().withLocale(UK).buildContext();
    DateTime datetime = new DateTime(1972, 8, 17, 2, 40, 23, 833);

    assertEquals("17/08/72 02:40", formatter.format("a", datetime, "short", context, null));
    assertEquals("17-Aug-1972 02:40:23", formatter.format("a", datetime, "medium", context, null));
    assertEquals("17 August 1972 02:40:23 CET", formatter.format("a", datetime, "long", context, null));
    assertEquals("Thursday, 17 August 1972 02:40:23 o'clock CET", formatter.format("a", datetime, "full", context, null));

    assertEquals("17-Aug-1972", formatter.format("a", datetime, "date", context, null));
    assertEquals("02:40:23", formatter.format("a", datetime, "time", context, null));
  }


  @org.junit.Test
  public void testCustomPattern()
  {
    JodaDateTimeFormatter formatter = new JodaDateTimeFormatter();
    final Context context = MessageContext.builder().withLocale(FRANCE).buildContext();
    DateTime datetime = new DateTime(1972, 8, 17, 2, 40, 23, 833);

    assertEquals("17 août",
        formatter.format("a", datetime, null, context, new ParameterString("dd MMMM")));
    assertEquals("jeu. jeudi",
        formatter.format("a", datetime, null, context, new ParameterString("EEE EEEE")));
    assertEquals("02:40:23,833",
        formatter.format("a", datetime, null, context, new ParameterString("HH:mm:ss,SSS")));
  }
}
