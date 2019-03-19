package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Context;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import de.sayayi.lib.message.parameter.ParameterString;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import static de.sayayi.lib.message.MessageFactory.parse;
import static java.util.Locale.FRANCE;
import static java.util.Locale.GERMANY;
import static java.util.Locale.UK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


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
    final Context context = MessageContext.builder().withLocale(GERMANY).buildContext();
    LocalDate date = new LocalDate(1972, 8, 17);

    assertEquals("17.08.72", formatter.format("a", date, "short", context, null));
    assertEquals("17.08.1972", formatter.format("a", date, "medium", context, null));
    assertEquals("17. August 1972", formatter.format("a", date, "long", context, null));
    assertEquals("Donnerstag, 17. August 1972", formatter.format("a", date, "full", context, null));
    assertEquals("17.08.1972", formatter.format("a", date, "date", context, null));

    assertNull(formatter.format("a", date, "time", context, null));
  }


  @Test
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


  @Test
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


  @Test
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


  @Test
  public void testFormatter() throws Exception
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new JodaDateTimeFormatter());

    final Context context = MessageContext.builder()
        .withFormatterService(formatterRegistry)
        .withParameter("a", new LocalDate(1972, 8, 17))
        .withParameter("b", new LocalTime(16, 45, 9, 123))
        .withParameter("c", new DateTime(2019, 2, 19, 14, 23, 1, 9))
        .withLocale("nl")
        .buildContext();
    final Message msg = parse("%{a} %{b,short} %{c,time} %{c,'yyyy-MM-dd MMM'}");

    assertEquals("17-aug-1972 16:45 14:23:01 2019-02-19 feb", msg.format(context));
  }
}
