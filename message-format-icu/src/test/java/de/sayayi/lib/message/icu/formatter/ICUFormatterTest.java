/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.icu.formatter;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;

import java.util.Date;

import static java.util.Calendar.JANUARY;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("'icu' formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ICUFormatterTest extends AbstractFormatterTest
{
  private static MessageSupport messageSupport;


  @BeforeAll
  static void init()
  {
    val formatterService = new GenericFormatterService();
    formatterService.addFormatter(new ICUFormatter());

    messageSupport = MessageSupportFactory
        .create(formatterService)
        .setLocale(ENGLISH);
  }


  @Test
  @DisplayName("Plural format with count parameter")
  void testPlural()
  {
    val msg = messageSupport
        .message("%{unused,format:icu,icu:'{count, plural, one {# item} other {# items}}'}");

    assertEquals("1 item", msg.with("count", 1).format());
    assertEquals("0 items", msg.with("count", 0).format());
    assertEquals("5 items", msg.with("count", 5).format());
    assertEquals("42 items", msg.with("count", 42).format());
  }


  @Test
  @DisplayName("Select format with gender parameter")
  void testSelect()
  {
    val msg = messageSupport
        .message("%{unused,format:icu,icu:'{gender, select, male {He} female {She} other {They}} liked your post.'}");

    assertEquals("He liked your post.", msg.with("gender", "male").format());
    assertEquals("She liked your post.", msg.with("gender", "female").format());
    assertEquals("They liked your post.", msg.with("gender", "other").format());
    assertEquals("They liked your post.", msg.with("gender", "unknown").format());
  }


  @Test
  @DisplayName("Select ordinal format")
  void testSelectOrdinal()
  {
    val msg = messageSupport
        .message("%{unused,format:icu,icu:'{rank, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}'}");

    assertEquals("1st", msg.with("rank", 1).format());
    assertEquals("2nd", msg.with("rank", 2).format());
    assertEquals("3rd", msg.with("rank", 3).format());
    assertEquals("4th", msg.with("rank", 4).format());
    assertEquals("11th", msg.with("rank", 11).format());
    assertEquals("21st", msg.with("rank", 21).format());
    assertEquals("22nd", msg.with("rank", 22).format());
    assertEquals("33rd", msg.with("rank", 33).format());
  }


  @Test
  @DisplayName("Select ordinal format with Welsh locale")
  void testSelectOrdinalWelsh()
  {
    // Welsh has rich ordinal categories: zero, one, two, few, many, other
    val msg = messageSupport
        .message("%{unused,format:icu,icu:'{rank, selectordinal, zero {#ain} one {#af} two {#ail} few {#ydd} many {#ed} other {#fed}}'}")
        .locale(forLanguageTag("cy"));

    assertEquals("0ain", msg.with("rank", 0).format());
    assertEquals("1af", msg.with("rank", 1).format());
    assertEquals("2ail", msg.with("rank", 2).format());
    assertEquals("3ydd", msg.with("rank", 3).format());
    assertEquals("4ydd", msg.with("rank", 4).format());
    assertEquals("5ed", msg.with("rank", 5).format());
    assertEquals("6ed", msg.with("rank", 6).format());
    assertEquals("7ain", msg.with("rank", 7).format());
    assertEquals("10fed", msg.with("rank", 10).format());
  }


  @Test
  @DisplayName("Number format with style")
  void testNumberFormat()
  {
    val msg = messageSupport
        .message("%{unused,format:icu,icu:'{amount, number, currency}'}")
        .locale(US);

    assertEquals("$1,234.56", msg.with("amount", 1234.56).format());
    assertEquals("$0.99", msg.with("amount", 0.99).format());

    val percentMsg = messageSupport
        .message("%{unused,format:icu,icu:'{ratio, number, percent}'}")
        .locale(US);

    assertEquals("75%", percentMsg.with("ratio", 0.75).format());
    assertEquals("100%", percentMsg.with("ratio", 1.0).format());
  }


  @Test
  @DisplayName("Combined plural and select in one pattern")
  void testCombinedPluralAndSelect()
  {
    val msg = messageSupport
        .message("%{unused,icu:'{gender, select, male {He has {count, plural, one {# new message} other {# new messages}}} female {She has {count, plural, one {# new message} other {# new messages}}} other {They have {count, plural, one {# new message} other {# new messages}}}}'}");

    assertEquals("He has 1 new message", msg.with("gender", "male").with("count", 1).format());
    assertEquals("She has 5 new messages", msg.with("gender", "female").with("count", 5).format());
    assertEquals("They have 0 new messages", msg.with("gender", "other").with("count", 0).format());
  }


  @Test
  @DisplayName("Multiple parameters in pattern")
  void testMultipleParameters()
  {
    val msg = messageSupport
        .message("%{unused,format:icu,icu:'{name} has {count, plural, one {# cat} other {# cats}}'}");

    assertEquals("Alice has 1 cat", msg.with("name", "Alice").with("count", 1).format());
    assertEquals("Bob has 3 cats", msg.with("name", "Bob").with("count", 3).format());
  }


  @Test
  @DisplayName("Locale-sensitive plural rules")
  @SuppressWarnings("SpellCheckingInspection")
  void testLocaleSensitivePlural()
  {
    val msg = messageSupport
        .message("%{unused,format:icu,icu:'{count, plural, one {# plik} few {# pliki} many {# plików} other {# pliku}}'}")
        .locale(forLanguageTag("pl"));

    assertEquals("1 plik", msg.with("count", 1).format());
    assertEquals("2 pliki", msg.with("count", 2).format());
    assertEquals("5 plików", msg.with("count", 5).format());
    assertEquals("22 pliki", msg.with("count", 22).format());
    assertEquals("12 plików", msg.with("count", 12).format());
  }


  @Test
  @DisplayName("Date format with style")
  void testDateFormat()
  {
    @SuppressWarnings("deprecation")
    val date = new Date(2026 - 1900, JANUARY, 15, 10, 30, 0);

    val msg = messageSupport
        .message("%{unused,format:icu,icu:'{when, date, short}'}");

    assertEquals("1/15/26", msg.with("when", date).format());

    val longMsg = messageSupport
        .message("%{unused,format:icu,icu:'{when, date, long}'}");

    assertEquals("January 15, 2026", longMsg.with("when", date).format());
  }


  @Test
  @DisplayName("Choice format with numeric ranges")
  void testChoiceFormat()
  {
    val msg = messageSupport
        .message("%{unused,format:icu,icu:'{temp, choice, 0#freezing|30#warm|40#hot}'}");

    assertEquals("freezing", msg.with("temp", 0).format());
    assertEquals("freezing", msg.with("temp", 15).format());
    assertEquals("warm", msg.with("temp", 30).format());
    assertEquals("warm", msg.with("temp", 35).format());
    assertEquals("hot", msg.with("temp", 40).format());
    assertEquals("hot", msg.with("temp", 50).format());
  }


  @Test
  @DisplayName("Plural with offset")
  void testPluralWithOffset()
  {
    val msg = messageSupport
        .message("%{unused,format:icu,icu:'{guests, plural, offset:1 =0 {Nobody is attending} =1 {Only {name} is attending} one {{name} and # other person are attending} other {{name} and # other people are attending}}'}");

    assertEquals("Nobody is attending", msg.with("guests", 0).with("name", "Alice").format());
    assertEquals("Only Alice is attending", msg.with("guests", 1).with("name", "Alice").format());
    assertEquals("Alice and 1 other person are attending", msg.with("guests", 2).with("name", "Alice").format());
    assertEquals("Alice and 4 other people are attending", msg.with("guests", 5).with("name", "Alice").format());
  }
}
