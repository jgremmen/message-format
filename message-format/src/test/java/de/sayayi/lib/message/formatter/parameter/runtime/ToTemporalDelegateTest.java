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

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.util.Locale.GERMANY;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link ToTemporalDelegate}: formatting of {@link java.util.Date}, {@link java.sql.Date} and
 * {@link java.sql.Time} values.
 *
 * @author Jeroen Gremmen
 * @since 0.12.0  (renamed in 0.20.0)
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
  @DisplayName("Format java.sql.Date")
  @SuppressWarnings("deprecation")
  void formatSqlDate()
  {
    val date = new Date(2024 - 1900, Calendar.NOVEMBER, 9);
    val sqlDate = new java.sql.Date(date.getTime());

    assertEquals(noSpaceText("2024-11-09"), format(messageAccessor, sqlDate,
        Map.of("date", new TypedValueString(messageAccessor.getMessageFactory(), "yyyy-MM-dd")), Map.of()));
  }


  @Test
  @DisplayName("Format java.sql.Time")
  @SuppressWarnings("deprecation")
  void formatSqlTime()
  {
    val sqlTime = new java.sql.Time(23, 36, 4);

    assertEquals(noSpaceText("23:36"), format(messageAccessor, sqlTime,
        Map.of("date", new TypedValueString(messageAccessor.getMessageFactory(), "HH:mm")), Map.of()));
  }
}