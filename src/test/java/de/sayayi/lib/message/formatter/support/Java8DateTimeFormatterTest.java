package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.ParameterFactory;
import de.sayayi.lib.message.data.ParameterString;
import de.sayayi.lib.message.formatter.GenericFormatterRegistry;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static de.sayayi.lib.message.MessageFactory.parse;
import static java.util.Locale.FRANCE;
import static java.util.Locale.GERMANY;
import static java.util.Locale.UK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


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
    final ParameterFactory context = ParameterFactory.createFor(GERMANY);
    LocalDate date = LocalDate.of(1972, 8, 17);

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
    Java8DateTimeFormatter formatter = new Java8DateTimeFormatter();
    final ParameterFactory context = ParameterFactory.createFor(GERMANY);
    LocalTime time = LocalTime.of(16, 34, 11, 672000000);

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
    Java8DateTimeFormatter formatter = new Java8DateTimeFormatter();
    final ParameterFactory context = ParameterFactory.createFor(UK);
    LocalDateTime datetime = LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000);

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
    Java8DateTimeFormatter formatter = new Java8DateTimeFormatter();
    final ParameterFactory context = ParameterFactory.createFor(FRANCE);
    LocalDateTime datetime = LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000);

    assertEquals("17 août",
        formatter.format("a", datetime, null, context, new ParameterString("dd MMMM")));
    assertEquals("jeu. jeudi",
        formatter.format("a", datetime, null, context, new ParameterString("EEE EEEE")));
    assertEquals("02:40:23,833",
        formatter.format("a", datetime, null, context, new ParameterString("HH:mm:ss,SSS")));
  }


  @Test
  public void testFormatter()
  {
    final GenericFormatterRegistry formatterRegistry = new GenericFormatterRegistry();
    formatterRegistry.addFormatter(new Java8DateTimeFormatter());
    final ParameterFactory context = ParameterFactory.createFor(formatterRegistry);

    final Parameters parameters = context.parameters()
        .with("a", LocalDate.of(1972, 8, 17))
        .with("b", LocalTime.of(16, 45, 9, 123))
        .with("c", LocalDateTime.of(2019, 2, 19, 14, 23, 1, 9))
        .withLocale("nl");
    final Message msg = parse("%{a} %{b,short} %{c,time} %{c,'yyyy-MM-dd MMM'}");

    assertEquals("17-aug-1972 16:45 14:23:01 2019-02-19 feb", msg.format(parameters));
  }
}
