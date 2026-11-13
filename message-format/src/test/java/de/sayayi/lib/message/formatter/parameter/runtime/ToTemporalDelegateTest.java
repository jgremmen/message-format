/*
 * Copyright 2025 Jeroen Gremmen
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

import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import lombok.val;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.InstantSource;
import java.util.*;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.GERMANY;
import static java.util.TimeZone.getTimeZone;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link ToTemporalDelegate}: formatting of {@link java.util.Date}, {@link java.sql.Date} and
 * {@link java.sql.Time} values, and formatting of legacy date/time values without the optional {@code java.sql} module.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0
 */
@DisplayName("Temporal delegate")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ToTemporalDelegateTest extends AbstractFormatterTest
{
  private MessageAccessor messageAccessor;


  @BeforeEach
  void init()
  {
    messageAccessor = MessageSupportFactory
        .create(createFormatterService(new TemporalFormatter(), new ToTemporalDelegate()))
        .setLocale(GERMANY)
        .getMessageAccessor();
  }


  @Test
  @DisplayName("Format java.util.Date")
  @SuppressWarnings("deprecation")
  void formatDate()
  {
    val date = new Date(2024 - 1900, Calendar.NOVEMBER, 9);

    assertEquals(noSpaceText("09.11.2024, 00:00:00"), format(messageAccessor, date,
        Map.of("date", new TypedValueString(messageAccessor.getMessageFactory(), "medium")), Map.of()));
  }


  @Test
  @DisplayName("Format java.sql.Date as LocalDate (date only)")
  void formatSqlDateAsLocalDate()
  {
    val sqlDate = java.sql.Date.valueOf("2024-11-09");

    // a java.util.Date with the medium style includes the time: "09.11.2024, 00:00:00"
    assertEquals(noSpaceText("09.11.2024"), format(messageAccessor, sqlDate,
        Map.of("date", new TypedValueString(messageAccessor.getMessageFactory(), "medium")), Map.of()));
  }


  @Test
  @DisplayName("Format java.sql.Time as LocalTime (time only)")
  void formatSqlTimeAsLocalTime()
  {
    val sqlTime = java.sql.Time.valueOf("23:36:04");

    // a java.util.Date with the medium style includes the date: "01.01.1970, 23:36:04"
    assertEquals(noSpaceText("23:36:04"), format(messageAccessor, sqlTime,
        Map.of("date", new TypedValueString(messageAccessor.getMessageFactory(), "medium")), Map.of()));
  }


  @Test
  @DisplayName("Format java.util.Calendar in its own time zone")
  void formatCalendarWithTimeZone()
  {
    // use a zone which differs from the system default zone
    val zone = getTimeZone(TimeZone.getDefault().getID().equals("Asia/Tokyo") ? "America/New_York" : "Asia/Tokyo");
    val calendar = new GregorianCalendar(zone);
    calendar.clear();
    calendar.set(2024, Calendar.NOVEMBER, 9, 8, 30, 0);

    assertEquals(noSpaceText("2024-11-09 08:30 " + zone.getID()), format(messageAccessor, calendar,
        Map.of("date", new TypedValueString(messageAccessor.getMessageFactory(), "yyyy-MM-dd HH:mm VV")), Map.of()));
  }


  @Test
  @DisplayName("Format legacy date/time values without the java.sql module")
  void formatWithoutJavaSqlModule() throws IOException, InterruptedException
  {
    // the java.sql module is part of this JVM, so a separate JVM is started with java.base as the only system module
    val process = new ProcessBuilder(
        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "--limit-modules", "java.base",
        "-Duser.timezone=UTC",
        "-cp", System.getProperty("java.class.path"),
        WithoutJavaSqlMain.class.getName())
        .redirectErrorStream(true)
        .start();
    val output = new String(process.getInputStream().readAllBytes(), UTF_8);

    assertEquals(0, process.waitFor(), output);
    assertEquals("""
        java.sql module: false
        java.sql.Date: not loadable
        Date: 2024-11-09 23:36:04
        Calendar: 2024-11-09 23:36:04
        FileTime: 2024-11-09 23:36:04
        InstantSource: 2024-11-09 23:36:04
        """, output.replace(System.lineSeparator(), "\n"));
  }




  /**
   * Main class started by {@link #formatWithoutJavaSqlModule()} in a separate JVM without the {@code java.sql} module.
   * It formats the legacy date/time types handled by {@link ToTemporalDelegate} using the default formatters and prints
   * one line per value.
   */
  public static final class WithoutJavaSqlMain
  {
    public static void main(String[] args)
    {
      System.out.println("java.sql module: " + ModuleLayer.boot().findModule("java.sql").isPresent());

      try {
        Class.forName("java.sql.Date");
        System.out.println("java.sql.Date: loadable");
      } catch(ClassNotFoundException ex) {
        System.out.println("java.sql.Date: not loadable");
      }

      final var messageSupport = MessageSupportFactory.createDefault();
      final var message = messageSupport.message("%{d,date:'yyyy-MM-dd HH:mm:ss'}");
      final var instant = Instant.parse("2024-11-09T23:36:04Z");
      final var calendar = new GregorianCalendar(getTimeZone("UTC"));
      calendar.setTimeInMillis(instant.toEpochMilli());

      System.out.println("Date: " + message.with("d", Date.from(instant)).format());
      System.out.println("Calendar: " + message.with("d", (Calendar)calendar).format());
      System.out.println("FileTime: " + message.with("d", FileTime.from(instant)).format());
      System.out.println("InstantSource: " + message.with("d", InstantSource.fixed(instant)).format());
    }
  }
}
